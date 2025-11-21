package com.nubo.domain.recommendation.scheduler;

import com.nubo.domain.recommendation.entity.RecommendationGroup;
import com.nubo.domain.recommendation.repository.RecommendationGroupRepository;
import com.nubo.domain.recommendation.service.RecommendationGenerationService;
import com.nubo.domain.recommendation.service.RecommendationKeywordService;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationScheduler {

  private final RecommendationGenerationService generationService;
  private final RecommendationKeywordService keywordService;
  private final RecommendationGroupRepository groupRepository;
  private final UserRepository userRepository;

  /**
   * 매일 새벽 5시에 실행
   */
  @Scheduled(cron = "0 0 5 * * *")
  @Transactional
  public void generateDailyRecommendations() {

    log.info("[추천스케줄러] 매일 추천 생성 시작");

    // 0) 전체 사용자 조회
    List<User> users = userRepository.findAll();
    log.info("[추천스케줄러] 대상 사용자 수 = {}", users.size());

    // 1) 개인 추천 클린업
    for (User user : users) {
      generationService.cleanupExpiredGroups(user.getId());
    }

    // 2) 사용자별 키워드 기반 추천 생성
    for (User user : users) {

      Long userId = user.getId();

      try {
        // 키워드 추출 (개인별)
        List<String> keywords = keywordService.extractTopKeywords(userId, 2);

        // 키워드 기반 그룹 2개 생성
        List<RecommendationGroup> groups =
          generationService.createKeywordGroups(userId, keywords);

        // 그룹별 추천카드 생성
        for (RecommendationGroup g : groups) {
          generationService.generateCardsForGroup(g);
        }

      } catch (Exception e) {
        log.warn("[추천스케줄러] 개인 추천 생성 실패 userId={}, reason={}",
          userId, e.getMessage());
      }
    }

    // 3) 인기(공통) 추천 생성
    try {
      generationService.generatePopularRecommendationGroup();
    } catch (Exception e) {
      log.warn("[추천스케줄러] 인기 추천 생성 실패: {}", e.getMessage());
    }

    log.info("[추천스케줄러] 추천 생성 완료");
  }
}
