package com.nubo.domain.board.dto;

import com.nubo.domain.board.type.BoardSource;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardListResponseDto {

  private Long id;
  private String name;
  private BoardSource source;
  private boolean isShared;
  private boolean isFavorite;
  private LocalDateTime updatedAt;

  private long sectionCount;
  private long cardCount;
}
