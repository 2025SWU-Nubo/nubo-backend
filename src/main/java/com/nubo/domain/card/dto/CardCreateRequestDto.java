package com.nubo.domain.card.dto;

import com.nubo.domain.video.type.Platform;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardCreateRequestDto {

  // video 기본 정보
  private String videoId;
  private String videoUrl;
  private String thumbnailUrl;
  private Platform platform;

  // ai 가공용 정보
  private String videoTitle;
  private String videoDescription;
  private String transcript;
  private String subtitle;
}
