package com.nubo.domain.video.service;

import com.nubo.domain.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoService {

  private final VideoRepository videoRepository;

}
