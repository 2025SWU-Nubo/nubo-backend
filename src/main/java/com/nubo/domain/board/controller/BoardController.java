package com.nubo.domain.board.controller;

import com.nubo.domain.board.dto.BoardCardsDetachRequestDto;
import com.nubo.domain.board.dto.BoardCardsDetachResultDto;
import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardDeleteRequestDto;
import com.nubo.domain.board.dto.BoardDeleteResultDto;
import com.nubo.domain.board.dto.BoardDetailResponseDto;
import com.nubo.domain.board.dto.BoardResponseDto;
import com.nubo.domain.board.dto.BoardSummaryResponseDto;
import com.nubo.domain.board.dto.BoardWithSectionsSimpleResponseDto;
import com.nubo.domain.board.service.BoardService;
import com.nubo.global.auth.UserUtil;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/board")
public class BoardController {

  private final BoardService boardService;
  private final UserUtil userUtil;

  /**
   * 로그인한 사용자의 새 보드 또는 섹션을 생성한다.
   *
   * @param dto 생성할 보드 정보 DTO
   * @return 생성된 보드 정보 DTO
   */
  @PostMapping
  public BoardResponseDto createBoard(@RequestBody @Valid BoardCreateRequestDto dto) {
    Long userId = userUtil.getAuthenticatedUserId();
    return boardService.createBoard(dto, userId);
  }

  /**
   * 로그인한 사용자의 보드 목록을 조회한다. (섹션 정보 미포함)
   *
   * @return 사용자의 보드 리스트
   */
  @GetMapping
  public List<BoardSummaryResponseDto> getUserBoards() {
    Long userId = userUtil.getAuthenticatedUserId();
    return boardService.getUserBoards(userId);
  }

  /**
   * 보드 ID로 상세 정보를 조회한다.
   *
   * @param boardId 조회할 보드 ID
   * @return 보드 상세 정보 응답 DTO
   */
  @GetMapping("/{boardId}")
  public ResponseEntity<BoardDetailResponseDto> getBoardDetail(@PathVariable Long boardId) {
    BoardDetailResponseDto detail = boardService.getBoardDetail(boardId);
    return ResponseEntity.ok(detail);
  }

  /**
   * 카드 추가 시 사용자가 선택할 수 있는
   * 보드와 섹션 목록을 계층 구조로 반환한다.
   */
  @GetMapping("/with-sections")
  public ResponseEntity<List<BoardWithSectionsSimpleResponseDto>> getBoardsWithSections() {
    Long userId = userUtil.getAuthenticatedUserId();
    List<BoardWithSectionsSimpleResponseDto> result = boardService.getBoardsWithSections(userId);
    return ResponseEntity.ok(result);
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
}
