package com.nubo.domain.recommendation.repository;

import com.nubo.domain.recommendation.entity.UserSavedRecommendation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSavedRecommendationRepository
  extends JpaRepository<UserSavedRecommendation, Long> {

  boolean existsByUserIdAndRecommendationCardId(Long userId, Long cardId);

  Optional<UserSavedRecommendation> findByUserIdAndRecommendationCardId(
    Long userId,
    Long cardId
  );
}