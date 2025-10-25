package com.nubo.domain.board.dto;

import com.nubo.domain.card.dto.CardRestoreRequestDto;
import java.util.List;
import lombok.Getter;

@Getter
public class BoardRestoreRequestDto {

  private List<Long> boardIds;
  private List<Long> sectionIds;
  private List<CardRestoreRequestDto> cardRestores;
}
