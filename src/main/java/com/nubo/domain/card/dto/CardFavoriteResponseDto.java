package com.nubo.domain.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardFavoriteResponseDto {

  private Long cardId;
  private boolean favorite;
}
