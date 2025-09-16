package com.nubo.domain.card.dto;

import com.nubo.domain.board.type.BoardSource;
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

  private BoardSource boardSource;
  private String boardName;

  private String videoUrl;
  private String videoThumbnailUrl;
  private Platform videoPlatform;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
