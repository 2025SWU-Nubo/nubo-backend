package com.nubo.domain.board.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardWithSectionsSimpleResponseDto {

  private Long id;
  private String name;
  private boolean isFavorite;
  private List<SectionSimpleDto> sections;

  @Getter
  @Builder
  public static class SectionSimpleDto {

    private Long id;
    private String name;
    private boolean isFavorite;
  }
}
