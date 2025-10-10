package com.nubo.domain.card.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardDeleteRequestDto {

  private List<Long> cardIds;

  private DeleteMode deleteMode;

  /**
   * 카드 삭제 모드
   * - DETACH_ONLY: 연결된 보드(혹은 섹션)에서만 제거 (카드 데이터 유지)
   * - SOFT_DELETE: 카드 자체 soft delete
   */
  public enum DeleteMode {
    DETACH_ONLY,
    SOFT_DELETE
  }
}
