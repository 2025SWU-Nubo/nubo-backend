package com.nubo.domain.recommendation.entity;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recommendation_card")
public class RecommendationCard extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 그룹 FK
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id")
  private RecommendationGroup group;

  private String videoId;
  private String title;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Column(columnDefinition = "TEXT")
  private String tags; // JSON 문자열 or ',' 구분 문자열

  private String thumbnailUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "ai_category", length = 50)
  private DefaultBoard aiCategory;

  @Setter
  private boolean isSaved; // 추천 → 정식 카드로 저장 여부
}
