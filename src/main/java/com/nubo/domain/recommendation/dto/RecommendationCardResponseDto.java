package com.nubo.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendationCardResponseDto {

  private Long cardId;
  private String title;
  private String thumbnailUrl;
}
