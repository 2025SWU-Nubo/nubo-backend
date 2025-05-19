package com.nubo.domain.card.mapper;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.card.dto.CardRequestDto;
import com.nubo.domain.card.dto.CardResponseDto;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.video.entity.Video;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

  /**
   * CardRequestDto → Card 변환
   */
  public Card toEntity(CardRequestDto dto, User user, Video video, Board board, Board section) {
    return Card.builder()
      .user(user)
      .video(video)
      .title(dto.getCardTitle())
      .summary(dto.getSummary())
      .tags(dto.getTags())
      .board(board)
      .section(section)
      .isFavorite(false)
      .build();
  }

  /**
   * Card → CareResponseDto 변환
   */
  public CardResponseDto toResponseDto(Card card) {
    return CardResponseDto.builder()
      .id(card.getId())
      .title(card.getTitle())
      .summary(card.getSummary())
      .tags(card.getTags())
      .isFavorite(card.isFavorite())
      .videoId(card.getVideo().getId())
      .videoTitle(card.getVideo().getTitle())
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .platform(card.getVideo().getPlatform())
      .boardId(card.getBoard().getId())
      .sectionId(card.getSection() != null ? card.getSection().getId() : null)
      .build();
  }
}
