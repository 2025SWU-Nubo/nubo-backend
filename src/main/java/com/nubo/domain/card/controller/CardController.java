package com.nubo.domain.card.controller;

import com.nubo.domain.card.dto.CardCreateRequestDto;
import com.nubo.domain.card.dto.CardCreateResponseDto;
import com.nubo.domain.card.dto.CardDeleteRequestDto;
import com.nubo.domain.card.dto.CardDetailResponseDto;
import com.nubo.domain.card.dto.CardFavoriteRequestDto;
import com.nubo.domain.card.dto.CardFavoriteResponseDto;
import com.nubo.domain.card.dto.CardRestoreRequestDto;
import com.nubo.domain.card.dto.CardRestoreResponseDto;
import com.nubo.domain.card.dto.CardSimpleResponseDto;
import com.nubo.domain.card.dto.CardSummaryPromptRequestDto;
import com.nubo.domain.card.dto.CardSummaryUpdateRequestDto;
import com.nubo.domain.card.dto.CardSummaryUpdateResponseDto;
import com.nubo.domain.card.service.CardService;
import com.nubo.global.auth.UserUtil;
import com.nubo.global.common.FilterType;
import com.nubo.global.common.SortType;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/card")
@RequiredArgsConstructor
public class CardController {

  private final CardService cardService;
  private final UserUtil userUtil;

  /**
   * 로그인한 사용자의 새 카드를 생성한다.
   *
   * @param dto 생성할 카드 정보 DTO
   * @return 생성된 카드 정보 DTO
   */
  @PostMapping
  public ResponseEntity<CardCreateResponseDto> createCard(@RequestBody CardCreateRequestDto dto)
    throws IOException, InterruptedException {
    Long userId = userUtil.getAuthenticatedUserId();
    CardCreateResponseDto response = cardService.createCard(dto, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 로그인한 사용자의 카드 목록을 조회한다.
   *
   * @param page   페이지 번호
   * @param size   페이지 크기
   * @param sort   정렬 기준
   * @param filter 필터 기준 (전체 / 즐겨찾기 / 공유)
   * @return 카드 응답 DTO 리스트
   */
  @GetMapping
  public ResponseEntity<Page<CardSimpleResponseDto>> getMyCards(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "LATEST") SortType sort,
    @RequestParam(defaultValue = "ALL") FilterType filter
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    Page<CardSimpleResponseDto> response =
      cardService.getCardsByUser(userId, page, size, sort, filter);
    return ResponseEntity.ok(response);
  }

  /**
   * 특정 카드 ID에 대한 정보를 조회한다.
   *
   * @param cardId 조회할 카드 ID
   * @return 카드 응답 DTO
   */
  @GetMapping("/{cardId}")
  public ResponseEntity<CardDetailResponseDto> getCard(@PathVariable Long cardId) {
    Long userId = userUtil.getAuthenticatedUserId();
    CardDetailResponseDto response = cardService.getCardById(cardId, userId);
    return ResponseEntity.ok(response);
  }

  /**
   * 특정 키워드로 카드를 검색한다.
   *
   * @param keyword 검색 키워드
   * @param sort    정렬 방식 (LATEST, OLDEST, ALPHABET)
   * @return 검색된 카드 리스트
   */
  @GetMapping("/search")
  public ResponseEntity<List<CardSimpleResponseDto>> searchCards(
    @RequestParam String keyword,
    @RequestParam(defaultValue = "LATEST") SortType sort
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    List<CardSimpleResponseDto> result = cardService.searchCards(userId, keyword, sort);
    return ResponseEntity.ok(result);
  }

  /**
   * 카드 summary를 AI로 재가공한다.
   *
   * @param cardId  카드 ID
   * @param request 사용자 프롬프트 요청 DTO
   * @return 재가공된 summary 응답 DTO
   */
  @PatchMapping("/{cardId}/summary/ai")
  public ResponseEntity<CardSummaryUpdateResponseDto> regenerateSummary(
    @PathVariable Long cardId,
    @RequestBody CardSummaryPromptRequestDto request
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    CardSummaryUpdateResponseDto response =
      cardService.regenerateCardSummary(cardId, userId, request.getPrompt());
    return ResponseEntity.ok(response);
  }

  /**
   * 카드 summary를 사용자가 직접 수정한다.
   *
   * @param cardId  카드 ID
   * @param request 새로 수정할 내용을 담은 DTO
   * @return 수정된 summary 응답 DTO
   */
  @PatchMapping("/{cardId}/summary")
  public ResponseEntity<CardSummaryUpdateResponseDto> updateSummary(
    @PathVariable Long cardId,
    @RequestBody @Valid CardSummaryUpdateRequestDto request
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    CardSummaryUpdateResponseDto response =
      cardService.updateCardSummary(cardId, userId, request.getSummary(), request.getHighlights());
    return ResponseEntity.ok(response);
  }

  /**
   * 카드 즐겨찾기 상태를 업데이트한다.
   *
   * @param cardId  즐겨찾기를 설정할 카드의 ID
   * @param request 즐겨찾기 여부 요청 DTO (true: 추가, false: 해제)
   * @return 변경된 즐겨찾기 상태를 담은 응답 DTO
   */
  @PatchMapping("/{cardId}/favorite")
  public ResponseEntity<CardFavoriteResponseDto> updateCardFavorite(
    @PathVariable Long cardId,
    @RequestBody CardFavoriteRequestDto request
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    CardFavoriteResponseDto response = cardService.updateCardFavorite(userId, cardId, request);
    return ResponseEntity.ok(response);
  }

  /**
   * 카드 전역 삭제 (소프트 삭제).
   *
   * @param req 삭제할 카드 ID 목록
   * @return 카드별 처리 결과 리스트
   */
  @DeleteMapping
  public ResponseEntity<Map<String, Object>> deleteCards(
    @RequestBody CardDeleteRequestDto req
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    var results = cardService.deleteCardsByMode(req, userId);
    return ResponseEntity.ok(Map.of("results", results));
  }

  /**
   * 삭제된 카드를 복원한다 (되돌리기)
   *
   * @param req 복원할 카드 ID 목록
   * @return 복원 결과 갯수
   */
  @PatchMapping("/restore")
  public ResponseEntity<CardRestoreResponseDto> restoreCards(
    @RequestBody CardRestoreRequestDto req) {
    Long userId = userUtil.getAuthenticatedUserId();
    CardRestoreResponseDto response = cardService.restoreCards(req.getCardIds(), userId);
    return ResponseEntity.ok(response);
  }
}
