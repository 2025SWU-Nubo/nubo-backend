package com.nubo.domain.board.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 보드/카드 다중 액션 응답 DTO
 * - 복제 또는 이동 완료 후 결과 반환
 */
@Getter
@Builder
public class BulkActionResponseDto {

  // 새로 생성되거나 이동된 보드 ID 목록
  private List<Long> boardIds;

  // 새로 생성되거나 이동된 카드 ID 목록
  private List<Long> cardIds;

  // 붙여넣기 대상 보드 ID
  private Long targetBoardId;
}
