package com.nubo.domain.recommendation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendationCardResponseDto {

  private Long recCardId;
  private String videoId;
  private String title;
  private String summary;
  private List<String> tags;
  private String thumbnailUrl;
  private boolean saved;
  private String aiCategory;
}
