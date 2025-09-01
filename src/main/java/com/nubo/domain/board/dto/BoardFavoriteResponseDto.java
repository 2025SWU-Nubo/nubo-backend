package com.nubo.domain.board.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardFavoriteResponseDto {

  private Long boardId;
  private boolean favorite;
}
