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
import com.nubo.domain.video.VideoMetadataDto;
import com.nubo.domain.video.entity.Video;
import com.nubo.domain.video.repository.VideoRepository;
import com.nubo.domain.video.service.VideoService;
import com.nubo.global.ai.OpenAiClient;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
   * 사용자의 카드 생성 요청을 처리한다.
   *
   * 요청에 담긴 영상 정보가 기존에 존재하지 않으면 새로 저장하고,
   * 해당 유저/보드/섹션 정보를 기반으로 카드 엔티티를 생성하여 저장한 뒤,
   * 응답용 DTO로 변환해 반환한다.
   *
   * @param dto    카드 생성 요청 DTO (영상 및 카드 정보 포함)
   * @param userId 인증된 사용자 ID
   * @return 생성된 카드에 대한 응답 DTO
   * @exception ApiException board, section, user가 존재하지 않는 경우
   */
  @Transactional
  public CardResponseDto createCard(CardCreateRequestDto dto, Long userId)
    throws IOException, InterruptedException {

    // 1. 사용자 조회
    User user = userService.getUserById(userId);

    // 2. 영상 ID 추출 → DB에 있는지 확인
    String videoId = ytDlpService.extractVideoIdOnly(dto.getVideoUrl());
    Video video;

    if (videoRepository.existsById(videoId)) {
      video = videoRepository.findById(videoId)
        .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));
    } else {
      // 2-1. 메타데이터 추출
      VideoMetadataDto metadata = ytDlpService.extractMetadata(dto.getVideoUrl());

      // 2-2. 영상 저장
      video = videoService.getOrCreateVideo(metadata); // 내부에서 save
    }

    // 3. 중복 카드 방지
    if (cardRepository.existsByUserAndVideo(user, video)) {
      throw new ApiException(ErrorCode.DUPLICATE_CARD);
    }

    // 4. Whisper 처리
    byte[] audioBytes = ytDlpService.extractAudioBytes(dto.getVideoUrl());
    WhisperResponseDto whisperResult = transcribeService.transcribe(audioBytes);
    String transcript = whisperResult.getTranscript();
    video.setTranscript(transcript); // GPT 입력에 활용

    // 5. GPT 메타데이터 생성 (video 정보 가공)
    String inputText = buildFullText(video);
    AiCardMetaDto meta = openAiClient.generateCardMeta(inputText);

    // 6. 보드 매핑 (사용자 지정 or 기본 제공 보드)
    Long boardId = dto.getBoardId() != null
      ? dto.getBoardId()
      : meta.getBoardId();
    Board board = boardService.getBoardById(boardId);

    // 7. 카드 생성 및 저장
    Card card = cardMapper.toEntity(dto, user, video, board);
    card.updateMeta(meta.getSummary(), meta.getTags());
    Card savedCard = cardRepository.save(card);

    // 8. 응답 DTO로 변환
    return cardMapper.toResponseDto(savedCard);
  }

  /**
   * 로그인한 사용자의 전체 카드 목록을 조회한다.
   *
   * @param userId 사용자 ID
   * @return 카드 응답 DTO 리스트
   */
  @Transactional(readOnly = true)
  public List<CardResponseDto> getCardsByUser(Long userId) {
    // 1. 유저 조회 (정확한 연관 보장을 위해)
    User user = userService.getUserById(userId);

    // 2. 카드 리스트 조회
    List<Card> cards = cardRepository.findAllByUser(user);

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
