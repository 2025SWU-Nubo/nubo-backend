package com.nubo.domain.video.dto;

import com.nubo.domain.video.type.Platform;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoMetadataDto {

  private String videoId;
  private String videoUrl;
  private String title;
  private String description;
  private String thumbnailUrl;
  private Platform platform;
}
