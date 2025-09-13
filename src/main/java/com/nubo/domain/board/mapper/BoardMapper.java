package com.nubo.domain.board.mapper;

import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardCreateResponseDto;
import com.nubo.domain.board.dto.BoardDetailResponseDto;
import com.nubo.domain.board.dto.BoardFavoriteResponseDto;
import com.nubo.domain.board.dto.BoardShareResponseDto;
import com.nubo.domain.board.dto.BoardSimpleResponseDto;
import com.nubo.domain.board.dto.BoardSummaryResponseDto;
import com.nubo.domain.board.dto.BoardWithSectionsSimpleResponseDto;
import com.nubo.domain.board.dto.BoardWithSectionsSimpleResponseDto.SectionSimpleDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.card.dto.CardSimpleResponseDto;
import com.nubo.domain.user.entity.User;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BoardMapper {

  /**
   * Board → BoardCreateResponseDto 변환
   */
  public BoardCreateResponseDto toCreateResponseDto(Board board) {
    return BoardCreateResponseDto.builder()
      .id(board.getId())
      .name(board.getName())
      .boardType(board.getBoardType())
      .source(board.getSource())
      .isShared(board.isShared())
      .isFavorite(false)
      .parentBoardId(board.getParentBoard() != null ? board.getParentBoard().getId() : null)
      .build();
  }

  /**
   * BoardCreateRequestDto → Board Entity 변환
   */
  public Board toEntity(BoardCreateRequestDto dto, User user, Board parentBoard) {
    return Board.builder()
      .name(dto.getName())
      .boardType(dto.getBoardType())
      .source(BoardSource.USER)
      .shared(dto.isShared())
      .user(user)
      .parentBoard(parentBoard)
      .build();
  }

  /**
   * Board → BoardSummaryResponseDto 변환
   */
  public BoardSummaryResponseDto toSummaryResponseDto(
    Board board,
    long sectionCount,
    long cardCount,
    String thumbnailUrl,
    boolean favorite) {
    return BoardSummaryResponseDto.builder()
      .id(board.getId())
      .name(board.getName())
      .source(board.getSource())
      .isShared(board.isShared())
      .isFavorite(favorite)
      .updatedAt(board.getUpdatedAt())
      .sectionCount(sectionCount)
      .cardCount(cardCount)
      .videoThumbnailUrl(thumbnailUrl)
      .build();
  }

  /**
   * Board + 섹션 + 카드 리스트 → BoardDetailResponseDto 변환
   */
  public BoardDetailResponseDto toDetailResponseDto(
    Board board,
    List<BoardSummaryResponseDto> sections,
    List<CardSimpleResponseDto> cards,
    boolean favorite) {
    return BoardDetailResponseDto.builder()
      .id(board.getId())
      .name(board.getName())
      .isFavorite(favorite)
      .isShared(board.isShared())
      .sections(sections)
      .cards(cards)
      .build();
  }

  /**
   * Board → BoardWithSectionsSimpleResponseDto
   */
  public BoardWithSectionsSimpleResponseDto toWithSectionsSimpleResponseDto(
    Board board,
    boolean favorite,
    Map<Long, Boolean> favoriteMap
  ) {
    return BoardWithSectionsSimpleResponseDto.builder()
      .id(board.getId())
      .name(board.getName())
      .isFavorite(favorite)
      .sections(board.getSections().stream()
        .map(section -> SectionSimpleDto.builder()
          .id(section.getId())
          .name(section.getName())
          .isFavorite(favoriteMap.getOrDefault(section.getId(), false))
          .build())
        .collect(Collectors.toList()))
      .build();
  }


  /**
   * Board → BoardSimpleResponseDto
   */
  public BoardSimpleResponseDto toSimpleResponseDto(Board board) {
    return new BoardSimpleResponseDto(board.getId(), board.getName());
  }

  public BoardFavoriteResponseDto toFavoriteResponseDto(Board board, boolean favorite) {
    return BoardFavoriteResponseDto.builder()
      .boardId(board.getId())
      .favorite(favorite)
      .build();
  }

  /**
   * Board → BoardShareResponseDto
   */
  public BoardShareResponseDto toShareResponseDto(Board board) {
    return BoardShareResponseDto.builder()
      .boardId(board.getId())
      .shared(board.isShared())
      .build();
  }
}
