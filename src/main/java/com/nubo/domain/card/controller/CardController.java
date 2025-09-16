package com.nubo.domain.card.controller;

import com.nubo.domain.card.dto.CardCreateRequestDto;
import com.nubo.domain.card.dto.CardDeleteRequestDto;
import com.nubo.domain.card.dto.CardDetailResponseDto;
import com.nubo.domain.card.dto.CardResponseDto;
import com.nubo.domain.card.dto.CardSimpleResponseDto;
import com.nubo.domain.card.dto.CardSummaryUpdateRequestDto;
import com.nubo.domain.card.dto.CardSummaryUpdateResponseDto;
import com.nubo.domain.card.dto.SummaryPromptRequestDto;
import com.nubo.domain.card.service.CardService;
import com.nubo.global.auth.UserUtil;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
  public ResponseEntity<CardResponseDto> createCard(@RequestBody CardCreateRequestDto dto)
    throws IOException, InterruptedException {
    Long userId = userUtil.getAuthenticatedUserId();
    CardResponseDto response = cardService.createCard(dto, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 로그인한 사용자의 카드 목록을 조회한다.
   *
   * @param sort (optional) 정렬 방식: "latest" 또는 "alphabetical"
   * @return 카드 응답 DTO 리스트
   */
  @GetMapping
  public ResponseEntity<List<CardSimpleResponseDto>> getMyCards(
    @RequestParam(defaultValue = "latest") String sort) {
    Long userId = userUtil.getAuthenticatedUserId();
    List<CardSimpleResponseDto> response = cardService.getCardsByUser(userId, sort);
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
   * 카드 summary를 AI로 재가공한다.
   *
   * @param cardId  카드 ID (PathVariable)
   * @param request 사용자 프롬프트 요청 DTO
   * @return 재가공된 summary 응답 DTO
   */
  @PatchMapping("/{cardId}/summary/ai")
  public ResponseEntity<CardSummaryUpdateResponseDto> regenerateSummary(
    @PathVariable Long cardId,
    @RequestBody SummaryPromptRequestDto request
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    CardSummaryUpdateResponseDto response =
      cardService.regenerateCardSummary(cardId, userId, request.getPrompt());
    return ResponseEntity.ok(response);
  }

  /**
   * 카드 summary를 사용자가 직접 수정한다.
   *
   * PATCH /card/{cardId}/summary
   */
  @PatchMapping("/{cardId}/summary")
  public ResponseEntity<CardSummaryUpdateResponseDto> updateSummary(
    @PathVariable Long cardId,
    @RequestBody @Valid CardSummaryUpdateRequestDto request
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    CardSummaryUpdateResponseDto response =
      cardService.updateCardSummary(cardId, userId, request.getSummary());
    return ResponseEntity.ok(response);
  }

  /**
   * 카드 전역 삭제 (소프트 삭제).
   *
   * @param req 삭제할 카드 ID 목록
   * @return 카드별 처리 결과 리스트
   */
  @DeleteMapping
  public ResponseEntity<Map<String, Object>> deleteCards(@RequestBody CardDeleteRequestDto req) {
    Long userId = userUtil.getAuthenticatedUserId();
    var results = cardService.deleteCardsGlobally(req.getCardIds(), userId);
    return ResponseEntity.ok(Map.of("results", results));
  }
}
