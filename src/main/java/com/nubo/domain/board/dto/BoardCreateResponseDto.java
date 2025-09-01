package com.nubo.domain.board.dto;

import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardCreateResponseDto {

  private Long id;
  private String name;
  private BoardType boardType;
  private BoardSource source;
  private boolean isShared;
  private boolean isFavorite;
  private Long parentBoardId;
}
