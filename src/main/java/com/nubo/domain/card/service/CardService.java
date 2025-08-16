package com.nubo.domain.card.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.service.BoardService;
import com.nubo.domain.card.dto.AiCardMetaDto;
import com.nubo.domain.card.dto.CardCreateRequestDto;
import com.nubo.domain.card.dto.CardDetailResponseDto;
import com.nubo.domain.card.dto.CardListResponseDto;
import com.nubo.domain.card.dto.CardResponseDto;
import com.nubo.domain.card.dto.WhisperResponseDto;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.card.mapper.CardMapper;
import com.nubo.domain.card.repository.CardRepository;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import com.nubo.domain.video.dto.VideoMetadataDto;
import com.nubo.domain.video.entity.Video;
import com.nubo.domain.video.repository.VideoRepository;
import com.nubo.domain.video.service.VideoService;
import com.nubo.domain.video.type.Platform;
import com.nubo.global.ai.OpenAiClient;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

  private final CardRepository cardRepository;
  private final CardMapper cardMapper;

  private final VideoService videoService;
  private final UserService userService;
  private final BoardService boardService;
  private final OpenAiClient openAiClient;
  private final YtDlpService ytDlpService;
  private final TranscribeService transcribeService;
  private final VideoRepository videoRepository;

  // 길이 제한용 유틸
  private static String truncate(String s, int max) {
    return (s != null && s.length() > max) ? s.substring(0, max) + "..." : s;
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }

  private static String firstNonEmpty(String... vals) {
    for (String v : vals) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
  }

  /**
   * 최적화된 카드 생성 요청 처리
   * - 플랫폼 식별
   * - (필요 시) yt-dlp 한 번 호출로 메타데이터+오디오 추출
   * - Whisper → GPT 요약/태그 → 카드 생성
   *
   * @param dto    카드 생성 요청 DTO (영상 및 카드 정보 포함)
   * @param userId 인증된 사용자 ID
   * @return 생성된 카드에 대한 응답 DTO
   * @exception ApiException board, section, user가 존재하지 않는 경우
   */
  @Transactional
  public CardResponseDto createCard(CardCreateRequestDto dto, Long userId)
    throws IOException, InterruptedException {

    long startTime = System.currentTimeMillis();
    log.info("카드 생성 시작 - 사용자: {}, URL: {}", userId, dto.getVideoUrl());

    // 0) 필수 객체
    User user = userService.getUserById(userId);
    Platform platform = Platform.fromUrl(dto.getVideoUrl());

    // 1) 영상 ID/중복 체크 (플랫폼에 따라 다르게)
    String videoId = null;
    Video existingVideo = null;

    if (platform == Platform.YOUTUBE) {
      // 유튜브는 빠른 ID 추출 가능
      videoId = ytDlpService.extractVideoIdOnly(dto.getVideoUrl());
      existingVideo = (videoId != null) ? videoRepository.findById(videoId).orElse(null) : null;
      if (existingVideo != null && cardRepository.existsByUserAndVideo(user, existingVideo)) {
        throw new ApiException(ErrorCode.DUPLICATE_CARD);
      }
    } else {
      // 인스타/틱톡 등은 먼저 메타데이터 뽑아서 id 확보하는 쪽이 안전
      // (아래에서 실제 추출 후 중복 체크 수행)
    }

    Video video;
    byte[] audioBytes = null;
    VideoMetadataDto metadata = null;
    // 2) 기존 영상에 transcript 있으면 재사용 (오디오/추출 생략)
    if (existingVideo != null &&
      existingVideo.getTranscript() != null &&
      !existingVideo.getTranscript().isBlank()) {

      log.info("기존 영상 transcript 재사용: {}", existingVideo.getId());
      video = existingVideo;

    } else {
      // 3) 플랫폼 공용 추출 (메타데이터 → 오디오 wav)
      log.info("추출 시작 (플랫폼 공용): platform={}, url={}", platform, dto.getVideoUrl());
      YtDlpService.ExtractResult ex = ytDlpService.extractAllForPlatform(dto.getVideoUrl(),
        platform);
      audioBytes = ex.getAudioBytes();
      metadata = ex.getMetadata();

      // 인스타/틱톡 등은 여기서 id 확보되므로 중복 체크 수행
      if (platform != Platform.YOUTUBE) {
        videoId = metadata.getVideoId();
        existingVideo = (videoId != null) ? videoRepository.findById(videoId).orElse(null) : null;
        if (existingVideo != null && cardRepository.existsByUserAndVideo(user, existingVideo)) {
          log.info("중복 카드 감지: user={}, video={}", userId, videoId);
          throw new ApiException(ErrorCode.DUPLICATE_CARD);
        }
      }

      // 4) Video 저장/업서트
      if (existingVideo == null) {
        // 제목 비어있으면 설명(캡션)으로 대체
        String title = (metadata.getTitle() != null && !metadata.getTitle().isBlank())
          ? metadata.getTitle()
          : (metadata.getDescription() != null && !metadata.getDescription().isBlank()
            ? truncate(metadata.getDescription(), 120)
            : "(제목 없음)");

        video = videoService.getOrCreateVideo(
          VideoMetadataDto.builder()
            .videoId(metadata.getVideoId())
            .videoUrl(metadata.getVideoUrl())
            .title(title)
            .description(metadata.getDescription())
            .thumbnailUrl(metadata.getThumbnailUrl())
            .platform(platform)
            .build()
        );
      } else {
        video = existingVideo;
      }

      // 5) Whisper (기존 transcript 없을 때만)
      if (video.getTranscript() == null || video.getTranscript().isBlank()) {
        WhisperResponseDto whisper = transcribeService.transcribe(audioBytes);
        video.setTranscript(whisper.getTranscript());
        log.info("음성 인식 완료 - 누적 {}ms", System.currentTimeMillis() - startTime);
      }
    }

    // 6) GPT 요약/태그 생성 (video의 transcript 기반)
    String inputText = buildFullText(video);
    AiCardMetaDto meta = openAiClient.generateCardMeta(inputText, userId);
    log.info("AI 메타데이터 생성 완료 - 누적 {}ms", System.currentTimeMillis() - startTime);

    // 6-1) 최종 제목 결정
    String metaTitle = meta.getTitle();
    String mdTitle = (metadata != null) ? metadata.getTitle() : null;
    String mdDesc = (metadata != null) ? metadata.getDescription() : null;

    String finalTitle;
    if (platform == Platform.YOUTUBE) {
      // 유튜브는 원본 제목 우선
      finalTitle = firstNonEmpty(mdTitle, truncate(safe(mdDesc), 120), "(제목 없음)");
    } else {
      // 인스타/틱톡은 GPT 제목 우선
      finalTitle = firstNonEmpty(metaTitle, mdTitle, truncate(safe(mdDesc), 120), "(제목 없음)");
    }
    video.setTitle(finalTitle);

    // 7) 보드 매핑 (사용자 지정 우선, 없으면 AI 분류)
    Long boardId = (dto.getBoardId() != null) ? dto.getBoardId() : meta.getBoardId();
    Board board = boardService.getBoardById(boardId);
    boardService.updateActivity(boardId);

    // 8) 카드 생성/저장
    Card card = cardMapper.toEntity(user, video, board);
    card.updateMeta(meta.getSummary(), meta.getTags());
    Card savedCard = cardRepository.save(card);

    log.info("카드 생성 완료 - 총 {}ms", System.currentTimeMillis() - startTime);
    return cardMapper.toResponseDto(savedCard);
  }

  /**
   * 로그인한 사용자의 전체 카드 목록을 조회한다.
   *
   * @param userId 사용자 ID
   * @return 카드 응답 DTO 리스트
   */
  @Transactional(readOnly = true)
  public List<CardListResponseDto> getCardsByUser(Long userId, String sort) {
    // 1. 유저 조회 (정확한 연관 보장을 위해)
    User user = userService.getUserById(userId);

    // 2. 정렬 기준에 따라 카드 조회
    List<Card> cards;
    if ("alphabetical".equalsIgnoreCase(sort)) {
      cards = cardRepository.findAllByUserOrderByTitleAsc(user);
    } else {
      cards = cardRepository.findAllByUserOrderByCreatedAtDesc(user); // 기본: 최신순
    }

    // 3. DTO 변환
    return cards.stream()
      .map(cardMapper::toListResponseDto)
      .toList();
  }

  /**
   * 특정 ID의 카드를 사용자 기준으로 조회한다.
   *
   * @param cardId 카드 ID
   * @param userId 사용자 ID
   * @return 카드 응답 DTO
   * @exception ApiException 사용자의 카드가 존재하지 않으면 예외 발생
   */
  @Transactional(readOnly = true)
  public CardDetailResponseDto getCardById(Long cardId, Long userId) {
    User user = userService.getUserById(userId);

    Card card = cardRepository.findByIdAndUser(cardId, user)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    return cardMapper.toDetailResponseDto(card);
  }

  private String buildFullText(Video video) {
    StringBuilder sb = new StringBuilder();

    if (video.getTitle() != null && !video.getTitle().isBlank()) {
      sb.append("📌 제목:\n").append(video.getTitle()).append("\n\n");
    }
    if (video.getDescription() != null && !video.getDescription().isBlank()) {
      sb.append("📌 소개글:\n").append(video.getDescription()).append("\n\n");
    }
    if (video.getTranscript() != null && !video.getTranscript().isBlank()) {
      sb.append("📌 음성 텍스트:\n").append(video.getTranscript()).append("\n\n");
    }
    if (video.getSubtitle() != null && !video.getSubtitle().isBlank()) {
      sb.append("📌 자막:\n").append(video.getSubtitle()).append("\n\n");
    }

    return sb.toString();
  }

}