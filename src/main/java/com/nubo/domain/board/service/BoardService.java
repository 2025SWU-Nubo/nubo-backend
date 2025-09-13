package com.nubo.domain.board.service;

import com.nubo.domain.board.dto.BoardCardsDetachResultDto;
import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardCreateResponseDto;
import com.nubo.domain.board.dto.BoardDeleteRequestDto.DeleteLinkedCardsOption;
import com.nubo.domain.board.dto.BoardDeleteResultDto;
import com.nubo.domain.board.dto.BoardDetailResponseDto;
import com.nubo.domain.board.dto.BoardFavoriteRequestDto;
import com.nubo.domain.board.dto.BoardFavoriteResponseDto;
import com.nubo.domain.board.dto.BoardMemberListResponseDto;
import com.nubo.domain.board.dto.BoardMemberUpdateRequestDto;
import com.nubo.domain.board.dto.BoardShareResponseDto;
import com.nubo.domain.board.dto.BoardSimpleResponseDto;
import com.nubo.domain.board.dto.BoardStatsDto;
import com.nubo.domain.board.dto.BoardSummaryResponseDto;
import com.nubo.domain.board.dto.BoardWithSectionsSimpleResponseDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.mapper.BoardMapper;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import com.nubo.domain.card.dto.CardSimpleResponseDto;
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

  private final BoardCardService boardCardService;

  private final BoardMemberService boardMemberService;

  /**
   * 주어진 사용자 소유 보드 중 이름 중복 여부를 확인한다.
   *
   * @param userId 사용자 ID
   * @param name   확인할 보드 이름
   * @return 사용 가능 여부 (true=사용 가능, false=중복)
   */
  @Transactional(readOnly = true)
  public boolean isBoardNameAvailable(Long userId, String name) {
    // 앞뒤 공백 제거
    String cleanName = name != null ? name.trim() : null;
    // 내 보드 중 동일한 이름이 존재하는지 확인
    boolean exists = boardRepository.existsByUser_IdAndNameIgnoreCase(userId, cleanName);
    return !exists;
  }

  /**
   * 새 보드를 생성한다. 섹션일 경우 상위 보드 유효성도 함께 검사한다.
   *
   * @param dto    생성 요청 정보
   * @param userId 사용자 ID
   * @return 생성된 보드 DTO
   * @exception ApiException 필드 누락 또는 상위 보드 미존재 시 예외 발생
   */
  @Transactional
  public BoardCreateResponseDto createBoard(BoardCreateRequestDto dto, Long userId) {
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
    String cleanName = dto.getName() != null ? dto.getName().trim() : null; // 앞뒤 공백 제거
    newBoard.setName(cleanName);
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
      boardMemberService.createOwnerAndAdmins(savedBoard, owner, admins);
    }

    // 5. 결과 반환
    return boardMapper.toCreateResponseDto(savedBoard);
  }

  /**
   * 주어진 사용자 ID로 보드 목록을 조회한다. (섹션 제외)
   *
   * @param userId 사용자 ID
   * @return 보드 응답 DTO 리스트
   */
  @Transactional(readOnly = true)
  public List<BoardSummaryResponseDto> getUserBoards(Long userId) {
    List<Board> boards = boardRepository.findVisibleBoardsForUser(userId, BoardType.BOARD);

    List<Long> boardIds = boards.stream()
      .map(Board::getId)
      .toList();

    // 각 보드별 BoardMember 조회
    Map<Long, Boolean> favoriteMap = boardMemberService
      .getFavoriteMapByUserAndBoardIds(userId, boardIds);

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
        boolean favorite = favoriteMap.getOrDefault(board.getId(), false);

        return boardMapper.toSummaryResponseDto(
          board,
          stat.getSectionCount(),
          stat.getCardCount(),
          thumbnailUrl,
          favorite
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
  public BoardDetailResponseDto getBoardDetail(Long boardId, Long userId) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 즐겨찾기 상태 조회
    boolean favorite = boardMemberService.getFavoriteStatus(boardId, userId);

    // 섹션 리스트
    List<Board> sectionBoards = boardRepository.findByParentBoard_Id(boardId);
    List<Long> sectionIds = sectionBoards.stream().map(Board::getId).toList();

    // 섹션별 즐겨찾기 상태 조회
    Map<Long, Boolean> sectionFavoriteMap = sectionIds.isEmpty()
      ? Map.of()
      : boardMemberService.getFavoriteMapByUserAndBoardIds(userId, sectionIds);

    List<BoardSummaryResponseDto> sections = new ArrayList<>();
    for (Board section : sectionBoards) {
      long cardCount = cardRepository.countActiveByBoardId(section.getId());
      String thumbnailUrl = null;
      if (cardCount > 0) {
        var top1 = cardRepository.findRecentCardsByBoard(section, PageRequest.of(0, 1));
        thumbnailUrl = top1.isEmpty()
          ? null
          : (top1.get(0).getVideo() != null ? top1.get(0).getVideo().getThumbnailUrl() : null);
      }
      boolean sectionFavorite = sectionFavoriteMap.getOrDefault(section.getId(), false);
      sections.add(
        boardMapper.toSummaryResponseDto(section, 0L, cardCount, thumbnailUrl, sectionFavorite));
    }

    // 카드 리스트
    List<CardSimpleResponseDto> cards = cardRepository.findByBoardIdOrderByCreatedAtDesc(boardId)
      .stream()
      .map(cardMapper::toListResponseDto)
      .toList();

    return boardMapper.toDetailResponseDto(board, sections, cards, favorite);
  }

  /**
   * 홈 화면용 보드 이름 리스트 조회
   *
   * @param userId 조회할 사용자 ID
   * @return 보드 이름 리스트 DTO
   */
  @Transactional(readOnly = true)
  public List<BoardSimpleResponseDto> getBoardsForHome(Long userId) {
    List<Board> boards = boardRepository.findVisibleBoardsForUser(userId, BoardType.BOARD);
    return boards.stream()
      .map(boardMapper::toSimpleResponseDto)
      .toList();
  }

  /**
   * 현재 사용자의 모든 보드와 섹션을 계층 구조로 조회한다.
   *
   * @param userId 조회할 사용자 ID
   * @return 보드+섹션 이름 리스트 DTO
   */
  @Transactional(readOnly = true)
  public List<BoardWithSectionsSimpleResponseDto> getBoardsWithSections(Long userId) {
    List<Board> boards = boardRepository.findAllAccessibleBoardsWithSections(userId);

    // 보드 + 모든 섹션 id 수집
    List<Long> allBoardIds = boards.stream()
      .flatMap(board -> {
        // 부모 보드 포함 + 자식 섹션들까지 flatten
        return board.getSections().stream()
          .map(Board::getId)
          .collect(Collectors.toList())
          .stream()
          .collect(Collectors.collectingAndThen(
            Collectors.toList(),
            list -> {
              list.add(board.getId());
              return list.stream();
            }
          ));
      })
      .toList();

    // 유저의 모든 멤버십 조회 → favorite 값 매핑
    Map<Long, Boolean> favoriteMap = boardMemberService
      .getFavoriteMapByUserAndBoardIds(userId, allBoardIds);

    // 매핑
    return boards.stream()
      .map(board -> boardMapper.toWithSectionsSimpleResponseDto(
        board,
        favoriteMap.getOrDefault(board.getId(), false), // 보드 favorite
        favoriteMap                                                // 섹션 favorite들
      ))
      .toList();
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
   * 보드 즐겨찾기 상태를 업데이트한다.
   *
   * @param userId  현재 사용자 ID
   * @param boardId 보드 ID
   * @param request 즐겨찾기 요청 DTO (favorite: true/false)
   * @return 변경된 즐겨찾기 응답 DTO
   */
  @Transactional
  public BoardFavoriteResponseDto updateBoardFavorite(Long userId, Long boardId,
    BoardFavoriteRequestDto request) {
    Board board = getBoardById(boardId);
    boolean favorite = boardMemberService.updateFavorite(userId, boardId, request.isFavorite());
    return boardMapper.toFavoriteResponseDto(board, favorite);
  }


  /**
   * 사용자 보드를 공유 보드로 전환한다.
   *
   * @param boardId 대상 보드 ID
   * @param userId  요청자 ID
   * @param shared  공유 여부 (현재 정책상 true만 허용)
   * @return 공유 상태가 반영된 응답 DTO
   * @exception ApiException ENTITY_NOT_FOUND 보드가 없을 때
   * @exception ApiException ACCESS_DENIED 권한이 없을 때
   * @exception ApiException INVALID_REQUEST 공유 취소(false) 요청 시
   */
  @Transactional
  public BoardShareResponseDto updateShareStatus(Long boardId, Long userId, boolean shared) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 권한 체크
    if (board.getSource() != BoardSource.USER) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }
    if (!board.getUser().getId().equals(userId)) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    // 이미 공유된 보드를 다시 개인보드로 되돌리려는 경우 금지
    if (board.isShared() && !shared) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }

    board.setShared(true);

    return boardMapper.toShareResponseDto(board);
  }

  /**
   * 공유 보드의 멤버 목록을 수정한다.
   *
   * @param boardId       대상 보드 ID
   * @param currentUserId 요청자 ID
   * @param dto           추가할 멤버 이메일 리스트 DTO
   * @return 추가된 멤버 정보 목록
   * @exception ApiException ENTITY_NOT_FOUND 보드 또는 사용자 없을 때
   * @exception ApiException ACCESS_DENIED 권한이 없을 때
   */
  @Transactional
  public BoardMemberListResponseDto updateMembers(Long boardId, Long currentUserId,
    BoardMemberUpdateRequestDto dto) {

    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 권한 체크
    if (board.getSource() != BoardSource.USER || !board.getUser().getId().equals(currentUserId)) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    return boardMemberService.updateMembers(board, dto);
  }


  /**
   * 사용자 보드의 이름을 수정한다.
   *
   * @param boardId 수정할 보드 ID
   * @param newName 새로운 보드 이름
   * @param userId  요청한 사용자 ID
   * @return 수정된 보드의 id와 name만 담은 DTO
   */
  @Transactional
  public BoardSimpleResponseDto updateBoardName(Long boardId, String newName, Long userId) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 기본 보드(source != USER)는 이름 변경 불가
    if (board.getSource() != BoardSource.USER) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    // 공유 보드인 경우 소유자만 수정 가능
    if (board.isShared() && !board.getUser().getId().equals(userId)) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    // 이름 공백 제거 및 검증
    String cleanName = newName != null ? newName.trim() : null;
    if (cleanName == null || cleanName.isEmpty()) {
      throw new ApiException(ErrorCode.FIELD_REQUIRED);
    }

    board.setName(cleanName);
    board.touch(); // updatedAt 갱신

    return boardMapper.toSimpleResponseDto(board);
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
    if (!boardMemberService.existsByBoardAndUser(board.getId(), userId)) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    List<BoardCardsDetachResultDto> out = new ArrayList<>();
    for (Long cardId : cardIds) {
      try {
        boolean linked = boardCardService.existsLink(boardId, cardId);
        if (!linked) {
          out.add(BoardCardsDetachResultDto.builder()
            .cardId(cardId).status("NOT_LINKED").build());
          continue;
        }
        boardCardService.detachCard(boardId, cardId);
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
      boolean isMember = boardMemberService.existsByBoardAndUser(root.getId(), userId);
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
      : boardCardService.findDistinctCardIdsByBoardIds(targetBoardIds);

    int linksDetached = 0;
    int cardsSoftDeleted = 0;

    // 4. 옵션에 따른 카드 처리 (링크 해제 → 고아 카드 soft delete)
    try {
      if (option == DeleteLinkedCardsOption.DETACH_ONLY) {
        System.out.println("STEP-2 detach links start");
        if (!targetBoardIds.isEmpty()) {
          linksDetached = boardCardService.detachByBoardIds(targetBoardIds);
        }
        System.out.println("STEP-2 detach links done, linksDetached=" + linksDetached);
      } else {
        System.out.println("STEP-2a detach links for DELETE_ORPHANS start");
        if (!targetBoardIds.isEmpty()) {
          linksDetached = boardCardService.detachByBoardIds(targetBoardIds);
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
          boardMemberService.deleteByBoardIds(targetBoardIds);
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
