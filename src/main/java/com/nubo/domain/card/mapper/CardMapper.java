package com.nubo.domain.card.mapper;

import com.nubo.domain.board.entity.Board;
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

  /**
   * CardRequestDto → Card 변환
   */
  public Card toEntity(User user, Video video, Board board) {
    return Card.builder()
      .title(video.getTitle())
      .user(user)
      .video(video)
      .board(board)
      .isFavorite(false)
      .build();
  }

  /**
   * Card → CardResponseDto 변환
   */
  public CardResponseDto toResponseDto(Card card) {
    return CardResponseDto.builder()
      .id(card.getId())
      .title(card.getTitle())
      .summary(card.getSummary())
      .tags(splitTags(card.getTags()))
      .isFavorite(card.isFavorite())
      .videoId(card.getVideo().getId())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .boardId(card.getBoard().getId())
      .build();
  }

  /**
   * Card → CardListResponseDto 변환
   */
  public CardListResponseDto toListResponseDto(Card card) {
    return CardListResponseDto.builder()
      .id(card.getId())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .build();
  }

  /**
   * Card → CardDetailResponseDto 변환
   */
  public CardDetailResponseDto toDetailResponseDto(Card card) {
    return CardDetailResponseDto.builder()
      .id(card.getId())
      .title(card.getTitle())
      .summary(card.getSummary())
      .tags(splitTags(card.getTags()))
      .boardSource(card.getBoard().getSource())
      .boardName(card.getBoard().getName())
      .videoUrl(card.getVideo().getUrl())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .videoPlatform(card.getVideo().getPlatform())
      .createdAt(card.getCreatedAt())
      .updatedAt(card.getUpdatedAt())
      .build();
  }

  /**
   * 태그 문자열->리스트형으로 분리
   */
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
