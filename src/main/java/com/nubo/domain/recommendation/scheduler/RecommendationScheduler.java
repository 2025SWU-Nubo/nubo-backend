package com.nubo.domain.recommendation.scheduler;

import com.nubo.domain.card.service.CardService;
import com.nubo.domain.recommendation.entity.RecommendationGroup;
import com.nubo.domain.recommendation.service.RecommendationGenerationService;
import com.nubo.domain.recommendation.service.RecommendationKeywordService;
import com.nubo.domain.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationScheduler {

  private static final int MIN_CARD_FOR_KEYWORD_REC = 10;   // 키워드 생성을 위한 최소 사용자 카드 수
  private static final int KEYWORD_EXTRACTION_LIMIT = 3;    // 키워드 추출 개수
  private final UserService userService;
  private final CardService cardService;
  private final RecommendationKeywordService recommendationKeywordService;
  private final RecommendationGenerationService recommendationGenerationService;

  /**
   * 매일 새벽 5시에 모든 활성 유저의 추천 데이터를 비동기로 생성
   * (크론 표현식: 초 분 시 일 월 요일)
   */
  @Scheduled(cron = "0 0 14 * * *")
  public void generateRecommendationCardsDaily() {

    log.info("[스케줄러] 추천 생성 시작");

    // 0. 만료된 그룹 삭제
    recommendationGenerationService.cleanupExpiredGroups();

    // 미완료 그룹 조회
    List<RecommendationGroup> unprocessedGroups =
      recommendationGenerationService.getAllUnprocessedGroupsForToday();

    // 미완료 그룹 있을 때만
    if (unprocessedGroups.isEmpty()) {
      // 1. 공통 카테고리 그룹 생성
      recommendationGenerationService.createCategoryGroups();

      // 2. 유저 맞춤형 추천 그룹 생성
      List<Long> userIds = userService.getAllActiveUserIds();

      for (Long userId : userIds) {
        Long cardCount = cardService.getCardCountByUser(userId);

        if (cardCount >= MIN_CARD_FOR_KEYWORD_REC) {
          List<String> keywords =
            recommendationKeywordService.extractTopKeywords(userId, KEYWORD_EXTRACTION_LIMIT);

          recommendationGenerationService.createKeywordGroups(userId, keywords);
        }
      }
      unprocessedGroups = recommendationGenerationService.getAllUnprocessedGroupsForToday();
    }

    // 3. 미완료 그룹에 대해 카드 생성
    for (RecommendationGroup g : unprocessedGroups) {
      recommendationGenerationService.generateCardsForGroupAsync(g.getId());
    }

    log.info("[스케줄러] 추천 생성 완료");
  }
}
