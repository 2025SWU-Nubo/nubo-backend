package com.nubo.domain.card.dto;

import com.nubo.domain.card.type.CardDeleteMode;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardDeleteRequestDto {

  private List<Long> cardIds;

  private CardDeleteMode deleteMode;
}
