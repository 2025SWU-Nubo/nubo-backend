package com.nubo.domain.recommendation.service;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.card.dto.AiCardMetaDto;
import com.nubo.domain.card.dto.WhisperResponseDto;
import com.nubo.domain.card.service.CardService;
import com.nubo.domain.card.service.TranscribeService;
import com.nubo.domain.card.service.YtDlpService;
import com.nubo.domain.recommendation.dto.YoutubeSearchBundle;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationGenerationService {

  private static final int KEYWORD_GROUP_LIMIT = 2;         // 키워드 기반 그룹 생성 수
  private static final long GROUP_EXPIRE_DAYS = 1;          // 그룹 만료 기간
  private static final int RECOMMENDATION_CARD_TARGET = 1;  // 추천카드 생성 목표 개수

  private final RecommendationGroupRepository groupRepository;
  private final YtDlpService ytDlpService;
  private final TranscribeService transcribeService;
  private final VideoService videoService;
  private final CardService cardService;
  private final OpenAiClient openAiClient;
  private final RecommendationCardRepository recommendationCardRepository;
  private final YoutubeSearchService youtubeSearchService;

  /*
   * 카테고리 기반 추천 그룹 생성
   */
  @Transactional
  public void createCategoryGroups() {

    List<DefaultBoard> categories = DefaultBoard.getAllCategories();
    List<RecommendationGroup> groups = new ArrayList<>();

    for (DefaultBoard category : categories) {
      RecommendationGroup group = RecommendationGroup.builder()
        .userId(null)
        .groupType(RecommendationGroupType.CATEGORY)
        .searchKeyword(null) // 검색 시 지정
        .keyword(null)
        .category(category)
        .expiresAt(LocalDateTime.now().plusDays(GROUP_EXPIRE_DAYS))
        .build();

      groupRepository.save(group);
      groups.add(group);
    }
  }

  /*
   * 키워드 기반 추천 그룹 생성
   */
  @Transactional
  public void createKeywordGroups(Long userId, List<String> keywords) {

    List<RecommendationGroup> groups = new ArrayList<>();

    int count = Math.min(KEYWORD_GROUP_LIMIT, keywords.size());

    for (int i = 0; i < count; i++) {

      RecommendationGroup group = RecommendationGroup.builder()
        .userId(userId)
        .groupType(RecommendationGroupType.KEYWORD)
        .searchKeyword(null)
        .keyword(keywords.get(i))
        .category(null)
        .expiresAt(LocalDateTime.now().plusDays(GROUP_EXPIRE_DAYS))
        .build();

      groupRepository.save(group);
      groups.add(group);
    }
  }

  /*
   * 그룹별 추천 카드 생성 (유튜브 검색 → Whisper → GPT)
   */
  @Transactional
  public void generateCardsForGroup(Long groupId) {
    // 비동기 스레드에서 안전하게 Group을 다시 조회
    Optional<RecommendationGroup> groupOpt = groupRepository.findById(groupId);
    if (groupOpt.isEmpty()) {
      log.warn("[추천그룹] ID를 찾을 수 없습니다. (이미 삭제되었거나 커밋되지 않음) - groupId={}", groupId);
      return;
    }
    RecommendationGroup group = groupOpt.get();

    log.info("[추천그룹] 카드 생성 시작 - groupId={}, type={}, keyword={}",
      group.getId(), group.getGroupType(), group.getKeyword());

    YoutubeSearchBundle bundle;

    // KEYWORD 그룹 → 키워드 검색
    if (group.getGroupType() == RecommendationGroupType.KEYWORD) {
      bundle = youtubeSearchService.searchByKeyword(group.getKeyword());
    }
    // CATEGORY 그룹 → 카테고리 기반 인기 검색 (keyword 사용)
    else {
      bundle = youtubeSearchService.searchByCategory(group.getCategory());
      group.setSearchKeyword(bundle.getSearchKeyword());  // 검색 키워드 저장
    }

    if (bundle.getResults().isEmpty()) {
      log.warn("[추천그룹] YouTube 검색 결과 없음 - groupId={}", group.getId());
      return;
    }

    // 2) 목표 개수 설정
    int targetCount = RECOMMENDATION_CARD_TARGET;
    int successCount = 0;

    // 검색된 결과 전체를 순회
    for (YoutubeVideoResult r : bundle.getResults()) {
      // 목표치를 달성했으면 중단 (불필요한 API 호출 방지)
      if (successCount >= targetCount) {
        group.setCardGenerated(true);
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

  @Async("recommendationExecutor")
  public void generateCardsForGroupAsync(Long groupId) {
    try {
      generateCardsForGroup(groupId);
    } catch (Exception e) {
      log.error("[Async] 추천 카드 생성 실패 - groupId=" + groupId, e);
    }
  }

  /*
   * 만료 그룹 정리
   */
  @Transactional
  public void cleanupExpiredGroups() {

    LocalDateTime now = LocalDateTime.now();

    List<RecommendationGroup> expired =
      groupRepository.findAllByExpiresAtBefore(now);

    if (expired.isEmpty()) {
      return;
    }

    expired.forEach(groupRepository::delete);

    log.info("[추천정리] 만료된 그룹 {}개 삭제", expired.size());
  }

  /*
   * 추천 카드 생성
   */
  @Transactional
  public RecommendationCard createRecommendedCard(
    Long userId,
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
      Thread.sleep(5000); // yt-dlp 호출 지연 5초
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

  // 오늘(AM 5:00 이후) 생성된 모든 그룹 가져오기
  public List<RecommendationGroup> getAllGroupsForToday() {
    LocalDateTime todayFiveAM = LocalDate.now().atTime(5, 0);
    return groupRepository.findAllByExpiresAtAfter(todayFiveAM);
  }

  public List<RecommendationGroup> getAllUnprocessedGroupsForToday() {
    LocalDateTime todayFiveAM = LocalDate.now().atTime(5, 0);
    // isCardGenerated가 false인 그룹만 조회
    return groupRepository.findAllByExpiresAtAfterAndIsCardGeneratedIsFalse(todayFiveAM);
  }
}
