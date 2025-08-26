package com.nubo.domain.card.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardCard;
import com.nubo.domain.board.repository.BoardCardRepository;
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
import java.time.Instant;
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
  private final BoardCardRepository boardCardRepository;

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
    log.info("카드 생성 시작 - user={}, url={}", userId, dto.getVideoUrl());

    // 0) 공통 준비
    User user = userService.getUserById(userId);
    Platform platform = Platform.fromUrl(dto.getVideoUrl());

    String videoId = null;           // 복구/중복 판정용
    Video video = null;              // Video 엔티티
    byte[] audioBytes = null;        // Whisper용
    VideoMetadataDto metadata = null;// 신규 생성 시 메타

    /* 1) videoId 확보 + 단일 판정(활성/삭제본) */
    if (platform == Platform.YOUTUBE) {
      videoId = ytDlpService.extractVideoIdOnly(dto.getVideoUrl());
      if (videoId == null || videoId.isBlank()) {
        throw new ApiException(ErrorCode.INVALID_VIDEO_ID);
      }
    } else {
      // 비-유튜브: 메타 추출로 videoId 먼저 확보(오디오도 같이 옴)
      log.info("메타 우선 추출(비-유튜브): {}", dto.getVideoUrl());
      var ex = ytDlpService.extractAllForPlatform(dto.getVideoUrl(), platform);
      audioBytes = ex.getAudioBytes();
      metadata = ex.getMetadata();
      videoId = (metadata != null) ? metadata.getVideoId() : null;
      if (videoId == null || videoId.isBlank()) {
        throw new ApiException(ErrorCode.INVALID_VIDEO_ID);
      }
    }

    // 유저+videoId 단일 조회(락) → 활성/삭제본 동시 판정
    var matches = cardRepository.findAnyByUserAndVideoIdForUpdate(user, videoId);

    // 활성 중복 → 409
    var activeOpt = matches.stream().filter(c -> c.getDeletedAt() == null).findFirst();
    if (activeOpt.isPresent()) {
      throw new ApiException(ErrorCode.DUPLICATE_CARD);
    }

    // 삭제본 있으면 즉시 복구(콘텐츠 불변)
    var deletedOpt = matches.stream().filter(c -> c.getDeletedAt() != null).findFirst();
    if (deletedOpt.isPresent()) {
      Card revived = deletedOpt.get();
      revived.setDeletedAt(null);
      revived.setDeletedBy(null);
      cardRepository.save(revived); // ⚠️ 제목/요약/태그 변경 금지

      // 복구 시 연결된 보드들 전부 응답에 포함해주거나,
      // 우선 하나만 대표로 넘기려면 첫 번째 보드 가져오기
      List<Long> boardIds = boardCardRepository.findBoardIdsByCardId(revived.getId());

      log.info("카드 복구 완료 - 원래 연결된 보드들: {}", boardIds);

      var restoreBoardIds = boardCardRepository.findBoardIdsByCardId(revived.getId());

      return cardMapper.toResponseDto(revived, restoreBoardIds);
    }

    /* 2) 신규 생성 경로 */
    // Video 로드/업서트
    video = videoRepository.findById(videoId).orElse(null);
    if (video == null) {
      if (metadata == null) {
        // 유튜브 신규: 여기서 all-in-one 추출
        log.info("추출(유튜브 신규): {}", dto.getVideoUrl());
        var ex = ytDlpService.extractAllForPlatform(dto.getVideoUrl(), platform);
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
      // transcript 없으면 오디오 확보
      if ((video.getTranscript() == null || video.getTranscript().isBlank())
        && audioBytes == null) {
        log.info("오디오만 재추출: {}", dto.getVideoUrl());
        var ex = ytDlpService.extractAllForPlatform(dto.getVideoUrl(), platform);
        audioBytes = ex.getAudioBytes();
        if (metadata == null) {
          metadata = ex.getMetadata();
        }
      }
    }

    // Whisper (필요 시)
    if (video.getTranscript() == null || video.getTranscript().isBlank()) {
      WhisperResponseDto whisper = transcribeService.transcribe(audioBytes);
      video.setTranscript(whisper.getTranscript());
      log.info("Whisper 완료 - 누적 {}ms", System.currentTimeMillis() - startTime);
    }

    // GPT 요약/태그
    String inputText = buildFullText(video);
    AiCardMetaDto meta = openAiClient.generateCardMeta(inputText, userId);
    log.info("AI 메타 생성 완료 - 누적 {}ms", System.currentTimeMillis() - startTime);

    // 최종 제목(신규만 세팅)
    String mdTitle = (metadata != null) ? metadata.getTitle() : null;
    String mdDesc = (metadata != null) ? metadata.getDescription() : null;
    String finalTitle = (platform == Platform.YOUTUBE)
      ? firstNonEmpty(mdTitle, truncate(safe(mdDesc), 120), "(제목 없음)")
      : firstNonEmpty(meta.getTitle(), mdTitle, truncate(safe(mdDesc), 120), "(제목 없음)");
    video.setTitle(finalTitle);

    // 카드 생성/저장
    Card card = cardMapper.toEntity(user, video);
    card.updateMeta(meta.getSummary(), meta.getTags());
    Card savedCard = cardRepository.save(card);

    // 보드 결정 (여러 개 지원: 요청 or AI 분류)
    List<Long> targetBoardIds =
      (dto.getBoardIds() != null && !dto.getBoardIds().isEmpty())
        ? dto.getBoardIds()
        : List.of(meta.getBoardId());

    if (targetBoardIds.contains(null)) {
      throw new ApiException(ErrorCode.ENTITY_NOT_FOUND);
    }

    // 보드-카드 링크 생성
    for (Long boardId : targetBoardIds) {
      Board board = boardService.getBoardById(boardId);
      boardService.updateActivity(boardId);

      if (!boardCardRepository.existsByBoard_IdAndCard_Id(board.getId(), savedCard.getId())) {
        BoardCard link = new BoardCard();
        link.setBoard(board);
        link.setCard(savedCard);
        boardCardRepository.save(link);
      }
    }

    // 응답에 연결된 보드 ID 전체 반환
    List<Long> boardIds = boardCardRepository.findBoardIdsByCardId(savedCard.getId());
    log.info("카드 생성 완료 - 총 {}ms", System.currentTimeMillis() - startTime);
    return cardMapper.toResponseDto(savedCard, boardIds);
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
      cards = cardRepository.findAllActiveByUserOrderByTitleAsc(user);
    } else {
      cards = cardRepository.findAllActiveByUserOrderByCreatedAtDesc(user);
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

    Card card = cardRepository.findActiveByIdAndUser(cardId, user)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    return cardMapper.toDetailResponseDto(card);
  }

  private String buildFullText(Video video) {
    StringBuilder sb = new StringBuilder();

    // 1) 요약 근거
    sb.append("📌 요약 근거 (이 내용을 중심으로 요약하세요)\n");

    if (video.getDescription() != null && !video.getDescription().isBlank()) {
      sb.append("- description:\n").append(video.getDescription().trim()).append("\n\n");
    }
    if (video.getTranscript() != null && !video.getTranscript().isBlank()) {
      sb.append("- transcript:\n").append(video.getTranscript().trim()).append("\n\n");
    }
    if (video.getSubtitle() != null && !video.getSubtitle().isBlank()) {
      sb.append("- subtitle:\n").append(video.getSubtitle().trim()).append("\n\n");
    }

    // 2) 제목(참고용)
    String title = (video.getTitle() == null || video.getTitle().isBlank())
      ? "(제목 없음)" : video.getTitle().trim();
    sb.append("📌 원본 제목(참고용, 내용 요약에는 사용 금지)\n").append(title).append("\n\n");

    String result = sb.toString();
    log.info("=== buildFullText result ===\n{}", result); // fulltext 확인용
    return result;
  }

  /**
   * 여러 카드를 전역 삭제(소프트 삭제)한다.
   *
   * @param cardIds 삭제할 카드 ID 목록
   * @param userId  현재 요청을 보낸 사용자 ID (권한 검사에 사용)
   * @return 카드별 처리 결과 리스트
   */
  @Transactional
  public List<java.util.Map<String, Object>> deleteCardsGlobally(List<Long> cardIds, Long userId) {
    java.util.List<java.util.Map<String, Object>> results = new java.util.ArrayList<>();
    Instant now = Instant.now();

    for (Long cardId : cardIds) {
      var opt = cardRepository.findByIdForUpdate(cardId);
      if (opt.isEmpty()) {
        results.add(java.util.Map.of("cardId", cardId, "status", "NOT_FOUND"));
        continue;
      }
      Card card = opt.get();

      // 생성자 권한 확인
      if (!card.getUser().getId().equals(userId)) {
        results.add(java.util.Map.of("cardId", cardId, "status", "FORBIDDEN"));
        continue;
      }

      if (card.getDeletedAt() != null) {
        results.add(java.util.Map.of("cardId", cardId, "status", "ALREADY_DELETED"));
        continue;
      }

      // 소프트 삭제
      cardRepository.softDeleteById(cardId, userId, now);

      results.add(java.util.Map.of(
        "cardId", cardId,
        "action", "SOFT_DELETED",
        "status", "OK"
      ));
    }
    return results;
  }
}