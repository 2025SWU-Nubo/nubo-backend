package com.nubo.domain.card.dto;

import com.nubo.domain.video.type.Platform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponseDto {

  private Long id;
  private String title;
  private String summary;
  private String tags;
  private boolean isFavorite;

  private String videoId;
  private String videoTitle;
  private String videoThumbnailUrl;
  private Platform platform;

  private Long boardId;
}
