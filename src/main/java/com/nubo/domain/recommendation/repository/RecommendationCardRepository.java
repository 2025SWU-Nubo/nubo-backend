package com.nubo.domain.recommendation.repository;

import com.nubo.domain.recommendation.entity.RecommendationCard;
import com.nubo.domain.recommendation.entity.RecommendationGroup;
import com.nubo.domain.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationCardRepository extends JpaRepository<RecommendationCard, Long> {

  boolean existsByGroupAndVideo(RecommendationGroup group, Video video);
}
