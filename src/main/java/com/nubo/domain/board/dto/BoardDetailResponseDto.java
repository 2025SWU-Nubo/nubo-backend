package com.nubo.domain.board.dto;

import com.nubo.domain.card.dto.CardSimpleResponseDto;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Builder
@Getter
public class BoardDetailResponseDto {

  private Long id;
  private String name;
  private boolean isFavorite;
  private boolean isShared;
  private List<BoardSummaryResponseDto> sections;
  private Page<CardSimpleResponseDto> cards;
}
