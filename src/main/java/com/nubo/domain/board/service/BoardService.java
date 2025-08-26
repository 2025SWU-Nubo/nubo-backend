package com.nubo.domain.board.service;

import com.nubo.domain.board.dto.BoardCardsDetachResultDto;
import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardDeleteRequestDto.DeleteLinkedCardsOption;
import com.nubo.domain.board.dto.BoardDeleteResultDto;
import com.nubo.domain.board.dto.BoardDetailResponseDto;
import com.nubo.domain.board.dto.BoardListResponseDto;
import com.nubo.domain.board.dto.BoardResponseDto;
import com.nubo.domain.board.dto.BoardStatsDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardMember;
import com.nubo.domain.board.mapper.BoardMapper;
import com.nubo.domain.board.mapper.BoardMemberMapper;
import com.nubo.domain.board.repository.BoardCardRepository;
import com.nubo.domain.board.repository.BoardMemberRepository;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import com.nubo.domain.card.dto.CardListResponseDto;
import com.nubo.domain.card.mapper.CardMapper;
import com.nubo.domain.card.repository.CardRepository;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
  private final BoardCardRepository boardCardRepository;
  private final BoardMemberRepository boardMemberRepository;
  private final BoardMemberMapper boardMemberMapper;

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
    // 1. 섹션일 경우 상위 보드 유효성 검사
    Board parentBoard = null;
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
      if (dto.isShared()) {
        throw new ApiException(ErrorCode.FIELD_INVALID); // 섹션은 공유 불가
      }
    } else {
      // 1-b. 보드인데 shared=false인데 memberEmails가 존재하면 오류
      if (!dto.isShared()
        && dto.getMemberEmails() != null
        && !dto.getMemberEmails().isEmpty()) {
        throw new ApiException(ErrorCode.FIELD_INVALID); // shared=false + memberEmails 존재
      }
    }

    // 2. 보드 소유자 로드
    User owner = userService.getUserById(userId);

    // 3. 보드 엔티티 생성/저장
    Board newBoard = boardMapper.toEntity(dto, owner, parentBoard);
    newBoard.setShared(dto.isShared());
    Board savedBoard = boardRepository.save(newBoard);

    // 4. 공유 보드일 경우 멤버십 생성
    if (dto.getBoardType() == BoardType.BOARD && dto.isShared()) {
      // 4-1. 이메일 정제
      Set<String> inviteEmails = Optional.ofNullable(dto.getMemberEmails())
        .orElse(List.of())
        .stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .map(String::toLowerCase)
        .filter(s -> !s.isBlank())
        .filter(s -> !s.equalsIgnoreCase(owner.getEmail()))
        .collect(Collectors.toCollection(LinkedHashSet::new));

      // 4-2. 유저 조회 + 누락 이메일 검증
      List<User> admins = List.of();
      if (!inviteEmails.isEmpty()) {
        admins = userService.getUsersByEmails(new ArrayList<>(inviteEmails));
        Set<String> found = admins.stream()
          .map(u -> u.getEmail().toLowerCase())
          .collect(Collectors.toSet());
        List<String> missing = inviteEmails.stream()
          .filter(e -> !found.contains(e))
          .toList();
        if (!missing.isEmpty()) {
          throw new ApiException(ErrorCode.ENTITY_NOT_FOUND);
        }
      }

      // 4-3. OWNER + ADMIN 멤버 생성/저장
      List<BoardMember> members = admins.isEmpty()
        ? List.of(boardMemberMapper.toOwner(savedBoard, owner))
        : boardMemberMapper.toOwnerAndAdmins(savedBoard, owner, admins);

      boardMemberRepository.saveAll(members);
    }

    // 5. 결과 반환
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

      long cardCount = cardRepository.countActiveByBoardId(boardId);
      if (cardCount == 0) {
        thumbnailMap.put(boardId, null); // 썸네일 없음
        continue;
      }

      var top1 = cardRepository.findRecentCardsByBoard(board, PageRequest.of(0, 1));
      String thumbnailUrl = top1.isEmpty()
        ? null
        : (top1.get(0).getVideo() != null ? top1.get(0).getVideo().getThumbnailUrl() : null);

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
      long cardCount = cardRepository.countActiveByBoardId(section.getId());
      String thumbnailUrl = null;
      if (cardCount > 0) {
        var top1 = cardRepository.findRecentCardsByBoard(section, PageRequest.of(0, 1));
        thumbnailUrl = top1.isEmpty()
          ? null
          : (top1.get(0).getVideo() != null ? top1.get(0).getVideo().getThumbnailUrl() : null);
      }

      sections.add(boardMapper.toListResponseDto(section, 0L, cardCount, thumbnailUrl));
    }

    // 카드 리스트
    List<CardListResponseDto> cards = cardRepository.findByBoardIdOrderByCreatedAtDesc(boardId)
      .stream()
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

    // 섹션이고 상위 보드가 있다면, 상위 보드도 갱신
    if (board.getBoardType() == BoardType.SECTION && board.getParentBoard() != null) {
      Board parent = board.getParentBoard();
      parent.touch();
      boardRepository.save(parent);
    }
  }

  /**
   * 보드 다중 삭제/숨김.
   * - AI 보드(기본 제공): per-user 숨김 처리 (보드/카드 실삭제 없음)
   * - 사용자 생성 보드(개인/공유): 보드/섹션은 하드 삭제(자식 → 부모),
   * 카드 링크 해제 후 옵션에 따라 고아 카드만 soft delete(DELETE_ORPHANS)
   *
   * @param boardIds 대상 보드들
   * @param option   DETACH_ONLY | DELETE_ORPHANS(고아 카드 소프트 삭제)
   * @param userId   요청 유저 (소유자 검증)
   * @return 보드별 처리 결과 리스트
   */
  @Transactional
  public List<BoardDeleteResultDto> deleteBoards(
    List<Long> boardIds,
    DeleteLinkedCardsOption option,
    Long userId
  ) {
    final DeleteLinkedCardsOption effective =
      option == null ? DeleteLinkedCardsOption.DETACH_ONLY : option;

    List<BoardDeleteResultDto> results = new ArrayList<>();
    for (Long boardId : boardIds) {
      try {
        BoardDeleteResultDto r = handleSingleBoardDelete(boardId, effective, userId);
        results.add(r);
      } catch (ApiException ae) {
        results.add(BoardDeleteResultDto.builder()
          .boardId(boardId)
          .status("FAILED")
          .option(effective.name())
          .error(ae.getErrorCode().name())
          .build());
      }
    }
    return results;
  }

  /**
   * 보드에서 카드 다중 제거(링크만 해제).
   *
   * @param boardId 대상 보드
   * @param cardIds 제거할 카드들
   * @param userId  요청 유저 (보드 소유자 검증)
   * @return 카드별 처리 결과 리스트
   */
  @Transactional
  public List<BoardCardsDetachResultDto> detachCardsFromBoard(
    Long boardId, List<Long> cardIds, Long userId
  ) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 공유보드 포함 멤버십 권한 허용(OWNER/ADMIN)
    boolean allowed = boardMemberRepository.existsByBoard_IdAndUser_Id(board.getId(), userId);
    if (!allowed) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    List<BoardCardsDetachResultDto> out = new ArrayList<>();
    for (Long cardId : cardIds) {
      try {
        boolean linked = boardCardRepository.existsByBoard_IdAndCard_Id(boardId, cardId);
        if (!linked) {
          out.add(BoardCardsDetachResultDto.builder()
            .cardId(cardId).status("NOT_LINKED").build());
          continue;
        }
        boardCardRepository.deleteByBoardIdAndCardId(boardId, cardId);
        out.add(BoardCardsDetachResultDto.builder()
          .cardId(cardId).status("OK").action("DETACHED").build());
      } catch (ApiException ae) {
        out.add(BoardCardsDetachResultDto.builder()
          .cardId(cardId).status("FAILED").error(ae.getErrorCode().name()).build());
      } catch (Exception e) {
        out.add(BoardCardsDetachResultDto.builder()
          .cardId(cardId).status("FAILED").error("INTERNAL_ERROR").build());
      }
    }
    return out;
  }

  /**
   * 단일 보드 삭제/숨김 처리
   * - 기본 보드(AI): per-user 숨김 마킹
   * - 사용자 보드(USER): 섹션 포함 연쇄 삭제
   * - option=DETACH_ONLY: 링크만 제거
   * - option=DELETE_ORPHANS: 링크 제거 + 고아 카드 soft delete
   */
  private BoardDeleteResultDto handleSingleBoardDelete(
    Long boardId,
    DeleteLinkedCardsOption option,
    Long userId
  ) {
    // 1. 보드 조회 및 권한 확인
    Board root = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    if (root.getSource() != BoardSource.AI) {
      boolean isOwner = root.getUser() != null && Objects.equals(root.getUser().getId(), userId);
      boolean isMember = boardMemberRepository.existsByBoard_IdAndUser_Id(root.getId(), userId);
      if (!isOwner && !isMember) {
        throw new ApiException(ErrorCode.ACCESS_DENIED);
      }
    }

    // 2. 삭제 대상 보드(자식 섹션 포함) 수집
    List<Board> targets = collectSelfAndSectionDescendantsPostOrder(root);
    List<Long> targetBoardIds = targets.stream().map(Board::getId).toList();

    // 3. 대상 보드들에 연결된 카드 수집
    List<Long> allCardIds = targetBoardIds.isEmpty()
      ? List.of()
      : boardCardRepository.findDistinctCardIdsByBoardIds(targetBoardIds);

    int linksDetached = 0;
    int cardsSoftDeleted = 0;

    // 4. 옵션에 따른 카드 처리 (링크 해제 → 고아 카드 soft delete)
    try {
      if (option == DeleteLinkedCardsOption.DETACH_ONLY) {
        System.out.println("STEP-2 detach links start");
        if (!targetBoardIds.isEmpty()) {
          linksDetached = boardCardRepository.deleteByBoardIds(targetBoardIds);
        }
        System.out.println("STEP-2 detach links done, linksDetached=" + linksDetached);
      } else {
        System.out.println("STEP-2a detach links for DELETE_ORPHANS start");
        if (!targetBoardIds.isEmpty()) {
          linksDetached = boardCardRepository.deleteByBoardIds(targetBoardIds);
        }
        System.out.println("STEP-2a done, linksDetached=" + linksDetached);

        // (선택) 고아 카드만 soft delete 하고 싶으면 여기서 orphanIds만 추려서 삭제
        System.out.println("STEP-2b soft delete cards start");
        if (!allCardIds.isEmpty()) {
          cardsSoftDeleted = cardRepository.softDeleteByIds(allCardIds, userId, Instant.now());
        }
        System.out.println("STEP-2b soft delete cards done, cardsSoftDeleted=" + cardsSoftDeleted);
      }
    } catch (Exception e) {
      e.printStackTrace(); // 정확한 예외 타입/메시지 확인
      throw e;
    }

    if (root.getSource() == BoardSource.AI) {
      // 5. AI 기본 보드인 경우: 숨김 처리만
      return BoardDeleteResultDto.builder()
        .boardId(boardId)
        .status("HIDDEN")
        .option(option.name())
        .linksDetached(linksDetached)
        .cardsSoftDeleted(cardsSoftDeleted)
        .sectionsDeleted(0)
        .build();
    } else {
      // 6. 사용자 보드: 멤버 삭제 → 보드/섹션 삭제
      try {
        System.out.println("STEP-3 delete members start");
        if (!targetBoardIds.isEmpty()) {
          boardMemberRepository.deleteByBoardIds(targetBoardIds);
        }
        System.out.println("STEP-3 delete members done");
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }

      try {
        System.out.println("STEP-4 delete boards reverse start");
        deleteBoardsInReverse(targets);
        System.out.println("STEP-4 delete boards reverse done");
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }

      deleteBoardsInReverse(targets);

      int sectionsDeleted = (int) targets.stream()
        .filter(t -> t.getBoardType() == BoardType.SECTION).count();

      return BoardDeleteResultDto.builder()
        .boardId(boardId)
        .status("DELETED")
        .option(option.name())
        .linksDetached(linksDetached)
        .cardsSoftDeleted(cardsSoftDeleted)
        .sectionsDeleted(sectionsDeleted)
        .build();
    }
  }

  /**
   * root 보드와 모든 하위 섹션을 후위 순회(Post-Order)로 반환 (항상 자식 먼저 오게)
   */
  private List<Board> collectSelfAndSectionDescendantsPostOrder(Board root) {
    List<Board> list = new ArrayList<>();
    collectDfs(root, list);
    return list;
  }

  private void collectDfs(Board node, List<Board> out) {
    List<Board> children = boardRepository.findByParentBoard_Id(node.getId());
    for (Board child : children) {
      collectDfs(child, out);
    }
    out.add(node); // 후위: 자식들 뒤에 부모
  }

  /**
   * 역순 삭제
   */
  private void deleteBoardsInReverse(List<Board> boardsPostOrder) {
    // boardsPostOrder는 이미 자식→부모 순으로 정렬되어 있으므로 그대로 순회하며 delete
    for (Board b : boardsPostOrder) {
      boardRepository.delete(b);
    }
  }
}
