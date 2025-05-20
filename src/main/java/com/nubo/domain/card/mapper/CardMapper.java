package com.nubo.domain.card.mapper;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.card.dto.CardCreateRequestDto;
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
  public Card toEntity(CardCreateRequestDto dto, User user, Video video, Board board) {
    return Card.builder()
      .title(video.getTitle())
      .user(user)
      .video(video)
      .board(board)
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
      .videoThumbnailUrl(card.getVideo().getThumbnailUrl())
      .boardId(card.getBoard().getId())
      .build();
  }
}
