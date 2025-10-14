package com.nubo.domain.card.dto;

import com.nubo.domain.card.type.CardDeleteMode;
import java.util.List;
import lombok.Getter;

@Getter
public class CardRestoreRequestDto {

  private List<Long> cardIds;
  private Long boardId;
  private CardDeleteMode deleteMode;
}
