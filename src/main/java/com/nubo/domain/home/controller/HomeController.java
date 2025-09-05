package com.nubo.domain.home.controller;

import com.nubo.domain.board.dto.BoardSimpleResponseDto;
import com.nubo.domain.board.service.BoardService;
import com.nubo.domain.card.dto.CardSimpleResponseDto;
import com.nubo.domain.card.service.CardService;
import com.nubo.global.auth.UserUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

  private final UserUtil userUtil;
  private final BoardService boardService;
  private final CardService cardService;

  /**
   * 홈 화면용 보드 이름 리스트를 조회한다.
   *
   * @return 보드 ID와 이름 리스트
   */
  @GetMapping("/boards")
  public ResponseEntity<List<BoardSimpleResponseDto>> getUserBoardsForHome() {
    Long userId = userUtil.getAuthenticatedUserId();
    List<BoardSimpleResponseDto> boards = boardService.getBoardsForHome(userId);
    return ResponseEntity.ok(boards);
  }

  /**
   * 홈 화면용 미열람 카드 썸네일 리스트를 조회한다.
   *
   * @param boardId 조회할 보드의 ID (PathVariable)
   * @param limit   최대 반환 카드 수 (기본값: 10)
   * @return 미열람 카드 썸네일 DTO 리스트
   */
  @GetMapping("/boards/{boardId}/unviewed-cards")
  public ResponseEntity<List<CardSimpleResponseDto>> getUnviewedCardThumbnails(
    @PathVariable Long boardId,
    @RequestParam(defaultValue = "10") int limit) {
    Long userId = userUtil.getAuthenticatedUserId();
    List<CardSimpleResponseDto> cards = cardService.getUnviewedCardThumbnails(userId, boardId,
      limit);
    return ResponseEntity.ok(cards);
  }
}
