package com.nubo.domain.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardListResponseDto {

  private Long id;
  private String videoThumbnailUrl;
}
