package com.nubo.domain.recommendation.service;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.recommendation.dto.RecommendationResponseDto;
import com.nubo.domain.recommendation.entity.RecommendationGroup;
import com.nubo.domain.recommendation.mapper.RecommendationMapper;
import com.nubo.domain.recommendation.repository.RecommendationGroupRepository;
import com.nubo.domain.recommendation.type.RecommendationGroupType;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.entity.UserInterest;
import com.nubo.domain.user.repository.UserInterestRepository;
import com.nubo.domain.user.service.UserService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {

  private final RecommendationGroupRepository groupRepository;
  private final UserInterestRepository userInterestRepository;
  private final UserService userService;
  private final RecommendationMapper recommendationMapper;
  private final RecommendationGenerationService recommendationGenerationService;

  // 하루 기준 — 새벽 5시
  private LocalDateTime today5AM() {
    return LocalDate.now().atTime(5, 0);
  }

  /*
   * 추천 카드 조회
   */
  public RecommendationResponseDto getRecommendations(Long userId) {
    // 0) 유저, 오늘 생성된 그룹 조회
    User user = userService.getUserById(userId);
    List<RecommendationGroup> todayGroups = recommendationGenerationService.getAllGroupsForToday();

    // 1) 개인 키워드 그룹 먼저 확인
    List<RecommendationGroup> keywordGroups = todayGroups.stream()
      .filter(g -> g.getUserId() != null &&
        g.getUserId().equals(userId) &&
        g.getGroupType() == RecommendationGroupType.KEYWORD)
      .toList();

    if (!keywordGroups.isEmpty()) {
      return recommendationMapper.toRecommendationResponseDto(keywordGroups);
    }

    // 2) 관심사 기반 추천
    if (user.isInterestSetupCompleted()) {

      List<DefaultBoard> interests = userInterestRepository.findAllByUserId(userId)
        .stream()
        .map(UserInterest::getCategory)
        .toList();

      List<RecommendationGroup> filtered = todayGroups.stream()
        .filter(g -> g.getGroupType() == RecommendationGroupType.CATEGORY &&
          interests.contains(g.getCategory()))
        .toList();

      return recommendationMapper.toRecommendationResponseDto(filtered);
    }

    // 3) 관심사 없음 → 랜덤 추천
    List<RecommendationGroup> categoryGroups = todayGroups.stream()
      .filter(g -> g.getGroupType() == RecommendationGroupType.CATEGORY)
      .toList();

    List<RecommendationGroup> randomGroups = pickRandom(categoryGroups, 2);

    return recommendationMapper.toRecommendationResponseDto(randomGroups);
  }

  /**
   * 랜덤 그룹 선택
   */
  private List<RecommendationGroup> pickRandom(List<RecommendationGroup> list, int count) {
    if (list.size() <= count) {
      return list;
    }

    return list.stream()
      .sorted((a, b) -> Math.random() > 0.5 ? 1 : -1)
      .limit(count)
      .toList();
  }
}
