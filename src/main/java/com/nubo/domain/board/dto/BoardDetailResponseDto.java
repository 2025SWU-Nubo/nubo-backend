package com.nubo.domain.board.dto;

import com.nubo.domain.card.dto.CardListResponseDto;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BoardDetailResponseDto {

  private Long id;
  private String name;
  private List<BoardSummaryResponseDto> sections;
  private List<CardListResponseDto> cards;

}
