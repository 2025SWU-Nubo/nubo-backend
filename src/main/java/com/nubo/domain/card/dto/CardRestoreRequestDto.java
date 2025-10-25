package com.nubo.domain.card.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CardRestoreRequestDto {

  private List<Long> cardIds;
  private Long boardId;
}
