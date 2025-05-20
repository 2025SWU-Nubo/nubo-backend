package com.nubo.domain.video.entity;

import com.nubo.domain.video.type.Platform;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video extends BaseTimeEntity {

  @Id
  private String id; // YouTube 영상 고유 ID

  @Column(nullable = false)
  private String title; // 원본 영상 제목

  @Column(nullable = false)
  private String url; // 영상 URL

  @Column(nullable = false)
  private String thumbnailUrl; // 썸네일 이미지 URL

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Platform platform; // YOUTUBE, INSTAGRAM, TIKTOK

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(columnDefinition = "TEXT")
  private String transcript;

  @Column(columnDefinition = "TEXT")
  private String subtitle;
}
