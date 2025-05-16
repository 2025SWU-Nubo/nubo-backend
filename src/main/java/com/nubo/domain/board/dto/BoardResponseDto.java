package com.nubo.domain.board.dto;

import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardResponseDto {

  private Long id;
  private String name;
  private BoardType boardType;
  private BoardSource source;
  private boolean isShared;
  private boolean isFavorite;
  private LocalDateTime lastVisitedAt;
}
