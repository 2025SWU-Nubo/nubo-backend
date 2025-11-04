package com.nubo.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoardNameCheckResponseDto {

  private boolean available; // 생성 가능한지 여부 (중복 아닐 때 true)
  private boolean aiBoard;   // AI 기본보드 이름인지 여부
}
