package com.nubo.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoardStatsDto {

  private Long boardId;
  private long sectionCount;
  private long cardCount;
}
