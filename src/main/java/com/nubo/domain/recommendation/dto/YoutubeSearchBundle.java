package com.nubo.domain.recommendation.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class YoutubeSearchBundle {

  private String searchKeyword;
  private List<YoutubeVideoResult> results;
}
