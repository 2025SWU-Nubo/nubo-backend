package com.nubo.domain.recommendation.mapper;

import com.nubo.domain.recommendation.dto.RecommendationCardResponseDto;
import com.nubo.domain.recommendation.dto.RecommendationGroupResponseDto;
import com.nubo.domain.recommendation.dto.RecommendationResponseDto;
import com.nubo.domain.recommendation.entity.RecommendationCard;
import com.nubo.domain.recommendation.entity.RecommendationGroup;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecommendationMapper {

  /**
   * 그룹 + 카드 DTO 빌드
   */
  public RecommendationResponseDto toRecommendationResponseDto(List<RecommendationGroup> groups) {

    List<RecommendationGroupResponseDto> responseGroups = groups.stream()
      .map(g -> RecommendationGroupResponseDto.builder()
        .groupId(g.getId())
        .groupType(g.getGroupType())
        .keyword(g.getKeyword())
        .category(g.getCategory())
        .cards(
          g.getCards().stream()
            .map(this::toCardDto)
            .toList()
        )
        .build()
      )
      .toList();

    return RecommendationResponseDto.builder()
      .groups(responseGroups)
      .build();
  }

  /**
   * 추천 카드 엔티티 -> dto
   */
  public RecommendationCardResponseDto toCardDto(RecommendationCard card) {

    return RecommendationCardResponseDto.builder()
      .cardId(card.getId())
      .videoThumbnailUrl(card.getThumbnailUrl())
      .build();
  }
}
