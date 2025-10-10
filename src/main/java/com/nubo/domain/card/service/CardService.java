package com.nubo.domain.card.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.service.BoardCardService;
import com.nubo.domain.board.service.BoardService;
import com.nubo.domain.card.dto.AiCardMetaDto;
import com.nubo.domain.card.dto.CardCreateRequestDto;
import com.nubo.domain.card.dto.CardCreateResponseDto;
import com.nubo.domain.card.dto.CardDeleteRequestDto;
import com.nubo.domain.card.dto.CardDeleteResultDto;
import com.nubo.domain.card.dto.CardDetailResponseDto;
import com.nubo.domain.card.dto.CardFavoriteRequestDto;
import com.nubo.domain.card.dto.CardFavoriteResponseDto;
import com.nubo.domain.card.dto.CardRestoreResponseDto;
import com.nubo.domain.card.dto.CardSimpleResponseDto;
import com.nubo.domain.card.dto.CardSummaryUpdateRequestDto.HighlightRange;
import com.nubo.domain.card.dto.CardSummaryUpdateResponseDto;
import com.nubo.domain.card.dto.WhisperResponseDto;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.card.entity.CardUserStatus;
import com.nubo.domain.card.mapper.CardMapper;
import com.nubo.domain.card.repository.CardRepository;
import com.nubo.domain.notification.service.FcmService;
import com.nubo.domain.stat.dto.DropResultDto;
import com.nubo.domain.stat.service.GrowthService;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import com.nubo.domain.video.dto.VideoMetadataDto;
import com.nubo.domain.video.entity.Video;
import com.nubo.domain.video.service.VideoService;
import com.nubo.domain.video.type.Platform;
import com.nubo.global.ai.OpenAiClient;
import com.nubo.global.common.FilterType;
import com.nubo.global.common.PageRequestUtil;
import com.nubo.global.common.SortType;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
  private final CardUserStatusService cardUserStatusService;

  private final BoardService boardService;
  private final BoardCardService boardCardService;

  private final OpenAiClient openAiClient;
  private final YtDlpService ytDlpService;
  private final TranscribeService transcribeService;
  private final GrowthService growthService;
  private final FcmService fcmService;

  // 문자열 유틸
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
   * 카드 생성
   * 1. 플랫폼 식별 및 videoId 확보
   * 2. 중복/삭제본 판정
   * 3. 신규 생성 시 Video 업서트 + Whisper/GPT 처리
   * 4. 카드 저장 및 보드 연결
   *
   * @param dto    카드 생성 요청 DTO (영상 및 카드 정보 포함)
   * @param userId 인증된 사용자 ID
   * @return 생성된 카드에 대한 응답 DTO
   * @exception ApiException board, section, user가 존재하지 않는 경우
   */
  @Transactional
  public CardCreateResponseDto createCard(CardCreateRequestDto dto, Long userId)
    throws IOException, InterruptedException {

    long startTime = System.currentTimeMillis();
    log.info("카드 생성 시작 - user={}, url={}", userId, dto.getVideoUrl());

    // 0. 기본 준비
    User user = userService.getUserById(userId);
    Platform platform = Platform.fromUrl(dto.getVideoUrl());

    String videoId = null;           // 복구/중복 판정용
    Video video = null;              // Video 엔티티
    byte[] audioBytes = null;        // Whisper용
    VideoMetadataDto metadata = null;// 신규 생성 시 메타

    // 1. videoId 확보
    if (platform == Platform.YOUTUBE) {
      videoId = ytDlpService.extractVideoIdOnly(dto.getVideoUrl());
      if (videoId == null || videoId.isBlank()) {
        throw new ApiException(ErrorCode.INVALID_VIDEO_ID);
      }
    } else {
      var ex = ytDlpService.extractAllForPlatform(dto.getVideoUrl(), platform);
      audioBytes = ex.getAudioBytes();
      metadata = ex.getMetadata();
      videoId = (metadata != null) ? metadata.getVideoId() : null;
      if (videoId == null || videoId.isBlank()) {
        throw new ApiException(ErrorCode.INVALID_VIDEO_ID);
      }
    }

    // 2. 중복/삭제본 판정
    var matches = cardRepository.findAnyByUserAndVideoIdForUpdate(user, videoId);
    var activeOpt = matches.stream().filter(c -> c.getDeletedAt() == null).findFirst();
    if (activeOpt.isPresent()) {
      throw new ApiException(ErrorCode.DUPLICATE_CARD);
    }

    var deletedOpt = matches.stream().filter(c -> c.getDeletedAt() != null).findFirst();
    if (deletedOpt.isPresent()) {
      Card revived = deletedOpt.get();
      revived.setDeletedAt(null);
      revived.setDeletedBy(null);
      cardRepository.save(revived);

      List<Long> boardIds = boardCardService.findBoardIdsByCardId(revived.getId());
      log.info("카드 복구 완료 - 원래 연결된 보드들: {}", boardIds);

      var restoreBoardIds = boardCardService.findBoardIdsByCardId(revived.getId());
      return cardMapper.toResponseDto(revived, restoreBoardIds);
    }

    // 3. 신규 Video 업서트
    video = videoService.getVideoById(videoId).orElse(null);
    if (video == null) {
      if (metadata == null) {
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
      if ((video.getTranscript() == null || video.getTranscript().isBlank())
        && audioBytes == null) {
        var ex = ytDlpService.extractAllForPlatform(dto.getVideoUrl(), platform);
        audioBytes = ex.getAudioBytes();
        if (metadata == null) {
          metadata = ex.getMetadata();
        }
      }
    }

    // 4. Whisper
    if (video.getTranscript() == null || video.getTranscript().isBlank()) {
      WhisperResponseDto whisper = transcribeService.transcribe(audioBytes);
      video.setTranscript(whisper.getTranscript());
      log.info("Whisper 완료 - 누적 {}ms", System.currentTimeMillis() - startTime);
    }

    // 5. GPT 요약/태그
    String inputText = buildFullText(video);
    AiCardMetaDto meta = openAiClient.generateCardMeta(inputText, userId);
    log.info("AI 메타 생성 완료 - 누적 {}ms", System.currentTimeMillis() - startTime);

    // 6. 최종 제목
    String mdTitle = (metadata != null) ? metadata.getTitle() : null;
    String mdDesc = (metadata != null) ? metadata.getDescription() : null;
    String finalTitle = (platform == Platform.YOUTUBE)
      ? firstNonEmpty(mdTitle, truncate(safe(mdDesc), 120), "(제목 없음)")
      : firstNonEmpty(meta.getTitle(), mdTitle, truncate(safe(mdDesc), 120), "(제목 없음)");
    video.setTitle(finalTitle);

    // 7. 카드 생성/저장
    Card card = cardMapper.toEntity(user, video);
    card.updateMeta(meta.getSummary(), meta.getTags());
    Card savedCard = cardRepository.save(card);

    // 8. 보드 연결
    List<Long> targetBoardIds =
      (dto.getBoardIds() != null && !dto.getBoardIds().isEmpty())
        ? dto.getBoardIds()
        : List.of(meta.getBoardId());
    if (targetBoardIds.stream().anyMatch(Objects::isNull)) {
      throw new ApiException(ErrorCode.ENTITY_NOT_FOUND);
    }

    for (Long boardId : targetBoardIds) {
      Board board = boardService.getBoardById(boardId);
      boardService.updateActivity(boardId);
      boardCardService.attachCard(board, savedCard);
    }

    List<Long> boardIds = boardCardService.findBoardIdsByCardId(savedCard.getId());
    log.info("카드 생성 완료 - 총 {}ms", System.currentTimeMillis() - startTime);

    // 생성 완료 알림 발송
    fcmService.sendCardCreatedNotification(userId, savedCard.getTitle(), savedCard);

    return cardMapper.toResponseDto(savedCard, boardIds);
  }

  /**
   * 로그인한 사용자의 전체 카드 목록을 조회한다.
   *
   * @param userId 사용자 ID
   * @return 카드 응답 DTO 리스트
   */
  @Transactional(readOnly = true)
  public Page<CardSimpleResponseDto> getCardsByUser(
    Long userId, int page, int size, SortType sort, FilterType filter) {

    User user = userService.getUserById(userId);
    PageRequest pageable = PageRequestUtil.of(page, size, sort, Card.class);

    // 1. 카드 조회 (필터 적용)
    Page<Card> cards = switch (filter) {
      case FAVORITE -> cardRepository.findFavoriteCards(user.getId(), pageable);
      case SHARED -> cardRepository.findSharedBoardCards(user.getId(), pageable);
      default -> cardRepository.findByUserAndDeletedAtIsNull(user, pageable);
    };

    // 2. 상태 조회
    List<Long> cardIds = cards.stream().map(Card::getId).toList();
    Map<Long, CardUserStatus> statusMap = cardUserStatusService.getStatusMap(userId, cardIds);

    // 3. DTO 변환
    return cards.map(card -> {
      CardUserStatus status = statusMap.get(card.getId());
      boolean isFavorite = status != null && Boolean.TRUE.equals(status.getIsFavorite());
      boolean viewed = status != null && status.getViewedAt() != null;
      return cardMapper.toSimpleResponseDto(card, isFavorite, viewed);
    });
  }

  /**
   * 특정 ID의 카드를 사용자 기준으로 조회한다.
   *
   * @param cardId 카드 ID
   * @param userId 사용자 ID
   * @return 카드 응답 DTO
   * @exception ApiException 사용자의 카드가 존재하지 않으면 예외 발생
   */
  @Transactional
  public CardDetailResponseDto getCardById(Long cardId, Long userId) {
    User user = userService.getUserById(userId);

    Card card = cardRepository.findAccessibleById(cardId, userId)
      .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED));

    // 열람 기록 (최초 여부 확인)
    boolean firstView = cardUserStatusService.markAsViewed(userId, card);

    DropResultDto result;
    if (firstView) {
      // 최초 열람일 때만 성장 반영
      result = growthService.addDrop(userId);
    } else {
      // 이미 본 카드면 성장 반영 없음 → 현재 단계만 계산해서 내려줌
      int stage = Math.min(user.getCurrentDrops() / 5, 4);
      result = new DropResultDto(stage, false, false);
    }

    // 즐겨찾기 여부 조회
    boolean isFavorite = cardUserStatusService.getFavoriteStatus(userId, card.getId());

    return cardMapper.toDetailResponseDto(
      card,
      result.getStage(),
      result.isBerryGained(),
      result.isStageUp(),
      isFavorite
    );
  }

  /**
   * 지정된 보드에서 사용자가 아직 열람하지 않은 카드 썸네일 리스트를 반환한다.
   * 결과는 랜덤 순서로 제한된 개수만 반환한다.
   *
   * @param userId  사용자 ID
   * @param boardId 보드 ID
   * @param limit   최대 반환 개수
   * @return 카드 썸네일 DTO 리스트
   */
  @Transactional(readOnly = true)
  public List<CardSimpleResponseDto> getUnviewedCardThumbnails(Long userId, Long boardId,
    int limit) {
    Pageable pageable = PageRequest.of(0, limit);

    List<Card> unviewedCards =
      cardRepository.findUnviewedCardsByBoard(userId, boardId, pageable);

    List<Long> cardIds = unviewedCards.stream().map(Card::getId).toList();
    Map<Long, CardUserStatus> statusMap = cardUserStatusService.getStatusMap(userId, cardIds);

    return unviewedCards.stream()
      .map(card -> {
        CardUserStatus status = statusMap.get(card.getId());
        boolean isFavorite = status != null && Boolean.TRUE.equals(status.getIsFavorite());
        boolean viewed = status != null && status.getViewedAt() != null;
        return cardMapper.toSimpleResponseDto(card, isFavorite, viewed);
      })
      .toList();
  }

  /**
   * 홈탭용 미시청 카드 목록 조회 (중복 제거, '전체'칩용)
   *
   * @param userId 사용자 ID
   * @param limit  조회할 최대 카드 개수
   * @return 미시청 카드 목록 (랜덤 순서, 중복 제거됨)
   */
  @Transactional(readOnly = true)
  public List<CardSimpleResponseDto> getDistinctUnviewedCards(Long userId, int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    List<Card> cards = cardRepository.findDistinctUnviewedCardsForHome(userId, pageable);

    List<Long> cardIds = cards.stream().map(Card::getId).toList();
    Map<Long, CardUserStatus> statusMap = cardUserStatusService.getStatusMap(userId, cardIds);

    return cards.stream()
      .map(card -> {
        CardUserStatus status = statusMap.get(card.getId());
        boolean isFavorite = status != null && Boolean.TRUE.equals(status.getIsFavorite());
        boolean viewed = status != null && status.getViewedAt() != null;
        return cardMapper.toSimpleResponseDto(card, isFavorite, viewed);
      })
      .toList();
  }

  /**
   * 사용자가 접근 가능한 카드 중 keyword로 검색
   *
   * @param userId  현재 로그인한 사용자 ID
   * @param keyword 검색 키워드
   * @return 카드 목록 (간단 정보)
   */
  @Transactional(readOnly = true)
  public List<CardSimpleResponseDto> searchCards(Long userId, String keyword, SortType sort) {
    List<Card> cards = cardRepository.searchAccessibleCards(userId, keyword, sort.name());

    List<Long> cardIds = cards.stream().map(Card::getId).toList();
    Map<Long, CardUserStatus> statusMap = cardUserStatusService.getStatusMap(userId, cardIds);

    return cards.stream()
      .map(card -> {
        CardUserStatus status = statusMap.get(card.getId());
        boolean isFavorite = status != null && Boolean.TRUE.equals(status.getIsFavorite());
        boolean viewed = status != null && status.getViewedAt() != null;
        return cardMapper.toSimpleResponseDto(card, isFavorite, viewed);
      })
      .toList();
  }

  /**
   * 카드 메타 생성용 전체 텍스트를 구성한다.
   * description, transcript, subtitle 등을 합쳐 요약 입력 데이터로 사용된다.
   *
   * @param video Video 엔티티
   * @return 합쳐진 텍스트 문자열
   */
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
   * 카드 summary를 AI로 재가공한다.
   *
   * @param cardId 카드 ID
   * @param userId 요청한 사용자 ID
   * @param prompt 사용자 프롬프트
   * @return 업데이트된 카드 summary 응답 DTO
   */
  @Transactional
  public CardSummaryUpdateResponseDto regenerateCardSummary(Long cardId, Long userId,
    String prompt) {
    Card card = cardRepository.findById(cardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    if (!card.getUser().getId().equals(userId)) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    Video video = card.getVideo();

    try {
      Map<String, Object> result = openAiClient.regenerateSummary(
        card.getTitle(),
        card.getSummary(),
        video.getDescription(),
        video.getTranscript(),
        video.getSubtitle(),
        prompt
      );

      String newSummary = (String) result.get("summary");
      @SuppressWarnings("unchecked")
      List<HighlightRange> highlights = (List<HighlightRange>) result.get("highlights");
      boolean valid = result.get("valid") != null && (boolean) result.get("valid");

      // 5. 연관성 검증 실패 → 예외 처리
      if (!valid) {
        throw new ApiException(ErrorCode.INVALID_PROMPT);
      }

      card.setSummary(newSummary);

      if (highlights != null) {
        try {
          ObjectMapper mapper = new ObjectMapper();
          String highlightJson = mapper.writeValueAsString(highlights);
          card.setHighlightInfo(highlightJson);
        } catch (Exception e) {
          throw new ApiException(ErrorCode.INVALID_FORMAT);
        }
      } else {
        card.setHighlightInfo("[]");
      }

      Card saved = cardRepository.save(card);
      return cardMapper.toSummaryUpdateResponseDto(saved);

    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(ErrorCode.AI_SUMMARY_FAILED);
    }
  }

  /**
   * 카드 summary를 사용자가 직접 수정한다.
   *
   * @param cardId     카드 ID
   * @param userId     요청한 사용자 ID
   * @param newSummary 새로 수정할 내용
   * @return 업데이트된 카드 summary 응답 DTO
   */
  @Transactional
  public CardSummaryUpdateResponseDto updateCardSummary(
    Long cardId,
    Long userId,
    String newSummary,
    List<HighlightRange> highlights
  ) {

    Card card = cardRepository.findById(cardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    if (!card.getUser().getId().equals(userId)) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    card.setSummary(newSummary);

    if (highlights != null) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        String highlightJson = mapper.writeValueAsString(highlights);
        card.setHighlightInfo(highlightJson);
      } catch (Exception e) {
        throw new ApiException(ErrorCode.INVALID_FORMAT);
      }
    }

    Card saved = cardRepository.save(card);

    return cardMapper.toSummaryUpdateResponseDto(saved);
  }

  /**
   * 특정 카드에 대해 사용자의 즐겨찾기 상태를 변경한다.
   *
   * @param userId  현재 로그인한 사용자 ID
   * @param cardId  즐겨찾기를 변경할 카드 ID
   * @param request 즐겨찾기 여부 요청 DTO (true: 추가, false: 해제)
   * @return 카드 ID와 최종 즐겨찾기 여부를 담은 응답 DTO
   * @exception ApiException 카드가 존재하지 않는 경우 발생
   */
  @Transactional
  public CardFavoriteResponseDto updateCardFavorite(Long userId, Long cardId,
    CardFavoriteRequestDto request) {

    Card card = cardRepository.findById(cardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    boolean favorite = cardUserStatusService.updateFavorite(userId, cardId, request.isFavorite());

    return cardMapper.toFavoriteResponseDto(card, favorite);
  }

  /**
   * 여러 카드를 전역 삭제(소프트 삭제)한다.
   *
   * @param req    삭제 요청 DTO
   * @param userId 현재 요청을 보낸 사용자 ID (권한 검사에 사용)
   * @return 카드별 처리 결과 리스트
   */
  @Transactional
  public List<CardDeleteResultDto> deleteCardsByMode(CardDeleteRequestDto req, Long userId) {
    List<CardDeleteResultDto> results = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    for (Long cardId : req.getCardIds()) {
      var opt = cardRepository.findByIdForUpdate(cardId);
      if (opt.isEmpty()) {
        results.add(CardDeleteResultDto.notFound(cardId));
        continue;
      }

      Card card = opt.get();

      if (!card.getUser().getId().equals(userId)) {
        results.add(CardDeleteResultDto.forbidden(cardId));
        continue;
      }

      if (card.getDeletedAt() != null) {
        results.add(CardDeleteResultDto.alreadyDeleted(cardId));
        continue;
      }

      switch (req.getDeleteMode()) {
        case DETACH_ONLY -> {
          boardCardService.detachCardFromAllBoards(cardId, userId);
          results.add(CardDeleteResultDto.detached(cardId));
        }
        case SOFT_DELETE -> {
          cardRepository.softDeleteById(cardId, userId, now);
          results.add(CardDeleteResultDto.deleted(cardId));
        }
      }
    }
    return results;
  }

  /**
   * 여러 카드를 복원한다. (soft delete 해제)
   *
   * @param cardIds 복원할 카드 ID 목록
   * @param userId  현재 요청을 보낸 사용자 ID
   * @return 복원된 카드 갯수 dto
   */
  @Transactional
  public CardRestoreResponseDto restoreCards(List<Long> cardIds, Long userId) {
    int restoredCount = 0;

    for (Long cardId : cardIds) {
      Card card = cardRepository.findByIdForUpdate(cardId)
        .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

      if (!card.getUser().getId().equals(userId)) {
        throw new ApiException(ErrorCode.ACCESS_DENIED);
      }

      if (card.getDeletedAt() == null) {
        continue; // 이미 복원된 카드
      }

      cardRepository.restoreById(cardId);
      restoredCount++;
    }

    return new CardRestoreResponseDto(restoredCount);
  }
}