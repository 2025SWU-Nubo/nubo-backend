package com.nubo.domain.recommendation.repository;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.recommendation.entity.RecommendationGroup;
import com.nubo.domain.recommendation.type.RecommendationGroupType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationGroupRepository extends JpaRepository<RecommendationGroup, Long> {

  List<RecommendationGroup> findAllByExpiresAtAfter(LocalDateTime today);

  /**
   * 특정 유저의 KEYWORD 그룹을 오늘 기준(expiresAt 이후)만 조회
   */
  List<RecommendationGroup> findAllByUserIdAndGroupTypeAndExpiresAtAfter(
    Long userId,
    RecommendationGroupType groupType,
    LocalDateTime expiresAt
  );

  /**
   * CATEGORY 타입 중, 주어진 카테고리 목록에 포함되는 그룹만 오늘 기준으로 조회
   */
  List<RecommendationGroup> findAllByGroupTypeAndCategoryInAndExpiresAtAfter(
    RecommendationGroupType groupType,
    List<DefaultBoard> categories,
    LocalDateTime expiresAt
  );

  /**
   * CATEGORY 타입 전체를 오늘 기준으로 조회 (관심사 없는 유저용)
   */
  List<RecommendationGroup> findAllByGroupTypeAndExpiresAtAfter(
    RecommendationGroupType groupType,
    LocalDateTime expiresAt
  );

  /**
   * 만료된 그룹 전체 조회
   */
  List<RecommendationGroup> findAllByExpiresAtBefore(LocalDateTime now);
}
