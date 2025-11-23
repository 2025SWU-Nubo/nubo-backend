//package com.nubo.domain.recommendation.controller;
//
//import com.nubo.domain.board.service.BoardCardService;
//import com.nubo.domain.card.mapper.CardMapper;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/home/recommendation")
//public class RecommendationController {
//
//    private final RecommendationService recommendationService;
//  private final CardMapper cardMapper;
//  private final BoardCardService boardCardService;
//
//  @GetMapping
//  public RecommendationListResponseDto getRecommendations(
//    @AuthenticationPrincipal CustomUserDetails user
//  ) {
//    return recommendationService.getRecommendations(user.getId());
//  }
//
//  @PostMapping("/{recCardId}/save")
//  public CardCreateResponseDto saveRecommendation(
//    @AuthenticationPrincipal CustomUserDetails user,
//    @PathVariable Long recCardId
//  ) {
//    Card saved = recommendationService.saveRecommendedCard(recCardId, user.getId());
//    return cardMapper.toResponseDto(saved,
//      boardCardService.findBoardIdsByCardId(saved.getId()));
//  }
//
//  // ✅ 테스트 성공 (카드 10개 이상, 그룹 2개, 그룹당 카드 6개)
//  @PostMapping("/test/user/{userId}")
//  public String testUserRecommendations(@PathVariable Long userId) {
//    recommendationGenerationService.generateRecommendationsForUser(userId);
//    return "OK - user recommendations generated";
//  }
//
//  // ✅ 테스트 성공 (특정 카테고리 지정, 6개)
//  @PostMapping("/test/category")
//  public String testCategoryRecommend() throws IOException, InterruptedException {
//    recommendationGenerationService.generateRecommendationForCategories();
//    return "OK - popular recommendations generated";
//  }
//}
