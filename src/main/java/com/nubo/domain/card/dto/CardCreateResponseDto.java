package com.nubo.domain.card.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardCreateResponseDto {

  private Long cardId;
  private String title;
  private String summary;
  private List<String> tags;
  private boolean isFavorite;

  private String videoId;
  private String videoThumbnailUrl;

  private List<Long> boardIds;
}
