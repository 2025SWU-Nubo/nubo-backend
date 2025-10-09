package com.nubo.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoardRestoreResponseDto {

  private int restoredCount; // 복원된 보드 개수
}
