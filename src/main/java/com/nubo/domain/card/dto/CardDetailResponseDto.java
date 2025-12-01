package com.nubo.domain.card.dto;

import com.nubo.domain.card.dto.CardSummaryUpdateRequestDto.HighlightRange;
import com.nubo.domain.video.type.Platform;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDetailResponseDto {

  private Long cardId;
  private String title;
  private String summary;
  private List<String> tags;
  private boolean isFavorite;
  private boolean isMine;

  private String videoUrl;
  private String videoThumbnailUrl;
  private Platform videoPlatform;
  private String aiCategoryName;

  private List<HighlightRange> highlights;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // === 성장 관련 필드 ===
  private int stage;            // 현재 단계 (0~4)
  private boolean berryGained;  // 열매 획득 여부
  private boolean stageUp;      // 단계 달성 여부
}
