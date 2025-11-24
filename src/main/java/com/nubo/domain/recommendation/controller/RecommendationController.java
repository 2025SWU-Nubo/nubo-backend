package com.nubo.domain.recommendation.controller;

import com.nubo.domain.recommendation.dto.RecommendationResponseDto;
import com.nubo.domain.recommendation.service.RecommendationService;
import com.nubo.global.auth.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home/recommendation")
public class RecommendationController {

  private final UserUtil userUtil;
  private final RecommendationService recommendationService;

  /**
   * 오늘의 추천 카드 조회
   */
  @GetMapping
  public ResponseEntity<RecommendationResponseDto> getRecommendations() {
    Long userId = userUtil.getAuthenticatedUserId();
    RecommendationResponseDto response =
      recommendationService.getRecommendations(userId);
    return ResponseEntity.ok(response);
  }
}
