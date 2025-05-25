package com.nubo.domain.board.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectionDto {

  private Long id;
  private String name;
}
