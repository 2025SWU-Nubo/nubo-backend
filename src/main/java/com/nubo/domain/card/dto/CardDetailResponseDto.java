package com.nubo.domain.card.dto;

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

  private Long id;
  private String title;
  private String summary;
  private List<String> tags;

  private String videoUrl;
  private String videoThumbnailUrl;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
