package com.nubo.domain.card.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class CardRestoreRequestDto {

  private List<Long> cardIds;
  private Long boardId;
}
