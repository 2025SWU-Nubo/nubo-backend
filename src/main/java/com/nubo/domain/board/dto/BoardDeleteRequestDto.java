package com.nubo.domain.board.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardDeleteRequestDto {

  private List<Long> boardIds;           // 다중 보드 ID
  private DeleteLinkedCardsOption deleteLinkedCards; // DETACH_ONLY | DELETE_ORPHANS

  public enum DeleteLinkedCardsOption {
    DETACH_ONLY, DELETE_ORPHANS
  }
}