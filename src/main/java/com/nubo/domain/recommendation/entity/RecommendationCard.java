package com.nubo.domain.recommendation.entity;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.video.entity.Video;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "video_id", nullable = false)
  private Video video;

  private String title;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Column(columnDefinition = "TEXT")
  private String tags; // JSON 문자열 or ',' 구분 문자열

  @Enumerated(EnumType.STRING)
  @Column(name = "ai_category", length = 50)
  private DefaultBoard aiCategory;

  @OneToMany(
    mappedBy = "recommendationCard",
    cascade = CascadeType.ALL,
    orphanRemoval = true
  )
  private List<UserSavedRecommendation> savedList = new ArrayList<>();
}
