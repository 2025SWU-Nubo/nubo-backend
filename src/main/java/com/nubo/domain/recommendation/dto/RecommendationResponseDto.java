package com.nubo.domain.recommendation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendationResponseDto {

  private List<RecommendationGroupResponseDto> groups;
}
