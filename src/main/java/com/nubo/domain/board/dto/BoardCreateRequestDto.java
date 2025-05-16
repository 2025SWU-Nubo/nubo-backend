package com.nubo.domain.board.dto;

import com.nubo.domain.board.type.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class BoardCreateRequestDto {

  @NotBlank
  private String name;

  @NotNull
  private BoardType boardType; // SECTION or BOARD

  private Long parentBoardId; // 섹션인 경우 필요
}
