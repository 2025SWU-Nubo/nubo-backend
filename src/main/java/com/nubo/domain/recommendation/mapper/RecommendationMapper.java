package com.nubo.domain.recommendation.mapper;

import com.nubo.domain.recommendation.dto.RecommendationCardDetailResponseDto;
import com.nubo.domain.recommendation.dto.RecommendationCardResponseDto;
import com.nubo.domain.recommendation.dto.RecommendationGroupResponseDto;
import com.nubo.domain.recommendation.dto.RecommendationResponseDto;
import com.nubo.domain.recommendation.entity.RecommendationCard;
import com.nubo.domain.recommendation.entity.RecommendationGroup;
import com.nubo.domain.recommendation.type.RecommendationGroupType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class RecommendationMapper {

  /**
   * 그룹 + 카드 DTO 빌드
   */
  public RecommendationResponseDto toRecommendationResponseDto(
    List<RecommendationGroup> groups,
    String nickname) {
    List<RecommendationGroupResponseDto> responseGroups = new ArrayList<>();

    List<Integer> indices = new ArrayList<>(
      IntStream.range(0, groups.size()).boxed().toList()
    );
    Collections.shuffle(indices);

    for (int i = 0; i < groups.size(); i++) {
      RecommendationGroup g = groups.get(i);

      String keyword = g.getGroupType() == RecommendationGroupType.KEYWORD
        ? g.getKeyword()
        : g.getSearchKeyword();

      if (keyword == null) {
        keyword = "관심사";
      }

      // keyword를 포함하는 그룹별 패턴 리스트 생성
      List<String> groupPatterns = getTitlePatterns(keyword, nickname);

      // 그룹 i에 어떤 문구 패턴을 배정할지 결정
      int patternIndex = indices.get(i) % groupPatterns.size();

      // 이 그룹의 title
      String title = groupPatterns.get(patternIndex);

      responseGroups.add(
        RecommendationGroupResponseDto.builder()
          .groupId(g.getId())
          .groupType(g.getGroupType())
          .keyword(keyword)
          .category(g.getCategory())
          .cards(
            g.getCards().stream()
              .map(this::toCardDto)
              .toList()
          )
          .title(title)
          .build()
      );
    }

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
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .build();
  }

  /**
   * 추천 카드 엔티티 -> 상세 dto
   */
  public RecommendationCardDetailResponseDto toDetailResponseDto(
    RecommendationCard card,
    String username,
    Integer matchPercent) {
    return RecommendationCardDetailResponseDto.builder()
      .recommendationCardId(card.getId())
      .title(card.getTitle())
      .summary(card.getSummary())
      .tags(splitTags(card.getTags()))
      .videoUrl(card.getVideo().getUrl())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .videoPlatform(card.getVideo().getPlatform())
      .aiCategoryName(card.getAiCategory().getDisplayName())
      .matchPercent(matchPercent)
      .username(username)
      .createdAt(card.getCreatedAt())
      .updatedAt(card.getUpdatedAt())
      .build();
  }

  // title 문구 리스트
  private List<String> getTitlePatterns(String keyword, String nickname) {
    return List.of(
      keyword + "에 대해 더 알고싶다면?",
      nickname + "님을 위한 " + keyword + " 추천 카드",
      "오늘의 " + keyword + " 추천 카드"
    );
  }

  // =========================
  // 유틸
  // =========================

  private List<String> splitTags(String tags) {
    if (tags == null || tags.isBlank()) {
      return List.of();
    }
    return Arrays.stream(tags.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .map(s -> "# " + s)
      .toList();
  }
}
