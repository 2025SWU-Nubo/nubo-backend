package com.nubo.domain.recommendation.repository;

import com.nubo.domain.recommendation.entity.UserSavedRecommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserSavedRecommendationRepository
  extends JpaRepository<UserSavedRecommendation, Long> {

  boolean existsByUserIdAndRecommendationCardId(Long userId, Long cardId);

  @Query("""
    select usr.recommendationCard.id
    from UserSavedRecommendation usr
    where usr.user.id = :userId
    """)
  List<Long> findSavedRecommendationCardIds(Long userId);
}