package com.nubo.domain.recommendation.entity;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.recommendation.type.RecommendationGroupType;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "recommendation_group")
public class RecommendationGroup extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // null이면 공통 추천, 값 있으면 개인 추천
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "group_type", length = 50)
  private RecommendationGroupType groupType; // KEYWORD / CATEGORY

  // groupType = CATEGORY일 때
  @Enumerated(EnumType.STRING)
  private DefaultBoard category;

  @Setter
  @Column(name = "search_keyword")
  private String searchKeyword; // 카테고리 기반 검색 키워드

  // groupType = KEYWORD일 때
  private String keyword;

  private LocalDateTime expiresAt;

  @Setter
  private boolean isCardGenerated;  // 그룹 내 카드 생성 완료 여부

  @Setter
  @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RecommendationCard> cards = new ArrayList<>();

}
