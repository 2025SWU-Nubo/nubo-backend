package com.nubo.domain.board.service;

import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardDetailResponseDto;
import com.nubo.domain.board.dto.BoardListResponseDto;
import com.nubo.domain.board.dto.BoardResponseDto;
import com.nubo.domain.board.dto.BoardStatsDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.mapper.BoardMapper;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import com.nubo.domain.card.dto.CardListResponseDto;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.card.mapper.CardMapper;
import com.nubo.domain.card.repository.CardRepository;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import com.nubo.domain.video.entity.Video;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  public List<BoardListResponseDto> getUserBoards(Long userId) {
    List<Board> boards = boardRepository.findVisibleBoardsForUser(userId, BoardType.BOARD);

    List<Long> boardIds = boards.stream()
      .map(Board::getId)
      .toList();

    // 통계 조회 (카운트 정보)
    List<BoardStatsDto> stats = boardRepository.getBoardStats(boardIds);
    Map<Long, BoardStatsDto> statsMap = stats.stream()
      .collect(Collectors.toMap(BoardStatsDto::getBoardId, Function.identity()));

    // 썸네일 조회
    Map<Long, String> thumbnailMap = new HashMap<>();

    for (Board board : boards) {
      Long boardId = board.getId();
      if (boardId == null) {
        continue;
      }

      long cardCount = cardRepository.countByBoardId(boardId);
      if (cardCount == 0) {
        thumbnailMap.put(boardId, null); // 썸네일 없음
        continue;
      }

      String thumbnailUrl = cardRepository.findTopByBoardOrderByCreatedAtDesc(board)
        .map(Card::getVideo)
        .map(Video::getThumbnailUrl)
        .orElse(null);

      thumbnailMap.put(boardId, thumbnailUrl);
    }

    // 매핑
    return boards.stream()
      .map(board -> {
        BoardStatsDto stat = statsMap.getOrDefault(board.getId(),
          new BoardStatsDto(board.getId(), 0L, 0L));
        String thumbnailUrl = thumbnailMap.get(board.getId());

        return boardMapper.toListResponseDto(
          board,
          stat.getSectionCount(),
          stat.getCardCount(),
          thumbnailUrl
        );
      })
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

    // 섹션 리스트
    List<Board> sectionBoards = boardRepository.findByParentBoard_Id(boardId);

    List<BoardListResponseDto> sections = new ArrayList<>();

    for (Board section : sectionBoards) {
      long cardCount = cardRepository.countByBoardId(section.getId());

      String thumbnailUrl = null;
      if (cardCount > 0) {
        thumbnailUrl = cardRepository.findTopByBoardOrderByCreatedAtDesc(section)
          .map(Card::getVideo)
          .map(video -> {
            if (video != null) {
              return video.getThumbnailUrl();
            }
            return null;
          })
          .orElse(null);
      }

      sections.add(boardMapper.toListResponseDto(section, 0L, cardCount, thumbnailUrl));
    }

    // 카드 리스트
    List<CardListResponseDto> cards = cardRepository.findByBoardId(boardId).stream()
      .map(cardMapper::toListResponseDto)
      .toList();

    return boardMapper.toDetailResponseDto(board, sections, cards);
  }

  /**
   * 보드의 최근 활동 시간을 갱신한다.
   *
   * 주로 카드가 추가될 때 사용되며,
   * Board.updatedAt 필드를 현재 시간으로 업데이트하여
   * "마지막으로 수정된 시간"을 기록하는 데 사용된다.
   *
   * @param boardId 활동을 갱신할 보드의 ID
   */
  @Transactional
  public void updateActivity(Long boardId) {
    Board board = getBoardById(boardId);

    // 현재 보드 갱신
    board.touch();
    boardRepository.save(board);

    // 만약 섹션(SECTION 타입)이고 상위 보드가 있다면, 상위 보드도 갱신
    if (board.getBoardType() == BoardType.SECTION && board.getParentBoard() != null) {
      Board parent = board.getParentBoard();
      parent.touch();
      boardRepository.save(parent);
    }
  }
}
