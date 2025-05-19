package com.nubo.domain.video.mapper;

import com.nubo.domain.card.dto.CardRequestDto;
import com.nubo.domain.video.entity.Video;
import org.springframework.stereotype.Component;

@Component
public class VideoMapper {

  /**
   * CardRequestDto 안의 video 관련 필드를 기반으로 Video 엔티티 생성
   */
  public Video toEntity(CardRequestDto dto) {
    return Video.builder()
      .id(dto.getVideoId())
      .title(dto.getVideoTitle())
      .url(dto.getVideoUrl())
      .thumbnailUrl(dto.getThumbnailUrl())
      .platform(dto.getPlatform())
      .build();
  }
}
