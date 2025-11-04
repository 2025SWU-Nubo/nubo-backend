package com.nubo.domain.board.service;

import com.nubo.domain.board.dto.BoardCardsDetachResultDto;
import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardCreateResponseDto;
import com.nubo.domain.board.dto.BoardDeleteRequestDto.DeleteLinkedCardsOption;
import com.nubo.domain.board.dto.BoardDeleteResultDto;
import com.nubo.domain.board.dto.BoardDetailResponseDto;
import com.nubo.domain.board.dto.BoardFavoriteRequestDto;
import com.nubo.domain.board.dto.BoardFavoriteResponseDto;
import com.nubo.domain.board.dto.BoardInvitationRequestDto;
import com.nubo.domain.board.dto.BoardInvitationResponseDto;
import com.nubo.domain.board.dto.BoardMemberListResponseDto;
import com.nubo.domain.board.dto.BoardNameCheckResponseDto;
import com.nubo.domain.board.dto.BoardPreviewResponseDto;
import com.nubo.domain.board.dto.BoardRestoreRequestDto;
import com.nubo.domain.board.dto.BoardRestoreResponseDto;
import com.nubo.domain.board.dto.BoardShareResponseDto;
import com.nubo.domain.board.dto.BoardSimpleResponseDto;
import com.nubo.domain.board.dto.BoardStatsDto;
import com.nubo.domain.board.dto.BoardSummaryResponseDto;
import com.nubo.domain.board.dto.BoardWithSectionsSimpleResponseDto;
import com.nubo.domain.board.dto.BulkActionRequestDto;
import com.nubo.domain.board.dto.BulkActionResponseDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardCard;
import com.nubo.domain.board.entity.BoardInvitation;
import com.nubo.domain.board.entity.BoardMember;
import com.nubo.domain.board.mapper.BoardMapper;
import com.nubo.domain.board.mapper.BoardMemberMapper;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.board.type.InvitationStatus;
import com.nubo.domain.card.dto.CardRestoreRequestDto;
import com.nubo.domain.card.dto.CardRestoreResponseDto;
import com.nubo.domain.card.dto.CardSimpleResponseDto;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.card.entity.CardUserStatus;
import com.nubo.domain.card.mapper.CardMapper;
import com.nubo.domain.card.repository.CardRepository;
import com.nubo.domain.card.service.CardService;
import com.nubo.domain.card.service.CardUserStatusService;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.common.FilterType;
import com.nubo.global.common.PageRequestUtil;
import com.nubo.global.common.SortType;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
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
  private final CardUserStatusService cardUserStatusService;

  private final BoardCardService boardCardService;

  private final BoardMemberService boardMemberService;
  private final BoardInvitationService boardInvitationService;
  private final BoardMemberMapper boardMemberMapper;

  private final ApplicationContext applicationContext;

  /**
   * 주어진 사용자 소유 보드 중 이름 중복 여부를 확인한다.
   *
   * @param userId 사용자 ID
   * @param name   확인할 보드 이름
   * @return 사용 가능 여부 (true=사용 가능, false=중복)
   */
  @Transactional(readOnly = true)
  public BoardNameCheckResponseDto checkBoardName(Long userId, String name) {
    // 0. 앞뒤 공백 제거
    String cleanName = name != null ? name.trim() : null;

    // 1. 이름 중복 여부
    boolean existsVisible = boardRepository.existsByUser_IdAndNameIgnoreCase(userId, cleanName);
    if (existsVisible) {
      return new BoardNameCheckResponseDto(false, false); // 생성 불가, AI보드 아님
    }

    // 2. 같은 이름의 숨겨진 AI보드 존재 여부
    boolean hiddenAiExists = boardRepository.existsHiddenAiBoardByUserAndName(userId, cleanName);
    if (hiddenAiExists) {
      // 생성 가능(복원), AI보드 이름과 동일함
      return new BoardNameCheckResponseDto(true, true);
    }

    // 3. 기타 정상 생성
    return new BoardNameCheckResponseDto(true, false);
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

    // 4. 멤버십 생성
    // 항상 OWNER 멤버 생성
    if (dto.getBoardType() == BoardType.SECTION || dto.getBoardType() == BoardType.BOARD) {
      boardMemberService.createOwner(savedBoard, owner);
    }

    // 공유 보드일 경우 초대 생성
    if (dto.getBoardType() == BoardType.BOARD && dto.isShared()) {
      Set<String> inviteEmails = Optional.ofNullable(dto.getMemberEmails())
        .orElse(List.of())
        .stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .map(String::toLowerCase)
        .filter(s -> !s.isBlank())
        .filter(s -> !s.equalsIgnoreCase(owner.getEmail()))
        .collect(Collectors.toCollection(LinkedHashSet::new));

      if (!inviteEmails.isEmpty()) {
        List<User> invitees = userService.getUsersByEmails(new ArrayList<>(inviteEmails));
        Set<String> found = invitees.stream()
          .map(u -> u.getEmail().toLowerCase())
          .collect(Collectors.toSet());
        List<String> missing = inviteEmails.stream()
          .filter(e -> !found.contains(e))
          .toList();
        if (!missing.isEmpty()) {
          throw new ApiException(ErrorCode.ENTITY_NOT_FOUND);
        }

        // BoardInvitation 생성 (PENDING)
        boardInvitationService.createInvitations(savedBoard, owner, invitees);
      }
    }

    // 5. 결과 반환
    return boardMapper.toCreateResponseDto(savedBoard);
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
   * 주어진 사용자 ID로 보드 목록을 조회한다. (섹션 제외)
   *
   * @param userId 사용자 ID
   * @param page   페이지 번호
   * @param size   페이지 크기
   * @param sort   정렬 기준
   * @param filter 필터 기준 (전체 / 즐겨찾기 / 공유)
   * @return 보드 요약 DTO 페이지
   */
  @Transactional(readOnly = true)
  public Page<BoardSummaryResponseDto> getUserBoards(
    Long userId, int page, int size, SortType sort, FilterType filter) {

    PageRequest pageable;

    switch (filter) {
      case FAVORITE, SHARED -> {
        pageable = PageRequestUtil.of(page, size, sort, Board.class, "board.");
      }
      default -> {
        pageable = PageRequestUtil.of(page, size, sort, Board.class);
      }
    }

    // 1. 보드 조회 (필터별 분기)
    Page<Board> boards = switch (filter) {
      case FAVORITE -> boardRepository.findFavoriteBoards(userId, pageable);
      case SHARED -> boardRepository.findSharedBoards(userId, pageable);
      default -> boardRepository.findAccessibleBoards(userId, BoardType.BOARD, pageable);
    };

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
    Map<Long, String> thumbnailMap = getThumbnailsForBoards(boards.getContent());

    // 매핑
    return boards.map(board -> {
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
    });
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
  public BoardDetailResponseDto getBoardDetail(
    Long boardId,
    Long userId,
    int page,
    int size,
    SortType sort,
    FilterType filter
  ) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    if (board.getSource() == BoardSource.USER) {
      boolean isOwner = board.getUser().getId().equals(userId);
      boolean isMember = boardMemberService.existsByBoardAndUser(boardId, userId);

      if (!isOwner && !isMember) {
        throw new ApiException(ErrorCode.ACCESS_DENIED);
      }
    }

    // 마지막 방문 시간 갱신
    boardMemberService.updateLastVisitedAt(boardId, userId);

    // 즐겨찾기 상태 조회
    boolean favorite = boardMemberService.getFavoriteStatus(boardId, userId);

    // 섹션 리스트 (상단 고정 리스트를 위해 page값 고정)
    PageRequest sectionPageable = PageRequestUtil.of(0, size, sort, Board.class);
    Page<Board> sectionPage = filter == FilterType.FAVORITE
      ? boardRepository.findFavoriteSectionsByParentBoardId(boardId, userId, sectionPageable)
      : boardRepository.findByParentBoardId(boardId, sectionPageable);

    List<Long> sectionIds = sectionPage.stream().map(Board::getId).toList();
    Map<Long, Boolean> sectionFavoriteMap = sectionIds.isEmpty()
      ? Map.of()
      : boardMemberService.getFavoriteMapByUserAndBoardIds(userId, sectionIds);

    List<BoardSummaryResponseDto> sections = sectionPage.getContent().stream()
      .map(section -> {
        long cardCount = cardRepository.countActiveByBoardId(section.getId());
        String thumbnailUrl = null;
        if (cardCount > 0) {
          var top1 = cardRepository.findRecentCardsByBoard(section, PageRequest.of(0, 1));
          thumbnailUrl = !top1.isEmpty() && top1.get(0).getVideo() != null
            ? top1.get(0).getVideo().getThumbnailUrl()
            : null;
        }
        boolean sectionFavorite = sectionFavoriteMap.getOrDefault(section.getId(), false);
        return boardMapper.toSummaryResponseDto(section, 0L, cardCount, thumbnailUrl,
          sectionFavorite);
      })
      .toList();

    // 카드 리스트
    PageRequest cardPageable = PageRequestUtil.of(page, size, sort, Card.class);
    Page<Card> cardPage = filter == FilterType.FAVORITE
      ? cardRepository.findFavoriteCardsByBoard(boardId, userId, cardPageable)
      : cardRepository.findActiveCardsByBoard(boardId, cardPageable);

    // 상태 한 번에 조회
    List<Long> cardIds = cardPage.stream().map(Card::getId).toList();
    Map<Long, CardUserStatus> statusMap = cardUserStatusService.getStatusMap(userId, cardIds);

    // DTO 변환
    Page<CardSimpleResponseDto> cards = cardPage.map(card -> {
      CardUserStatus status = statusMap.get(card.getId());
      boolean isFavorite = status != null && Boolean.TRUE.equals(status.getIsFavorite());
      boolean viewed = status != null && status.getViewedAt() != null;
      return cardMapper.toSimpleResponseDto(card, isFavorite, viewed);
    });

    return boardMapper.toDetailResponseDto(board, sections, cards, favorite);
  }

  /**
   * 홈 화면용 보드 이름 리스트 조회
   *
   * @param userId 조회할 사용자 ID
   * @return 보드 이름 리스트 DTO
   */
  @Transactional(readOnly = true)
  public List<BoardSimpleResponseDto> getBoardsForHome(Long userId, SortType sort) {
    List<Board> boards = boardRepository.findBoardsWithUnviewedCards(userId, BoardType.BOARD,
      sort.name());
    return boards.stream()
      .map(boardMapper::toSimpleResponseDto)
      .toList();
  }

  /**
   * 홈 화면용 최근 방문한 보드 리스트 조회
   *
   * @param userId 조회할 사용자 ID
   * @return 보드 이름 리스트 DTO
   */
  public List<BoardPreviewResponseDto> getRecentVisitedBoards(Long userId, int limit) {
    List<BoardMember> members = boardMemberService.findRecentVisitedBoards(userId, limit);

    List<Board> boards = members.stream()
      .map(BoardMember::getBoard)
      .toList();
    Map<Long, String> thumbnailMap = getThumbnailsForBoards(boards);

    return members.stream()
      .map(bm -> boardMapper.toPreviewResponseDto(
        bm.getBoard(),
        thumbnailMap.get(bm.getBoard().getId())
      ))
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
    List<Board> boards = boardRepository.findAllAccessibleBoards(userId);

    // 보드 + 모든 섹션 id 수집
    List<Long> allBoardIds = boards.stream()
      .flatMap(board -> {
        List<Long> ids = new ArrayList<>();
        ids.add(board.getId()); // 부모 보드
        ids.addAll(
          board.getSections().stream()
            .map(Board::getId)
            .toList()
        ); // 자식 섹션들
        return ids.stream();
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
   * 현재 사용자의 모든 기본보드를 조회한다.
   *
   * @param userId 조회할 사용자 ID
   * @return 보드 이름 리스트 DTO
   */
  @Transactional(readOnly = true)
  public List<BoardSimpleResponseDto> getUserDefaultBoards(Long userId) {
    List<Board> boards = boardRepository.findAllDefaultBoardsByUserId(userId);
    return boards.stream()
      .filter(board -> !"기타".equals(board.getName()))
      .map(boardMapper::toSimpleResponseDto)
      .toList();
  }

  /**
   * 보드 이름으로 검색한다.
   *
   * @param userId  검색 요청 사용자 ID
   * @param keyword 검색 키워드
   * @param sort    정렬 기준
   * @return 검색된 보드 리스트
   */
  @Transactional(readOnly = true)
  public List<BoardSummaryResponseDto> searchBoards(Long userId, String keyword, SortType sort) {
    if (keyword == null || keyword.trim().isEmpty()) {
      throw new ApiException(ErrorCode.FIELD_REQUIRED);
    }

    // 1. 보드 검색 (이름 기준)
    List<Board> boards = boardRepository.searchBoardsByName(userId, keyword, sort.name());

    if (boards.isEmpty()) {
      return List.of();
    }

    List<Long> boardIds = boards.stream()
      .map(Board::getId)
      .toList();

    // 2. 즐겨찾기 여부 조회 (BoardMember 기반)
    Map<Long, Boolean> favoriteMap =
      boardMemberService.getFavoriteMapByUserAndBoardIds(userId, boardIds);

    // 3. 통계 조회 (섹션/카드 카운트)
    List<BoardStatsDto> stats = boardRepository.getBoardStats(boardIds);
    Map<Long, BoardStatsDto> statsMap = stats.stream()
      .collect(Collectors.toMap(BoardStatsDto::getBoardId, Function.identity()));

    // 4. 썸네일 조회
    Map<Long, String> thumbnailMap = getThumbnailsForBoards(boards);

    // 5. 매핑
    return boards.stream()
      .map(board -> {
        BoardStatsDto stat = statsMap.getOrDefault(
          board.getId(),
          new BoardStatsDto(board.getId(), 0L, 0L)
        );
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
   * 멤버 초대 생성
   *
   * @param boardId       대상 보드 ID
   * @param currentUserId 요청 사용자 ID (보드 소유자여야 함)
   * @param dto           초대 대상 이메일 목록
   * @return 초대 응답 DTO 목록
   */
  @Transactional
  public List<BoardInvitationResponseDto> inviteMembers(Long boardId, Long currentUserId,
    BoardInvitationRequestDto dto) {

    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 권한 체크 (보드 소유자만 가능)
    if (board.getSource() != BoardSource.USER || !board.getUser().getId().equals(currentUserId)) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    List<User> invitees = userService.getUsersByEmails(dto.getEmails());
    boardInvitationService.createInvitations(board, board.getUser(), invitees);

    // 방금 생성된 초대들 반환
    List<BoardInvitation> invitations = boardInvitationService.getInvitations(board);
    return invitations.stream()
      .filter(inv -> inv.getStatus() == InvitationStatus.PENDING)
      .map(inv -> BoardInvitationResponseDto.builder()
        .invitationId(inv.getId())
        .email(inv.getInvitee().getEmail())
        .nickname(inv.getInvitee().getNickname())
        .status(inv.getStatus())
        .build())
      .toList();
  }

  /**
   * 초대 취소 (삭제 처리)
   *
   * @param boardId       대상 보드 ID
   * @param currentUserId 요청 사용자 ID (보드 소유자여야 함)
   * @param invitationId  취소할 초대 ID
   */
  @Transactional
  public void cancelInvitation(Long boardId, Long currentUserId, Long invitationId) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 보드 소유자만 가능
    if (board.getSource() != BoardSource.USER || !board.getUser().getId().equals(currentUserId)) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    BoardInvitation invitation = boardInvitationService.findById(invitationId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    if (invitation.getStatus() == InvitationStatus.PENDING) {
      boardInvitationService.delete(invitation);
    } else {
      throw new ApiException(ErrorCode.INVALID_STATE);
    }
  }

  /**
   * 멤버 + 초대 목록 조회
   *
   * @param boardId       대상 보드 ID
   * @param currentUserId 요청 사용자 ID
   * @return 멤버 및 초대 목록 응답
   */
  @Transactional(readOnly = true)
  public BoardMemberListResponseDto getMembersWithInvitations(Long boardId, Long currentUserId) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    if (board.getSource() != BoardSource.USER || !board.getUser().getId().equals(currentUserId)) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    List<BoardMember> members = boardMemberService.getMembers(board);
    List<BoardInvitation> invitations = boardInvitationService.getInvitations(board);

    return boardMemberMapper.toMemberListResponseDto(board, members, invitations);
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
   * 선택된 보드와 카드를 복제한다.
   *
   * 규칙:
   * - AI 보드는 복제 불가
   * - 사용자 보드는 하위 섹션과 카드까지 포함 복제
   * - 카드가 대상 보드에 이미 존재하면 새 카드 엔티티를 만들어 "(1)", "(2)" 같은 suffix 붙임
   * - 섹션도 동일 구조로 복제
   * - 대상 보드가 공유 보드거나 null(루트)이면 예외 발생
   *
   * @param sourceBoardId 요청이 발생한 원본 보드 ID (컨텍스트용)
   * @param dto           복제 요청 (boardIds, cardIds, targetBoardId)
   * @param userId        요청 사용자 ID
   * @return 생성된 보드/카드 ID 리스트와 대상 보드 ID
   */
  @Transactional
  public BulkActionResponseDto copyBoardsAndCards(Long sourceBoardId, BulkActionRequestDto dto,
    Long userId) {

    // 1. 대상 보드 확인 (루트 복제 금지)
    if (dto.getTargetBoardId() == null) {
      throw new ApiException(ErrorCode.FIELD_REQUIRED);
    }

    Board targetBoard = boardRepository.findById(dto.getTargetBoardId())
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    if (targetBoard.isShared()) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    List<Long> createdBoardIds = new ArrayList<>();
    List<Long> createdCardIds = new ArrayList<>();

    // 2. 보드 복제
    if (dto.getBoardIds() != null) {
      for (Long boardId : dto.getBoardIds()) {
        Board source = boardRepository.findById(boardId)
          .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));
        if (source.getSource() == BoardSource.AI) {
          continue;
        }

        String newName = resolveDuplicateBoardName(source.getName(), targetBoard, userId);
        User user = userService.getUserById(userId);
        Board copied = boardMapper.toCopiedBoard(source, newName, user, targetBoard);
        boardRepository.save(copied);
        boardMemberService.createOwner(copied, user);
        createdBoardIds.add(copied.getId());

        List<BoardCard> boardCards = boardCardService.getByBoardId(source.getId());
        for (BoardCard bc : boardCards) {
          Long newCardId = copyOrLinkCard(bc.getCard(), copied, userId);
          if (newCardId != null) {
            createdCardIds.add(newCardId);
          }
        }
      }
    }

    // 3. 카드 복제
    if (dto.getCardIds() != null) {
      for (Long cardId : dto.getCardIds()) {
        Card card = cardRepository.findById(cardId)
          .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));
        Long newCardId = copyOrLinkCard(card, targetBoard, userId);
        if (newCardId != null) {
          createdCardIds.add(newCardId);
        }
      }
    }

    // 4. 결과 반환
    return BulkActionResponseDto.builder()
      .boardIds(createdBoardIds)
      .cardIds(createdCardIds)
      .targetBoardId(targetBoard.getId())
      .build();
  }

  /**
   * 선택된 보드와 카드를 이동한다.
   *
   * 규칙:
   * - AI 보드, 공유 보드는 이동 불가
   * - 보드 이동: 다른 보드 밑으로 가면 섹션으로 전환, 상위 제거되면 보드로 승격
   * - 카드 이동: sourceBoardId와의 링크 제거 후 targetBoard에 새 링크 추가
   * - 카드가 targetBoard에 이미 있으면 무시
   * - 대상 보드가 공유 보드면 예외 발생
   *
   * @param sourceBoardId 원본 보드 ID
   * @param dto           이동 요청 (boardIds, cardIds, targetBoardId)
   * @param userId        요청 사용자 ID
   * @return 이동된 보드/카드 ID 리스트와 대상 보드 ID
   */
  @Transactional
  public BulkActionResponseDto moveBoardsAndCards(Long sourceBoardId, BulkActionRequestDto dto,
    Long userId) {

    // 1. 대상 보드 확인 (루트 이동 불가)
    if (dto.getTargetBoardId() == null) {
      throw new ApiException(ErrorCode.FIELD_REQUIRED);
    }

    Board targetBoard = boardRepository.findById(dto.getTargetBoardId())
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    if (targetBoard.isShared()) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    List<Long> movedBoardIds = new ArrayList<>();
    List<Long> movedCardIds = new ArrayList<>();

    // 2. 보드 이동
    if (dto.getBoardIds() != null) {
      for (Long boardId : dto.getBoardIds()) {
        Board source = boardRepository.findById(boardId)
          .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

        // AI 보드, 공유 보드는 이동 불가
        if (source.getSource() == BoardSource.AI || source.isShared()) {
          continue;
        }

        // 같은 대상이면 무시
        if (Objects.equals(source.getParentBoard(), targetBoard)) {
          continue;
        }

        // 보드 → 다른 보드 밑으로 이동 (섹션 전환)
        if (targetBoard.getBoardType() == BoardType.BOARD) {
          source.setBoardType(BoardType.SECTION);
          source.setParentBoard(targetBoard);
        }
        // 보드 승격 (섹션 → 보드로 이동)
        else {
          source.setBoardType(BoardType.BOARD);
          source.setParentBoard(null);
        }

        boardRepository.save(source);
        movedBoardIds.add(source.getId());
      }
    }

    // 3. 카드 이동
    if (dto.getCardIds() != null) {
      for (Long cardId : dto.getCardIds()) {
        Card card = cardRepository.findById(cardId)
          .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

        // source → target 이동
        boolean alreadyLinked = boardCardService.exists(targetBoard.getId(), cardId);
        if (alreadyLinked) {
          continue;
        }

        // 기존 소스 보드와의 링크 제거
        if (sourceBoardId != null) {
          boardCardService.detachCard(sourceBoardId, cardId);
        }

        // 타겟 보드에 링크 추가
        boardCardService.add(targetBoard, card);
        movedCardIds.add(card.getId());
      }
    }

    // 4. 결과 반환
    return BulkActionResponseDto.builder()
      .boardIds(movedBoardIds)
      .cardIds(movedCardIds)
      .targetBoardId(targetBoard.getId())
      .build();
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
    List<Long> targetBoardIds = targets.stream()
      .map(Board::getId)
      .toList();

    int linksDetached = 0;
    int cardsSoftDeleted = 0;

    // 3. 대상 보드들에 연결된 카드 수집
    Map<Long, List<Long>> boardCardMap = new HashMap<>();
    for (Long targetId : targetBoardIds) {
      List<Long> cardIds = boardCardService.findDistinctCardIdsByBoardIds(List.of(targetId));
      if (!cardIds.isEmpty()) {
        boardCardMap.put(targetId, cardIds);
      }
    }

    // 전체 카드 목록 (soft delete 처리용)
    List<Long> allCardIds = boardCardMap.values().stream()
      .flatMap(Collection::stream)
      .distinct()
      .toList();

    // 4. 옵션에 따른 카드 처리
    try {
      if (!targetBoardIds.isEmpty()) {
        // (공통) 보드/섹션 내 카드 링크 제거
        linksDetached = boardCardService.detachByBoardIds(targetBoardIds);
      }

      if (option == DeleteLinkedCardsOption.DELETE_ORPHANS && !allCardIds.isEmpty()) {
        // (선택) 고아 카드도 soft delete
        cardsSoftDeleted = cardRepository.softDeleteByIds(allCardIds, userId, LocalDateTime.now());
      }
    } catch (Exception e) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // 5. AI 기본 보드: 숨김 처리
    if (root.getSource() == BoardSource.AI) {

      // 자식 섹션 목록
      List<Long> sectionIds = targets.stream()
        .filter(t -> t.getBoardType() == BoardType.SECTION && !t.getId().equals(root.getId()))
        .map(Board::getId)
        .toList();

      // (1) 자식 섹션 soft delete
      if (!sectionIds.isEmpty()) {
        boardRepository.softDeleteByIds(sectionIds, userId, LocalDateTime.now());
      }

      // (2) 카드 soft delete
      if (!allCardIds.isEmpty()) {
        cardsSoftDeleted += cardRepository.softDeleteByIds(allCardIds, userId, LocalDateTime.now());
      }

      // 보드-카드 링크 제거
      if (!targetBoardIds.isEmpty()) {
        linksDetached = boardCardService.detachByBoardIds(targetBoardIds);
      }

      // (3) 루트 보드 숨김 처리
      boardMemberService.hideBoardForUser(root.getId(), userId);

      // (4) 복원용 카드 매핑값 저장
      List<CardRestoreRequestDto> cardRestores = boardCardMap.entrySet().stream()
        .map(e -> CardRestoreRequestDto.builder()
          .boardId(e.getKey())
          .cardIds(e.getValue())
          .build()
        )
        .toList();

      return BoardDeleteResultDto.builder()
        .boardId(root.getId())
        .status("HIDDEN")
        .option(option.name())
        .linksDetached(linksDetached)
        .cardsSoftDeleted(cardsSoftDeleted)
        .sectionsDeleted(sectionIds.size())
        .deletedSectionIds(sectionIds)
        .cardRestores(cardRestores)
        .build();
    }

    // 6. 사용자 보드: 섹션 포함 soft delete
    try {
      if (!targetBoardIds.isEmpty()) {
        // 멤버십 삭제
        boardMemberService.deleteByBoardIds(targetBoardIds);

        // 하위 섹션 포함 soft delete
        LocalDateTime now = LocalDateTime.now();
        boardRepository.softDeleteByIds(targetBoardIds, userId, now);

        int sectionsDeleted = (int) targets.stream()
          .filter(t -> t.getBoardType() == BoardType.SECTION)
          .count();

        List<CardRestoreRequestDto> cardRestores = boardCardMap.entrySet().stream()
          .map(e -> CardRestoreRequestDto.builder()
            .boardId(e.getKey())
            .cardIds(e.getValue())
            .build())
          .toList();

        return BoardDeleteResultDto.builder()
          .boardId(boardId)
          .status("SOFT_DELETED")
          .option(option.name())
          .linksDetached(linksDetached)
          .cardsSoftDeleted(cardsSoftDeleted)
          .sectionsDeleted(sectionsDeleted)
          .deletedSectionIds(
            targets.stream()
              .filter(t -> t.getBoardType() == BoardType.SECTION)
              .map(Board::getId)
              .toList()
          )
          .cardRestores(cardRestores)
          .build();

      }
    } catch (Exception e) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    // fallback (보드 없음 등)
    return BoardDeleteResultDto.builder()
      .boardId(boardId)
      .status("NO_ACTION")
      .option(option.name())
      .build();
  }

  /**
   * 삭제된 보드를 복원한다.
   *
   * @param req    복원할 정보를 담은 DTO
   * @param userId 현재 요청을 보낸 사용자 ID
   * @return 복원된 보드 갯수 DTO
   */
  @Transactional
  public BoardRestoreResponseDto restoreBoards(BoardRestoreRequestDto req, Long userId) {
    int restored = 0;

    List<Long> restoredBoards = new ArrayList<>();
    List<Long> restoredSections = new ArrayList<>();
    List<Long> restoredCards = new ArrayList<>();

    // 보드 복원
    if (req.getBoardIds() != null && !req.getBoardIds().isEmpty()) {
      for (Long boardId : req.getBoardIds()) {
        Board board = boardRepository.findById(boardId)
          .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

        boolean isOwner = board.getUser() != null && board.getUser().getId().equals(userId);
        boolean isMember = boardMemberService.existsByBoardAndUser(boardId, userId);

        if (!isOwner && !isMember) {
          throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        if (board.getSource() == BoardSource.AI) {
          boardMemberService.restoreVisibleForUser(boardId, userId);
          restored++;
          restoredBoards.add(boardId);
          continue;
        }

        if (board.getSource() == BoardSource.USER) {
          User owner = board.getUser();
          boardMemberService.createOwner(board, owner);
        }

        if (board.isDeleted()) {
          board.restore();
          restored++;
          restoredBoards.add(boardId);
        }
      }
    }

    // 섹션 복원 (soft-deleted 상태)
    if (req.getSectionIds() != null && !req.getSectionIds().isEmpty()) {
      int count = boardRepository.restoreByIds(req.getSectionIds());
      if (count > 0) {
        restored += count;
        restoredSections.addAll(req.getSectionIds());

        for (Long sectionId : restoredSections) {
          Board section = boardRepository.findById(sectionId)
            .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));
          if (section.getSource() == BoardSource.USER) {
            boardMemberService.restoreVisibleForUser(sectionId, userId);
          }
        }
      }
    }

    // 카드 복원 (soft-deleted 상태)
    if (req.getCardRestores() != null && !req.getCardRestores().isEmpty()) {
      CardService cardService = applicationContext.getBean(CardService.class);

      for (CardRestoreRequestDto cardReq : req.getCardRestores()) {
        if (cardReq.getCardIds() == null || cardReq.getCardIds().isEmpty()) {
          continue;
        }

        CardRestoreResponseDto result = cardService.restoreCards(cardReq, userId);
        restored += result.getRestoredCount();
        restoredCards.addAll(cardReq.getCardIds());
      }
    }

    return BoardRestoreResponseDto.builder()
      .restoredCount(restored)
      .restoredBoardIds(restoredBoards)
      .restoredSectionIds(restoredSections)
      .restoredCardIds(restoredCards)
      .build();
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
   * 보드 썸네일 추출
   */
  private Map<Long, String> getThumbnailsForBoards(List<Board> boards) {
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

    return thumbnailMap;
  }

  // 벌크 액션 헬퍼 메서드

  // 카드 복제/링크 처리
  private Long copyOrLinkCard(Card original, Board targetBoard, Long userId) {
    if (targetBoard == null) {
      // 루트에 카드를 직접 복제할 수 없음 → 무시
      return null;
    }

    User user = userService.getUserById(userId);

    String newTitle = resolveDuplicateCardTitle(original.getTitle(), targetBoard);
    Card copied = cardMapper.toCopiedCard(original, newTitle, user);
    cardRepository.save(copied);

    boardCardService.add(targetBoard, copied);

    return copied.getId();
  }

  // 보드 이름 중복 처리
  private String resolveDuplicateBoardName(String baseName, Board targetBoard, Long userId) {
    String candidate = baseName;
    int count = 1;
    User user = userService.getUserById(userId);

    if (targetBoard == null) {
      while (boardRepository.existsByUserAndNameAndParentBoardIsNull(user, candidate)) {
        candidate = baseName + " (" + count + ")";
        count++;
      }
    } else {
      while (boardRepository.existsByNameConflict(candidate, user, targetBoard)) {
        candidate = baseName + " (" + count + ")";
        count++;
      }
    }
    return candidate;
  }

  // 카드 제목 중복 처리
  private String resolveDuplicateCardTitle(String baseTitle, Board targetBoard) {
    if (targetBoard == null) {
      return baseTitle; // 루트에 카드는 없음
    }
    String candidate = baseTitle;
    int count = 1;
    while (boardCardService.existsByTitleInBoard(targetBoard.getId(), candidate)) {
      candidate = baseTitle + " (" + count + ")";
      count++;
    }
    return candidate;
  }

  // 특정 사용자의 AI 기본보드 중 주어진 카테고리(DefaultBoard)에 해당하는 보드를 조회한다.
  @Transactional(readOnly = true)
  public Board getAiBoardByUserAndCategory(Long userId, DefaultBoard category) {
    return boardRepository.findByUserIdAndName(userId, category.getDisplayName())
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));
  }

  // AI 보드 visible 처리
  @Transactional
  public void ensureVisibleForUser(Long boardId, Long userId) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    if (board.getSource() == BoardSource.AI) {
      boardMemberService.enableVisibility(board, userId);
    }
  }

}
