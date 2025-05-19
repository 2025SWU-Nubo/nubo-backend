package com.nubo.domain.video.service;

import com.nubo.domain.card.dto.CardRequestDto;
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

  /**
   * videoId 기준으로 기존 영상을 조회하거나, 존재하지 않을 경우 새로 생성해 저장한다.
   *
   * CardRequestDto 내 video 관련 필드를 기반으로 Video 엔티티를 구성하며,
   * 최초 등록 시에만 저장소에 persist된다. 이후에는 동일 ID 기준으로 중복 저장되지 않는다.
   *
   * @param dto 카드 생성 요청 DTO (영상 관련 필드 포함)
   * @return 기존 또는 새로 저장된 Video 엔티티
   */
  public Video getOrCreateVideo(CardRequestDto dto) {
    return videoRepository.findById(dto.getVideoId())
      .orElseGet(() -> {
        Video newVideo = videoMapper.toEntity(dto);
        return videoRepository.save(newVideo);
      });
  }
}
