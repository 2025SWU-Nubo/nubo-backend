package com.nubo.domain.card.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CardCreateRequestDto {

  // video 기본 정보
  private String videoUrl;

  // 사용자 지정 보드 (optional)
  private List<Long> boardIds;
}
