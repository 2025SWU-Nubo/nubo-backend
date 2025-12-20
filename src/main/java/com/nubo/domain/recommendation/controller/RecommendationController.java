package com.nubo.domain.recommendation.controller;

import com.nubo.domain.card.dto.CardCreateResponseDto;
import com.nubo.domain.recommendation.dto.RecommendationCardDetailResponseDto;
import com.nubo.domain.recommendation.dto.RecommendationCardSaveRequestDto;
import com.nubo.domain.recommendation.dto.RecommendationResponseDto;
import com.nubo.domain.recommendation.scheduler.RecommendationScheduler;
import com.nubo.domain.recommendation.service.RecommendationGenerationService;
import com.nubo.domain.recommendation.service.RecommendationService;
import com.nubo.global.auth.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home/recommendation")
public class RecommendationController {

  private final UserUtil userUtil;
  private final RecommendationService recommendationService;
  private final RecommendationScheduler recommendationScheduler;
  private final RecommendationGenerationService recommendationGenerationService;

  /**
   * 오늘의 추천 컨텐츠 그룹 리스트 조회
   */
  @GetMapping
  public ResponseEntity<RecommendationResponseDto> getRecommendations() {
    Long userId = userUtil.getAuthenticatedUserId();
    RecommendationResponseDto response =
      recommendationService.getRecommendations(userId);
    return ResponseEntity.ok(response);
  }

  /**
   * 특정 카드 ID에 대한 정보를 조회한다.
   *
   * @param cardId 조회할 카드 ID
   * @return 카드 응답 DTO
   */
  @GetMapping("/{cardId}")
  public ResponseEntity<RecommendationCardDetailResponseDto> getRecommendationCard(
    @PathVariable Long cardId) {
    Long userId = userUtil.getAuthenticatedUserId();
    RecommendationCardDetailResponseDto response =
      recommendationService.getRecommendationCardById(cardId, userId);
    return ResponseEntity.ok(response);
  }

  /**
   * 추천카드를 정식 카드로 저장
   */
  @PostMapping("/save")
  public ResponseEntity<CardCreateResponseDto> saveRecommendationCard(
    @RequestBody RecommendationCardSaveRequestDto dto
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    CardCreateResponseDto response =
      recommendationService.saveRecommendationCard(userId, dto);
    return ResponseEntity.ok(response);
  }

  // 스케줄러 수동 실행
  @PostMapping("/admin/run-recommendation-cards")
  public String runRecommendationScheduler() {
    recommendationScheduler.generateRecommendationCardsDaily();
    return "Recommendation card generation started asynchronously.";
  }

  // 특정 그룹의 추천 카드 생성 수동 실행
  @PostMapping("/admin/{groupId}")
  public String runRecommendationByGroup(@PathVariable Long groupId) {
    recommendationGenerationService.generateCardsForGroupAsync(groupId);
    return "Recommendation card generation (by group) started asynchronously.";
  }
}
