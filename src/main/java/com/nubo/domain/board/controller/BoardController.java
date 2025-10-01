package com.nubo.domain.board.controller;

import com.nubo.domain.board.dto.BoardCardsDetachRequestDto;
import com.nubo.domain.board.dto.BoardCardsDetachResultDto;
import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardCreateResponseDto;
import com.nubo.domain.board.dto.BoardDeleteRequestDto;
import com.nubo.domain.board.dto.BoardDeleteResultDto;
import com.nubo.domain.board.dto.BoardDetailResponseDto;
import com.nubo.domain.board.dto.BoardFavoriteRequestDto;
import com.nubo.domain.board.dto.BoardFavoriteResponseDto;
import com.nubo.domain.board.dto.BoardInvitationRequestDto;
import com.nubo.domain.board.dto.BoardInvitationResponseDto;
import com.nubo.domain.board.dto.BoardMemberListResponseDto;
import com.nubo.domain.board.dto.BoardNameCheckResponseDto;
import com.nubo.domain.board.dto.BoardShareRequestDto;
import com.nubo.domain.board.dto.BoardShareResponseDto;
import com.nubo.domain.board.dto.BoardSimpleResponseDto;
import com.nubo.domain.board.dto.BoardSummaryResponseDto;
import com.nubo.domain.board.dto.BoardUpdateNameRequestDto;
import com.nubo.domain.board.dto.BoardWithSectionsSimpleResponseDto;
import com.nubo.domain.board.service.BoardInvitationService;
import com.nubo.domain.board.service.BoardService;
import com.nubo.global.auth.UserUtil;
import com.nubo.global.common.FilterType;
import com.nubo.global.common.SortType;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardController {

  private final BoardService boardService;
  private final UserUtil userUtil;
  private final BoardInvitationService boardInvitationService;

  /**
   * 보드 이름 중복 여부를 확인한다.
   *
   * @param name 확인할 보드 이름
   * @return 사용 가능 여부 DTO
   */
  @GetMapping("/check-name")
  public ResponseEntity<BoardNameCheckResponseDto> checkBoardName(@RequestParam String name) {
    Long userId = userUtil.getAuthenticatedUserId();
    boolean available = boardService.isBoardNameAvailable(userId, name);
    return ResponseEntity.ok(new BoardNameCheckResponseDto(available));
  }

  /**
   * 로그인한 사용자의 새 보드 또는 섹션을 생성한다.
   *
   * @param dto 생성할 보드 정보 DTO
   * @return 생성된 보드 정보 DTO
   */
  @PostMapping
  public BoardCreateResponseDto createBoard(@RequestBody @Valid BoardCreateRequestDto dto) {
    Long userId = userUtil.getAuthenticatedUserId();
    return boardService.createBoard(dto, userId);
  }

  /**
   * 로그인한 사용자의 보드 목록을 조회한다. (섹션 정보 미포함)
   *
   * @param page   페이지 번호
   * @param size   페이지 크기
   * @param sort   정렬 기준
   * @param filter 필터 기준 (전체 / 즐겨찾기 / 공유)
   * @return 사용자의 보드 리스트
   */
  @GetMapping
  public ResponseEntity<Page<BoardSummaryResponseDto>> getUserBoards(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "LATEST") SortType sort,
    @RequestParam(defaultValue = "ALL") FilterType filter
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    Page<BoardSummaryResponseDto> response =
      boardService.getUserBoards(userId, page, size, sort, filter);
    return ResponseEntity.ok(response);
  }

  /**
   * 보드 ID로 상세 정보를 조회한다.
   *
   * @param boardId 조회할 보드 ID
   * @param page    페이지 번호
   * @param size    페이지 크기
   * @param sort    정렬 기준
   * @param filter  필터 기준 (전체 / 즐겨찾기 / 공유)
   * @return 보드 상세 정보 응답 DTO
   */
  @GetMapping("/{boardId}")
  public ResponseEntity<BoardDetailResponseDto> getBoardDetail(
    @PathVariable Long boardId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "LATEST") SortType sort,
    @RequestParam(defaultValue = "ALL") FilterType filter
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    BoardDetailResponseDto detail =
      boardService.getBoardDetail(boardId, userId, page, size, sort, filter);
    return ResponseEntity.ok(detail);
  }

  /**
   * 카드 추가 시 사용자가 선택할 수 있는
   * 보드와 섹션 목록을 계층 구조로 반환한다.
   *
   * @return 보드+섹션 이름 정보 응답 DTO
   */
  @GetMapping("/with-sections")
  public ResponseEntity<List<BoardWithSectionsSimpleResponseDto>> getBoardsWithSections() {
    Long userId = userUtil.getAuthenticatedUserId();
    List<BoardWithSectionsSimpleResponseDto> result = boardService.getBoardsWithSections(userId);
    return ResponseEntity.ok(result);
  }

  /**
   * 관심사 설정을 위한 기본 보드를 모두 조회한다.
   *
   * @return 보드 이름 정보 응답 DTO
   */
  @GetMapping("/defaults")
  public ResponseEntity<List<BoardSimpleResponseDto>> getDefaultBoards() {
    Long userId = userUtil.getAuthenticatedUserId();
    List<BoardSimpleResponseDto> result = boardService.getUserDefaultBoards(userId);
    return ResponseEntity.ok(result);
  }

  /**
   * 키워드로 보드를 검색한다.
   *
   * @param keyword 검색 키워드 (보드 이름)
   * @param sort    정렬 기준 (기본값: 최신순)
   * @return 검색 결과 보드 목록
   */
  @GetMapping("/search")
  public ResponseEntity<List<BoardSummaryResponseDto>> searchBoards(
    @RequestParam String keyword,
    @RequestParam(defaultValue = "LATEST") SortType sort
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    List<BoardSummaryResponseDto> result = boardService.searchBoards(userId, keyword, sort);
    return ResponseEntity.ok(result);
  }

  /**
   * 사용자 보드를 공유 보드로 전환한다.
   *
   * @param boardId    대상 보드 ID (PathVariable)
   * @param requestDto 공유 여부 요청 DTO (현재 정책상 true만 허용)
   * @return 공유 상태가 반영된 응답 DTO
   */
  @PatchMapping("/{boardId}/share")
  public ResponseEntity<BoardShareResponseDto> updateShareStatus(
    @PathVariable Long boardId,
    @RequestBody BoardShareRequestDto requestDto) {

    Long currentUserId = userUtil.getAuthenticatedUserId();
    BoardShareResponseDto result =
      boardService.updateShareStatus(boardId, currentUserId, requestDto.isShared());

    return ResponseEntity.ok(result);
  }

  /**
   * 공유 보드에 새로운 멤버를 초대한다.
   *
   * @param boardId    대상 보드 ID
   * @param requestDto 초대할 사용자 이메일 목록
   * @return 생성된 초대 목록
   */
  @PostMapping("/{boardId}/invitation")
  public ResponseEntity<List<BoardInvitationResponseDto>> inviteMembers(
    @PathVariable Long boardId,
    @RequestBody BoardInvitationRequestDto requestDto
  ) {
    Long currentUserId = userUtil.getAuthenticatedUserId();
    List<BoardInvitationResponseDto> result =
      boardService.inviteMembers(boardId, currentUserId, requestDto);
    return ResponseEntity.ok(result);
  }

  /**
   * 공유 보드 초대를 취소한다. (초대 목록에서 삭제)
   *
   * @param boardId      대상 보드 ID
   * @param invitationId 초대를 취소할 ID
   */
  @DeleteMapping("/{boardId}/invitation/{invitationId}")
  public ResponseEntity<Void> cancelInvitation(
    @PathVariable Long boardId,
    @PathVariable Long invitationId
  ) {
    Long currentUserId = userUtil.getAuthenticatedUserId();
    boardService.cancelInvitation(boardId, currentUserId, invitationId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 공유 보드의 멤버와 초대 현황을 조회한다.
   *
   * @param boardId 대상 보드 ID
   * @return 멤버 목록과 초대 목록
   */
  @GetMapping("/{boardId}/members")
  public ResponseEntity<BoardMemberListResponseDto> getMembersWithInvitations(
    @PathVariable Long boardId
  ) {
    Long currentUserId = userUtil.getAuthenticatedUserId();
    BoardMemberListResponseDto result =
      boardService.getMembersWithInvitations(boardId, currentUserId);
    return ResponseEntity.ok(result);
  }

  /**
   * 초대 수락
   */
  @PostMapping("/invitation/{invitationId}/accept")
  public ResponseEntity<Void> acceptInvitation(@PathVariable Long invitationId) {
    Long userId = userUtil.getAuthenticatedUserId();
    boardInvitationService.acceptInvitation(userId, invitationId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 초대 거절
   */
  @PostMapping("/invitation/{invitationId}/reject")
  public ResponseEntity<Void> rejectInvitation(@PathVariable Long invitationId) {
    Long userId = userUtil.getAuthenticatedUserId();
    boardInvitationService.rejectInvitation(userId, invitationId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 보드 이름을 수정한다.
   *
   * - 사용자 보드(source=USER)만 가능
   * - 공유 보드(shared=true)는 소유자만 가능
   *
   * @param boardId 보드 ID (PathVariable)
   * @param request 새로운 이름을 담은 DTO
   * @return 수정된 보드의 id, name
   */
  @PatchMapping("/{boardId}/name")
  public ResponseEntity<BoardSimpleResponseDto> updateBoardName(
    @PathVariable Long boardId,
    @RequestBody @Valid BoardUpdateNameRequestDto request
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    BoardSimpleResponseDto response =
      boardService.updateBoardName(boardId, request.getName(), userId);
    return ResponseEntity.ok(response);
  }

  /**
   * 보드 다중 삭제 또는 숨김 처리.
   * - 기본 제공 보드는 삭제되지 않고 숨김 처리됨
   * - 연결된 카드 처리 방식은 옵션(deleteLinkedCards)에 따라 결정됨
   *
   * @param req 삭제할 boardIds와 연결된 카드 처리 옵션
   * @return 각 보드별 처리 결과 목록
   */
  @DeleteMapping
  public ResponseEntity<List<BoardDeleteResultDto>> deleteBoards(
    @Valid @RequestBody BoardDeleteRequestDto req
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    var option = Optional.ofNullable(req.getDeleteLinkedCards())
      .orElse(BoardDeleteRequestDto.DeleteLinkedCardsOption.DETACH_ONLY);

    List<BoardDeleteResultDto> results =
      boardService.deleteBoards(req.getBoardIds(), option, userId);

    return ResponseEntity.ok(results);
  }

  /**
   * 특정 보드에서 여러 카드 제거(카드 자체는 삭제되지 않음).
   * - 보드-카드 연결 관계만 해제
   *
   * @param boardId 카드들을 제거할 보드 ID
   * @param req     제거할 cardIds 목록
   * @return 각 카드별 처리 결과 목록
   */
  @DeleteMapping("/{boardId}/cards")
  public ResponseEntity<List<BoardCardsDetachResultDto>> detachCardsFromBoard(
    @PathVariable Long boardId,
    @RequestBody BoardCardsDetachRequestDto req
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    List<BoardCardsDetachResultDto> results =
      boardService.detachCardsFromBoard(boardId, req.getCardIds(), userId);

    return ResponseEntity.ok(results);
  }

  /**
   * 보드 즐겨찾기 설정/해제 API
   *
   * @param boardId 보드 ID
   * @param request 즐겨찾기 요청 DTO (favorite: true/false)
   * @return 변경된 즐겨찾기 상태
   */
  @PatchMapping("/{boardId}/favorite")
  public ResponseEntity<BoardFavoriteResponseDto> updateBoardFavorite(
    @PathVariable Long boardId,
    @RequestBody BoardFavoriteRequestDto request
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    BoardFavoriteResponseDto response = boardService.updateBoardFavorite(userId, boardId, request);
    return ResponseEntity.ok(response);
  }
}
