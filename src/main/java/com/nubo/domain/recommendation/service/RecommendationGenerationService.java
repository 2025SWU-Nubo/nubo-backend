package com.nubo.domain.recommendation.service;

import com.nubo.domain.board.service.BoardService;
import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.card.dto.AiCardMetaDto;
import com.nubo.domain.card.dto.WhisperResponseDto;
import com.nubo.domain.card.service.CardService;
import com.nubo.domain.card.service.TranscribeService;
import com.nubo.domain.card.service.YtDlpService;
import com.nubo.domain.recommendation.dto.YoutubeVideoResult;
import com.nubo.domain.recommendation.entity.RecommendationCard;
import com.nubo.domain.recommendation.entity.RecommendationGroup;
import com.nubo.domain.recommendation.repository.RecommendationCardRepository;
import com.nubo.domain.recommendation.repository.RecommendationGroupRepository;
import com.nubo.domain.recommendation.type.RecommendationGroupType;
import com.nubo.domain.video.dto.VideoMetadataDto;
import com.nubo.domain.video.entity.Video;
import com.nubo.domain.video.service.VideoService;
import com.nubo.domain.video.type.Platform;
import com.nubo.global.ai.OpenAiClient;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationGenerationService {

  private final RecommendationGroupRepository groupRepository;
  private final RecommendationCardRepository cardRepository;
  private final RecommendationKeywordService keywordService;
  private final YtDlpService ytDlpService;
  private final TranscribeService transcribeService;
  private final VideoService videoService;
  private final CardService cardService;
  private final OpenAiClient openAiClient;
  private final RecommendationCardRepository recommendationCardRepository;
  private final BoardService boardService;
  private final YoutubeSearchService youtubeSearchService;

  // -----------------------------
  // 메인 진입점
  // -----------------------------
  @Transactional
  public void generateRecommendationsForUser(Long userId) {

    // 1) 기존 만료된 그룹/카드 정리
    cleanupExpiredGroups(userId);

    // 2) 사용자 카드 기반 키워드 추출
    List<String> topKeywords = keywordService.extractTopKeywords(userId, 5);

    // 3) 키워드 기반 추천 그룹 1~2개 생성
    List<RecommendationGroup> keywordGroups = createKeywordGroups(userId, topKeywords);

    // 4) 그룹마다 카드 생성 (유튜브 검색 → AI 요약)
    for (RecommendationGroup group : keywordGroups) {
      try {
        generateCardsForGroup(group);
      } catch (Exception e) {
        // 하나의 그룹이 실패해도 다른 그룹 생성을 위해 로그만 찍고 계속 진행
        log.error("[추천생성] 그룹 카드 생성 중 실패 (건너뜀) - groupId={}, keyword={}",
          group.getId(), group.getKeyword(), e);
      }
    }

    log.info("[추천생성] 사용자별 추천 생성 완료 - userId={}", userId);
  }

  // ----------------------------------------------------
  // 공통 인기 추천 그룹 생성 (userId = null)
  // 하루 1번 Scheduler에서 호출 예정
  // ----------------------------------------------------
  @Transactional
  public RecommendationGroup generatePopularRecommendationGroup()
    throws IOException, InterruptedException {

    log.info("[추천그룹] 인기 추천 그룹 생성 시작");

    // 1) 이전 인기 추천 그룹 삭제
    cleanupExpiredGroups(null);

    // 2) 새 그룹 생성
    RecommendationGroup group = RecommendationGroup.builder()
      .userId(null)  // 공통 추천
      .groupType(RecommendationGroupType.CATEGORY)
      .keyword(null)
      .expiresAt(LocalDateTime.now().plusDays(1))
      .build();

    groupRepository.save(group);

    // 3) 인기 영상 기반 추천카드 생성
    generateCardsForGroup(group);

    log.info("[추천그룹] 인기 추천 그룹 생성 완료 - groupId={}", group.getId());

    return group;
  }


  // ==============================================
  // 키워드 기반 추천 그룹 생성
  // ==============================================
  @Transactional
  public List<RecommendationGroup> createKeywordGroups(Long userId, List<String> keywords) {

    List<RecommendationGroup> groups = new ArrayList<>();

    if (keywords.isEmpty()) {
      return groups;
    }

    // 상위 2개만 사용
    int count = Math.min(2, keywords.size());

    for (int i = 0; i < count; i++) {

      RecommendationGroup group = RecommendationGroup.builder()
        .userId(userId)
        .groupType(RecommendationGroupType.KEYWORD)
        .keyword(keywords.get(i))
        .expiresAt(LocalDateTime.now().plusDays(1))
        .build();

      groupRepository.save(group);
      groups.add(group);
    }

    return groups;
  }


  // ==============================================
  // 추천 카드 생성 (유튜브 검색 → Whisper → GPT)
  // ==============================================
  @Transactional
  public void generateCardsForGroup(RecommendationGroup group)
    throws IOException, InterruptedException {

    log.info("[추천그룹] 카드 생성 시작 - groupId={}, type={}, keyword={}",
      group.getId(), group.getGroupType(), group.getKeyword());

    // 1) YouTube 검색
    List<YoutubeVideoResult> results;

    if (group.getGroupType() == RecommendationGroupType.KEYWORD) {
      results = youtubeSearchService.searchByKeyword(group.getKeyword());
    } else {
      results = youtubeSearchService.searchPopularByCategory(DefaultBoard.HOBBY);
    }

    if (results.isEmpty()) {
      log.warn("[추천그룹] YouTube 검색 결과 없음 - groupId={}", group.getId());
      return;
    }

    // 2) 목표 개수 설정 (예: 6개)
    int targetCount = 6;
    int successCount = 0;

    // 검색된 결과 전체를 순회
    for (YoutubeVideoResult r : results) {
      // 목표치를 달성했으면 중단 (불필요한 API 호출 방지)
      if (successCount >= targetCount) {
        break;
      }

      try {
        RecommendationCard createdCard = createRecommendedCard(
          group.getUserId(),
          r.getVideoUrl(),
          r.getVideoId(),
          group
        );

        // null이 아니면 성공으로 카운트
        if (createdCard != null) {
          successCount++;
        }

      } catch (Exception e) {
        // 개별 실패는 로그만 남기고 계속 진행 (성공 카운트는 안 올라감)
        log.error("추천 카드 생성 실패 (건너뜀) - videoId={}", r.getVideoId(), e);
      }
    }
  }

  // ==============================================
  // 만료 그룹 정리
  // ==============================================
  @Transactional
  public void cleanupExpiredGroups(Long userId) {

    LocalDateTime now = LocalDateTime.now();

    List<RecommendationGroup> expiredGroups;

    if (userId == null) {
      // 공통 추천 그룹 정리
      expiredGroups = groupRepository.findAllByUserIdIsNullAndExpiresAtBefore(now);
    } else {
      // 개인 추천 그룹 정리
      expiredGroups = groupRepository.findAllByUserIdAndExpiresAtBefore(userId, now);
    }

    if (expiredGroups.isEmpty()) {
      return;
    }

    for (RecommendationGroup g : expiredGroups) {
      groupRepository.delete(g); // cards도 함께 삭제됨
    }

    log.info("[추천정리] 만료된 그룹 {}개 삭제 (userId={})", expiredGroups.size(), userId);
  }


  @Transactional
  public RecommendationCard createRecommendedCard(
    Long userId,        // null 가능 (공통 추천)
    String videoUrl,
    String videoId,
    RecommendationGroup group
  ) throws IOException, InterruptedException {

    long startTime = System.currentTimeMillis();
    log.info("[추천카드] 생성 시작 - user={}, video={}", userId, videoId);

    Platform platform = Platform.fromUrl(videoUrl);

    Video video = null;
    byte[] audioBytes = null;
    VideoMetadataDto metadata = null;

    // 1. videoId 확보
    if (platform == Platform.YOUTUBE) {
      videoId = ytDlpService.extractVideoIdOnly(videoUrl);
      if (videoId == null || videoId.isBlank()) {
        throw new ApiException(ErrorCode.INVALID_VIDEO_ID);
      }
    } else {
      var ex = ytDlpService.extractAllForPlatform(videoUrl, platform);
      audioBytes = ex.getAudioBytes();
      metadata = ex.getMetadata();
      videoId = (metadata != null) ? metadata.getVideoId() : null;
      if (videoId == null || videoId.isBlank()) {
        throw new ApiException(ErrorCode.INVALID_VIDEO_ID);
      }
    }

    // 2. Video 조회/업서트
    video = videoService.getVideoById(videoId).orElse(null);
    if (video == null) {
      // metadata가 없다면 한 번 더 전체 추출
      if (metadata == null) {
        var ex = ytDlpService.extractAllForPlatform(videoUrl, platform);
        audioBytes = ex.getAudioBytes();
        metadata = ex.getMetadata();
      }

      String titleSeed =
        (metadata != null && metadata.getTitle() != null && !metadata.getTitle().isBlank())
          ? metadata.getTitle() : "";

      video = videoService.getOrCreateVideo(
        VideoMetadataDto.builder()
          .videoId(metadata.getVideoId())
          .videoUrl(metadata.getVideoUrl())
          .title(titleSeed)
          .description(metadata.getDescription())
          .thumbnailUrl(metadata.getThumbnailUrl())
          .platform(platform)
          .build()
      );
    } else {
      // 기존 video에 transcript가 비어있는데 audioBytes도 없으면 새로 다운로드
      if ((video.getTranscript() == null || video.getTranscript().isBlank())
        && audioBytes == null) {
        var ex = ytDlpService.extractAllForPlatform(videoUrl, platform);
        audioBytes = ex.getAudioBytes();
        if (metadata == null) {
          metadata = ex.getMetadata();
        }
      }
    }

    // 3. Whisper
    if (video.getTranscript() == null || video.getTranscript().isBlank()) {
      WhisperResponseDto whisper = transcribeService.transcribe(audioBytes);
      video.setTranscript(whisper.getTranscript());
      log.info("Whisper 완료 - 누적 {}ms", System.currentTimeMillis() - startTime);
    }

    // 4. GPT 요약/태그
    String inputText = cardService.buildFullText(video);
    AiCardMetaDto meta = openAiClient.generateCardMeta(inputText, userId, true, true);

    log.info("[추천카드] GPT 완료 - elapsed={}ms", System.currentTimeMillis() - startTime);

    if (meta == null) {
      log.info("[추천카드] 생성 스킵 (요약 불가/품질 미달) - videoId={}", videoId);
      return null;
    }

    // 5. AI 보드 판단 → DefaultBoard 매핑만 저장 (실제 보드 연결 X)
    DefaultBoard aiCategory = DefaultBoard.ETC;

    String boardName = meta.getAiCategory();
    if (boardName != null && !boardName.isBlank()) {
      aiCategory = Arrays.stream(DefaultBoard.values())
        .filter(b -> b.getDisplayName().equals(boardName))
        .findFirst()
        .orElse(DefaultBoard.ETC);
    }

    // 6. 추천 카드 생성(DB 저장)
    RecommendationCard card = RecommendationCard.builder()
      .group(group)
      .videoId(videoId)
      .title(meta.getTitle())
      .summary(meta.getSummary())
      .tags(String.join(",", meta.getTags()))
      .thumbnailUrl(video.getThumbnailUrl())
      .aiCategory(aiCategory) // 추천카드에 저장해두고 "정식 저장" 시 사용
      .isSaved(false)   // 정식카드로 변환 전까지는 false
      .build();

    RecommendationCard saved = recommendationCardRepository.save(card);

    log.info("[추천카드] 생성 완료 - group={}, cardId={}, elapsed={}ms",
      group.getId(), saved.getId(), System.currentTimeMillis() - startTime);

    return saved;
  }
}
