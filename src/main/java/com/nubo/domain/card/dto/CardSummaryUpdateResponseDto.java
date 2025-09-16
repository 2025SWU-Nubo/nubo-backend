package com.nubo.domain.card.dto;

import com.nubo.domain.card.dto.CardSummaryUpdateRequestDto.HighlightRange;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardSummaryUpdateResponseDto {

  List<HighlightRange> highlights;
  private Long cardId;
  private String summary;
  private LocalDateTime updatedAt;
}
