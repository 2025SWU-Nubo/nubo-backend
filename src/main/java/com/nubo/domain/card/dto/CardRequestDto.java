package com.nubo.domain.card.dto;

import com.nubo.domain.video.type.Platform;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardRequestDto {

  private String videoId;
  private String videoTitle;
  private String videoUrl;
  private String thumbnailUrl;
  private Platform platform;

  private String summary;
  private String tags;

  private String cardTitle;
  private Long boardId;
  private Long sectionId;
}
