package com.nubo.domain.card.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardHighlightUpdateRequestDto {

  @NotNull
  private List<HighlightRange> highlights;

  /**
   * 개별 하이라이트 구간 정의 (시작/끝 인덱스)
   */
  @Getter
  @Setter
  public static class HighlightRange {

    private int rangeStart;
    private int rangeEnd;
  }
}
