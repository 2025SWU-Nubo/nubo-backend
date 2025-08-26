package com.nubo.domain.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCardsDetachResultDto {
  
  private Long cardId;   // 카드 ID
  private String status; // 처리 상태 (OK | NOT_LINKED | FAILED)
  private String action; // 성공 시 액션 값 (예: DETACHED)
  private String error;  // 실패 시 에러 코드
}