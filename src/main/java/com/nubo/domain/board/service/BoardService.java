package com.nubo.domain.board.service;

import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardDetailResponseDto;
import com.nubo.domain.board.dto.BoardResponseDto;
import com.nubo.domain.board.dto.SectionDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.mapper.BoardMapper;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import com.nubo.domain.card.dto.CardResponseDto;
import com.nubo.domain.card.mapper.CardMapper;
import com.nubo.domain.card.repository.CardRepository;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardService {

  private final BoardRepository boardRepository;
  private final BoardMapper boardMapper;
  private final UserService userService;
  private final CardRepository cardRepository;
  private final CardMapper cardMapper;

  /**
   * 새 보드를 생성한다. 섹션일 경우 상위 보드 유효성도 함께 검사한다.
   *
   * @param dto    생성 요청 정보
   * @param userId 사용자 ID
   * @return 생성된 보드 DTO
   * @exception ApiException 필드 누락 또는 상위 보드 미존재 시 예외 발생
   */
  @Transactional
  public BoardResponseDto createBoard(BoardCreateRequestDto dto, Long userId) {
    Board parentBoard = null;

    // 섹션일 경우 상위 보드 필수
    if (dto.getBoardType() == BoardType.SECTION) {
      if (dto.getParentBoardId() == null) {
        throw new ApiException(ErrorCode.FIELD_REQUIRED);
      }

      parentBoard = boardRepository.findById(dto.getParentBoardId())
        .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

      if (parentBoard.getSource() == BoardSource.USER &&
        !parentBoard.getUser().getId().equals(userId)) {
        throw new ApiException(ErrorCode.ACCESS_DENIED);
      }
    }

    User user = userService.getUserById(userId);

    Board newBoard = boardMapper.toEntity(dto, user, parentBoard);
    Board savedBoard = boardRepository.save(newBoard);

    return boardMapper.toResponseDto(savedBoard);
  }

  /**
   * 주어진 사용자 ID로 보드 목록을 조회한다. (섹션 제외)
   *
   * @param userId 사용자 ID
   * @return 보드 응답 DTO 리스트
   */
  @Transactional(readOnly = true)
  public List<BoardResponseDto> getUserBoards(Long userId) {
    List<Board> boards = boardRepository.findVisibleBoardsForUser(userId, BoardType.BOARD);

    return boards.stream()
      .map(boardMapper::toResponseDto)
      .toList();
  }

  /**
   * 보드 ID로 보드를 조회한다.
   *
   * @param boardId 보드 ID
   * @return 조회된 보드 엔티티
   * @exception ApiException 보드가 존재하지 않는 경우 예외 발생
   */
  @Transactional(readOnly = true)
  public Board getBoardById(Long boardId) {
    return boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));
  }

  /**
   * 보드 ID를 기반으로 보드 상세 정보를 조회한다.
   * 하위 섹션과 포함된 카드 정보도 함께 반환한다.
   *
   * @param boardId 보드 ID
   * @return 보드 상세 응답 DTO
   * @exception ApiException 보드가 존재하지 않는 경우 예외 발생
   */
  @Transactional(readOnly = true)
  public BoardDetailResponseDto getBoardDetail(Long boardId) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    List<SectionDto> sectionDtos = boardRepository.findByParentBoard_Id(boardId).stream()
      .map(boardMapper::toSectionDto)
      .toList();

    List<CardResponseDto> cardDtos = cardRepository.findByBoardId(boardId).stream()
      .map(cardMapper::toResponseDto)
      .toList();

    return boardMapper.toDetailResponseDto(board, sectionDtos, cardDtos);
  }

}
