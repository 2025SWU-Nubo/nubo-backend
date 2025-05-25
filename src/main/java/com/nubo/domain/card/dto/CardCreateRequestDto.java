package com.nubo.domain.card.dto;

import com.nubo.domain.video.type.Platform;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardCreateRequestDto {

  // video 기본 정보
  private String videoUrl;
  private Platform platform;

  // 사용자 지정 보드 (optional)
  private Long boardId;
}
