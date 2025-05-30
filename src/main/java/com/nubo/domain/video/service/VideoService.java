package com.nubo.domain.video.service;

import com.nubo.domain.video.dto.VideoMetadataDto;
import com.nubo.domain.video.entity.Video;
import com.nubo.domain.video.mapper.VideoMapper;
import com.nubo.domain.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoService {

  private final VideoRepository videoRepository;
  private final VideoMapper videoMapper;

  public Video getOrCreateVideo(VideoMetadataDto meta) {
    return videoRepository.findById(meta.getVideoId())
      .orElseGet(() -> {
        Video newVideo = Video.builder()
          .id(meta.getVideoId())
          .url(meta.getVideoUrl())
          .title(meta.getTitle())
          .description(meta.getDescription())
          .thumbnailUrl(meta.getThumbnailUrl())
          .platform(meta.getPlatform())
          .build();
        return videoRepository.save(newVideo);
      });
  }

}
