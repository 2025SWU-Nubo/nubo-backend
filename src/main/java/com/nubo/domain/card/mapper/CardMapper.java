package com.nubo.domain.card.mapper;

import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.card.dto.CardDetailResponseDto;
import com.nubo.domain.card.dto.CardListResponseDto;
import com.nubo.domain.card.dto.CardResponseDto;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.video.entity.Video;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

  // =========================
  // 엔티티 생성
  // =========================

  // Card 생성 (보드 연결은 BoardCard로 별도 관리)
  public Card toEntity(User user, Video video) {
    return Card.builder()
      .title(video.getTitle())
      .user(user)
      .video(video)
      .isFavorite(false)
      .build();
  }

  // =========================
  // 응답 매핑
  // =========================

  // Card → CardResponseDto (보드 ID 리스트 포함)
  public CardResponseDto toResponseDto(Card card, List<Long> boardIds) {
    return CardResponseDto.builder()
      .id(card.getId())
      .title(card.getTitle())
      .summary(card.getSummary())
      .tags(splitTags(card.getTags()))
      .isFavorite(card.isFavorite())
      .videoId(card.getVideo().getId())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .boardIds(boardIds)
      .build();
  }

  // Card → CardListResponseDto (리스트용, 썸네일만 포함)
  public CardListResponseDto toListResponseDto(Card card) {
    return CardListResponseDto.builder()
      .id(card.getId())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .build();
  }

  // Card → CardDetailResponseDto (보드 컨텍스트 정보 포함 가능)
  public CardDetailResponseDto toDetailResponseDto(
    Card card,
    String contextBoardName,
    BoardSource contextBoardSource
  ) {
    return CardDetailResponseDto.builder()
      .id(card.getId())
      .title(card.getTitle())
      .summary(card.getSummary())
      .tags(splitTags(card.getTags()))
      .boardSource(contextBoardSource)
      .boardName(contextBoardName)
      .videoUrl(card.getVideo().getUrl())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .videoPlatform(card.getVideo().getPlatform())
      .createdAt(card.getCreatedAt())
      .updatedAt(card.getUpdatedAt())
      .build();
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
      .toList();
  }
}
