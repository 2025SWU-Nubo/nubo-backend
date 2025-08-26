package com.nubo.domain.card.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardDeleteRequestDto {

  private List<Long> cardIds;
}
