package com.nubo.domain.recommendation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendationGroupResponseDto {

  private Long groupId;
  private String groupType;
  private String keyword;
  private List<RecommendationCardResponseDto> cards;
}
