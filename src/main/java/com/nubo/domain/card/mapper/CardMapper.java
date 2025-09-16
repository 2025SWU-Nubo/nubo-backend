package com.nubo.domain.card.mapper;

import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.card.dto.CardDetailResponseDto;
import com.nubo.domain.card.dto.CardHighlightUpdateRequestDto;
import com.nubo.domain.card.dto.CardHighlightUpdateResponseDto;
import com.nubo.domain.card.dto.CardResponseDto;
import com.nubo.domain.card.dto.CardSimpleResponseDto;
import com.nubo.domain.card.dto.CardSummaryUpdateResponseDto;
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
      .build();
  }

  // =========================
  // 응답 매핑
  // =========================

  // Card → CardResponseDto (보드 ID 리스트 포함)
  public CardResponseDto toResponseDto(Card card, List<Long> boardIds) {
    return CardResponseDto.builder()
      .cardId(card.getId())
      .title(card.getTitle())
      .summary(card.getSummary())
      .tags(splitTags(card.getTags()))
      .videoId(card.getVideo().getId())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .boardIds(boardIds)
      .build();
  }

  // Card → CardSimpleResponseDto (리스트용, 썸네일만 포함)
  public CardSimpleResponseDto toListResponseDto(Card card) {
    return CardSimpleResponseDto.builder()
      .cardId(card.getId())
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
      .cardId(card.getId())
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

  // Card → CardSummaryUpdateResponseDto (summary 수정)
  public CardSummaryUpdateResponseDto toSummaryUpdateResponseDto(Card card) {
    return CardSummaryUpdateResponseDto.builder()
      .cardId(card.getId())
      .summary(card.getSummary())
      .updatedAt(card.getUpdatedAt())
      .build();
  }

  // Card → CardHighlightUpdateResponseDto (Highlight 수정)
  public CardHighlightUpdateResponseDto toHighlightUpdateResponseDto(
    Card card,
    List<CardHighlightUpdateRequestDto.HighlightRange> highlights
  ) {
    return CardHighlightUpdateResponseDto.builder()
      .cardId(card.getId())
      .highlights(highlights)
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
