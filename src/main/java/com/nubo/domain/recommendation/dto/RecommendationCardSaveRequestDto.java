package com.nubo.domain.recommendation.dto;

import java.util.List;
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
public class RecommendationCardSaveRequestDto {

  private Long recommendationCardId;
  private List<Long> boardIds; // optional, 없으면 AI 보드로 자동 저장
}
