package com.nubo.domain.home.controller;

import com.nubo.domain.board.dto.BoardPreviewResponseDto;
import com.nubo.domain.board.dto.BoardSimpleResponseDto;
import com.nubo.domain.board.service.BoardService;
import com.nubo.domain.card.dto.CardSimpleResponseDto;
import com.nubo.domain.card.service.CardService;
import com.nubo.global.auth.UserUtil;
import com.nubo.global.common.SortType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
   * 홈 화면용 최근 방문한 보드 리스트를 조회한다.
   *
   * @param limit 최대 반환 보드 수 (기본값: 5)
   * @return 썸네일을 포함한 보드 DTO 리스트
   */
  @GetMapping("/boards/recent")
  public ResponseEntity<List<BoardPreviewResponseDto>> getRecentVisitedBoards(
    @RequestParam(defaultValue = "5") int limit) {
    Long userId = userUtil.getAuthenticatedUserId();
    List<BoardPreviewResponseDto> boards = boardService.getRecentVisitedBoards(userId, limit);
    return ResponseEntity.ok(boards);
  }

  /**
   * 홈 화면용 보드 이름 리스트를 조회한다.
   *
   * @return 보드 ID와 이름 리스트
   */
  @GetMapping("/boards")
  public ResponseEntity<List<BoardSimpleResponseDto>> getUserBoardsForHome(
    @RequestParam(defaultValue = "LATEST") SortType sort
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    List<BoardSimpleResponseDto> boards = boardService.getBoardsForHome(userId, sort);
    return ResponseEntity.ok(boards);
  }

  /**
   * 홈 화면용 미열람 카드 썸네일 리스트를 조회한다.
   *
   * @param boardIds 조회 요청 보드 id 목록
   * @param limit    조회 개수 (기본값 20)
   * @return 미열람 카드 썸네일 DTO 리스트
   */
  @GetMapping("/boards/unviewed-cards")
  public ResponseEntity<List<CardSimpleResponseDto>> getUnviewedCardThumbnails(
    @RequestParam List<Long> boardIds,
    @RequestParam(defaultValue = "20") int limit) {
    Long userId = userUtil.getAuthenticatedUserId();
    List<CardSimpleResponseDto> cards =
      cardService.getUnviewedCardThumbnails(userId, boardIds, limit);
    return ResponseEntity.ok(cards);
  }

  /**
   * 홈 화면용 미열람 카드 썸네일 리스트를 복제 카드의 중복을 제외한 후 조회한다. ('전체'보드)
   *
   * @param limit 한 번에 조회할 카드 개수 (기본값 20)
   * @return 미시청 카드 리스트 (랜덤 순서, 중복 제거)
   */
  @GetMapping("/boards/all/unviewed-cards")
  public ResponseEntity<List<CardSimpleResponseDto>> getHomeRecommendations(
    @RequestParam(defaultValue = "20") int limit
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    List<CardSimpleResponseDto> result = cardService.getDistinctUnviewedCards(userId, limit);
    return ResponseEntity.ok(result);
  }
}
