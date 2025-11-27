package com.nubo.domain.recommendation.entity;

import com.nubo.domain.user.entity.User;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
  name = "user_recommendation_saved",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_user_saved_card",
      columnNames = {"user_id", "recommendation_card_id"}
    )
  }
)
public class UserSavedRecommendation extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 유저
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // 추천 카드
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recommendation_card_id", nullable = false)
  private RecommendationCard recommendationCard;
}
