package com.nubo.domain.card.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiCardMetaDto {

  private String title;
  private String summary;
  private List<String> tags;
  private Long boardId;
  private String aiCategory;
}
