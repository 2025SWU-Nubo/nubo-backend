package com.nubo.domain.board.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 보드/카드 다중 액션 요청 DTO
 * - 복제 또는 이동 액션 시 사용
 */
@Getter
@Setter
public class BulkActionRequestDto {

  // 복제/이동할 보드 ID 목록 (섹션 포함)
  private List<Long> boardIds;

  // 복제/이동할 카드 ID 목록
  private List<Long> cardIds;

  // 붙여넣을 대상 보드 ID
  private Long targetBoardId;
}
