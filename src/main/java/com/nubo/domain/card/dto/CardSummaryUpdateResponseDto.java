package com.nubo.domain.card.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardSummaryUpdateResponseDto {

  private Long cardId;
  private String summary;
  private LocalDateTime updatedAt;
}
