package com.nubo.domain.board.dto;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardRestoreResponseDto {

  private int restoredCount;                  // 복원된 보드 개수
  private List<Long> restoredBoardIds;        // 복원된 보드 ID 목록
  private List<Long> restoredSectionIds;      // 복원된 섹션 ID 목록
  private List<Long> restoredCardIds;         // 복원된 카드 ID 목록

  public static BoardRestoreResponseDto of(
    int restoredCount,
    List<Long> restoredBoardIds,
    List<Long> restoredSectionIds,
    List<Long> restoredCardIds
  ) {
    return BoardRestoreResponseDto.builder()
      .restoredCount(restoredCount)
      .restoredBoardIds(restoredBoardIds == null ? Collections.emptyList() : restoredBoardIds)
      .restoredSectionIds(sectionIdsOrEmpty(restoredSectionIds))
      .restoredCardIds(cardIdsOrEmpty(restoredCardIds))
      .build();
  }
  
  private static List<Long> sectionIdsOrEmpty(List<Long> list) {
    return list == null ? Collections.emptyList() : list;
  }

  private static List<Long> cardIdsOrEmpty(List<Long> list) {
    return list == null ? Collections.emptyList() : list;
  }
}
