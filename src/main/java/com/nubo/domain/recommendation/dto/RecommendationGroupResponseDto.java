package com.nubo.domain.recommendation.dto;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.recommendation.type.RecommendationGroupType;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendationGroupResponseDto {

  private Long groupId;
  private RecommendationGroupType groupType;
  private String title;
  private String keyword;         // keyword 그룹일 때만
  private DefaultBoard category;  // category 그룹일 때만
  private List<RecommendationCardResponseDto> cards;
}
