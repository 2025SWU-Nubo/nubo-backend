package com.nubo.domain.card.dto;

import com.nubo.domain.card.dto.CardHighlightUpdateRequestDto.HighlightRange;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardHighlightUpdateResponseDto {

  private Long cardId;
  private List<HighlightRange> highlights; // 저장된 하이라이트 배열
  private LocalDateTime updatedAt;
}
