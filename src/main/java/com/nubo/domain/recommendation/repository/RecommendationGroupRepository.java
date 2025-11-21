package com.nubo.domain.recommendation.repository;

import com.nubo.domain.recommendation.entity.RecommendationGroup;
import com.nubo.domain.recommendation.type.RecommendationGroupType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationGroupRepository extends JpaRepository<RecommendationGroup, Long> {


  List<RecommendationGroup> findAllValidGroupsByUserId(Long userId);

  Optional<RecommendationGroup> findFirstByGroupTypeOrderByCreatedAtDesc(
    RecommendationGroupType groupType);

  List<RecommendationGroup> findAllByUserIdIsNullAndExpiresAtBefore(LocalDateTime now);

  List<RecommendationGroup> findAllByUserIdAndExpiresAtBefore(Long userId, LocalDateTime now);
}
