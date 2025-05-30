package com.nubo.domain.card.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.service.BoardService;
import com.nubo.domain.card.dto.AiCardMetaDto;
import com.nubo.domain.card.dto.CardCreateRequestDto;
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

  /**
   * 최적화된 카드 생성 요청 처리
   * yt-dlp를 한 번만 호출하여 오디오와 메타데이터를 동시에 추출
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

    // 1. 사용자 조회
    User user = userService.getUserById(userId);

    // 2. 영상 ID 빠른 추출
    String videoId = ytDlpService.extractVideoIdOnly(dto.getVideoUrl());

    // 3. 기존 영상 확인 및 중복 카드 체크
    Video existingVideo = videoRepository.findById(videoId).orElse(null);
    if (existingVideo != null && cardRepository.existsByUserAndVideo(user, existingVideo)) {
      throw new ApiException(ErrorCode.DUPLICATE_CARD);
    }

    Video video;
    byte[] audioBytes;
    VideoMetadataDto metadata;

    if (existingVideo != null && existingVideo.getTranscript() != null
      && !existingVideo.getTranscript().isBlank()) {
      // 기존 영상에 transcript가 있으면 재사용 (오디오 추출 생략)
      log.info("기존 영상 및 transcript 재사용: {}", videoId);
      video = existingVideo;
      audioBytes = null; // Whisper 처리 생략
      metadata = null;   // 메타데이터 추출 생략
    } else {
      // 4. 통합 추출: 오디오와 메타데이터를 한 번에 처리
      log.info("yt-dlp 통합 추출 시작");
      YtDlpService.ExtractResult result = ytDlpService.extractAudioAndMetadata(dto.getVideoUrl());
      audioBytes = result.getAudioBytes();
      metadata = result.getMetadata();

      log.info("yt-dlp 통합 추출 완료 - 소요시간: {}ms",
        System.currentTimeMillis() - startTime);

      // 5. 영상 저장 (신규인 경우)
      if (existingVideo == null) {
        video = videoService.getOrCreateVideo(metadata);
      } else {
        video = existingVideo;
      }

      // 6. Whisper 처리 (transcript가 없는 경우만)
      WhisperResponseDto whisperResult = transcribeService.transcribe(audioBytes);
      String transcript = whisperResult.getTranscript();
      video.setTranscript(transcript);

      log.info("음성 인식 완료 - 소요시간: {}ms",
        System.currentTimeMillis() - startTime);
    }

    // 7. GPT 메타데이터 생성 (video 정보 가공)
    String inputText = buildFullText(video);
    AiCardMetaDto meta = openAiClient.generateCardMeta(inputText);

    log.info("AI 메타데이터 생성 완료 - 소요시간: {}ms",
      System.currentTimeMillis() - startTime);

    // 8. 보드 매핑 (사용자 지정 or 기본 제공 보드)
    Long boardId = dto.getBoardId() != null
      ? dto.getBoardId()
      : meta.getBoardId();
    Board board = boardService.getBoardById(boardId);

    // 9. 카드 생성 및 저장
    Card card = cardMapper.toEntity(dto, user, video, board);
    card.updateMeta(meta.getSummary(), meta.getTags());
    Card savedCard = cardRepository.save(card);

    long totalTime = System.currentTimeMillis() - startTime;
    log.info("카드 생성 완료 - 총 소요시간: {}ms", totalTime);

    // 10. 응답 DTO로 변환
    return cardMapper.toResponseDto(savedCard);
  }


  /**
   * 로그인한 사용자의 전체 카드 목록을 조회한다.
   *
   * @param userId 사용자 ID
   * @return 카드 응답 DTO 리스트
   */
  @Transactional(readOnly = true)
  public List<CardResponseDto> getCardsByUser(Long userId, String sort) {
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
      .map(cardMapper::toResponseDto)
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
  public CardResponseDto getCardById(Long cardId, Long userId) {
    User user = userService.getUserById(userId);

    Card card = cardRepository.findByIdAndUser(cardId, user)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    return cardMapper.toResponseDto(card);
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