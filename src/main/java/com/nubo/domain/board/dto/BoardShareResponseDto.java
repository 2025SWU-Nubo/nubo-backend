package com.nubo.domain.board.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardShareResponseDto {

  private Long boardId;
  private boolean shared;
}
