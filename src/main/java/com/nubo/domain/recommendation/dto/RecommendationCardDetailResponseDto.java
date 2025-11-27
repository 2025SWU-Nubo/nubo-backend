package com.nubo.domain.recommendation.dto;

import com.nubo.domain.video.type.Platform;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationCardDetailResponseDto {

  private Long recommendationCardId;
  private String title;
  private String summary;
  private List<String> tags;

  private String videoUrl;
  private String videoThumbnailUrl;
  private Platform videoPlatform;
  private String aiCategoryName;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
