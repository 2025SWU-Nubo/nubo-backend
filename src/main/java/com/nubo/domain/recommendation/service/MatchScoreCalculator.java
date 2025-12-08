package com.nubo.domain.recommendation.service;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.recommendation.entity.RecommendationCard;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 추천카드 matchPercent 계산기.
 * - 유저 카드 10개 이상 → 사용자 카드 태그·제목 기반 개인화 계산
 * - 유저 카드 1~9개 → 관심사 기반 계산
 * - 유저 카드 0개 → null 반환 (UI 미표시)
 */
@Component
public class MatchScoreCalculator {

  /**
   * 추천카드 매칭률 계산
   *
   * @param userCards     사용자 보유 카드
   * @param userInterests 사용자 관심사
   * @param recCard       추천카드
   * @return 퍼센트 (null = 랜덤 fallback)
   */
  public Integer calculateMatchPercent(
    List<Card> userCards,
    List<DefaultBoard> userInterests,
    RecommendationCard recCard
  ) {

    // --------------------------
    // 0) 데이터 기반 판단
    // --------------------------

    // 0-A. 유저 카드 없음 → fallback → null
    if (userCards.isEmpty()) {
      return null;  // UI에서도 표시하지 않음
    }

    // --------------------------
    // 1) 사용자 카드 10개 이상 → 개인화 계산
    // --------------------------
    if (userCards.size() >= 10) {
      return calculateByUserProfile(userCards, recCard);
    }

    // --------------------------
    // 2) 사용자 카드 1~9개 → 관심사 기반 계산
    // --------------------------
    return calculateByInterest(userInterests, recCard);
  }


  // =======================================================
  // 개인화 계산 (사용자 카드 기반)
  // =======================================================
  private Integer calculateByUserProfile(List<Card> userCards, RecommendationCard recCard) {

    // 유저가 가진 모든 태그 수집
    Set<String> userTags = userCards.stream()
      .flatMap(c -> parseTags(c.getTags()).stream())
      .map(String::toLowerCase)
      .collect(Collectors.toSet());

    // 추천카드 태그 파싱
    List<String> recTags = parseTags(recCard.getTags()).stream()
      .map(String::toLowerCase)
      .toList();

    if (recTags.isEmpty()) {
      return 87; // 태그 없으면 기본값
    }

    // 매칭 개수
    long matched = recTags.stream()
      .filter(userTags::contains)
      .count();

    double ratio = (double) matched / recTags.size();

    // 자연스러운 스케일링 (85~99)
    int score = (int) (85 + ratio * 14);

    return Math.min(99, Math.max(85, score));
  }


  // =======================================================
  // 관심사 기반 계산
  // =======================================================
  private Integer calculateByInterest(List<DefaultBoard> interests, RecommendationCard recCard) {

    if (interests == null || interests.isEmpty()) {
      return null; // 관심사도 없으면 미표시
    }

    // 추천카드 카테고리가 유저 관심사에 포함되면 높은 점수
    boolean categoryMatch = interests.contains(recCard.getAiCategory());

    int base = categoryMatch ? 90 : 85;

    List<String> recTags = parseTags(recCard.getTags());

    long matched = recTags.isEmpty() ? 0 : 1; // 태그 있으면 약한 매칭 인정

    int score = base + (int) (matched * 4);

    return Math.min(score, 95);
  }

  // =======================================================
  // 태그 파싱 (comma-separated → List<String>)
  // =======================================================
  private List<String> parseTags(String tags) {
    if (tags == null || tags.isBlank()) {
      return List.of();
    }

    return Arrays.stream(tags.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .toList();
  }
}
