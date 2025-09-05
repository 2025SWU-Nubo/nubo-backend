package com.nubo.domain.video.service;

import com.nubo.domain.video.dto.VideoMetadataDto;
import com.nubo.domain.video.entity.Video;
import com.nubo.domain.video.repository.VideoRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VideoService {

  private final VideoRepository videoRepository;

  /**
   * 메타데이터 기반으로 기존 영상을 찾거나, 없으면 새로 생성한다.
   */
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

  /**
   * 영상 ID로 영상을 조회한다. (없으면 Optional.empty 반환)
   */
  @Transactional(readOnly = true)
  public Optional<Video> getVideoById(String videoId) {
    return videoRepository.findById(videoId);
  }

}
