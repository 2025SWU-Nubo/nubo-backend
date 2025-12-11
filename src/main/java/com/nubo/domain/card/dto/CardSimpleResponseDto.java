package com.nubo.domain.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSimpleResponseDto {

  private Long cardId;
  private String videoThumbnailUrl;
  private boolean isFavorite;
  private boolean viewed;
  private boolean isMine;
}
