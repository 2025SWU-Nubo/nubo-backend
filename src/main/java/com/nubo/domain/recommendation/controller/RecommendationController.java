package com.nubo.domain.recommendation.controller;

import com.nubo.domain.board.service.BoardCardService;
import com.nubo.domain.card.dto.CardCreateResponseDto;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.card.mapper.CardMapper;
import com.nubo.domain.recommendation.dto.RecommendationListResponseDto;
import com.nubo.domain.recommendation.service.RecommendationGenerationService;
import com.nubo.domain.recommendation.service.RecommendationService;
import com.nubo.global.auth.CustomUserDetails;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home/recommendation")
public class RecommendationController {

  private final RecommendationService recommendationService;
  private final CardMapper cardMapper;
  private final BoardCardService boardCardService;
  private final RecommendationGenerationService recommendationGenerationService;

  @GetMapping
  public RecommendationListResponseDto getRecommendations(
    @AuthenticationPrincipal CustomUserDetails user
  ) {
    return recommendationService.getRecommendations(user.getId());
  }

  @PostMapping("/{recCardId}/save")
  public CardCreateResponseDto saveRecommendation(
    @AuthenticationPrincipal CustomUserDetails user,
    @PathVariable Long recCardId
  ) {
    Card saved = recommendationService.saveRecommendedCard(recCardId, user.getId());
    return cardMapper.toResponseDto(saved,
      boardCardService.findBoardIdsByCardId(saved.getId()));
  }

  @PostMapping("/test/user/{userId}")
  public String testUserRecommendations(@PathVariable Long userId) {
    recommendationGenerationService.generateRecommendationsForUser(userId);
    return "OK - user recommendations generated";
  }

  @PostMapping("/test/popular")
  public String testPopularRecommend() throws IOException, InterruptedException {
    recommendationGenerationService.generatePopularRecommendationGroup();
    return "OK - popular recommendations generated";
  }

  @PostMapping("/test/category")
  public String testCategoryRecommend() throws IOException, InterruptedException {
    recommendationGenerationService.generatePopularRecommendationGroup();
    return "OK - popular recommendations generated";
  }
}
