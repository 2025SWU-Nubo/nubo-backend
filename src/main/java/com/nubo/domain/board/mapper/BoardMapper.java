package com.nubo.domain.board.mapper;

import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardDetailResponseDto;
import com.nubo.domain.board.dto.BoardResponseDto;
import com.nubo.domain.board.dto.BoardSimpleResponseDto;
import com.nubo.domain.board.dto.BoardSummaryResponseDto;
import com.nubo.domain.board.dto.BoardWithSectionsSimpleResponseDto;
import com.nubo.domain.board.dto.BoardWithSectionsSimpleResponseDto.SectionSimpleDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.card.dto.CardListResponseDto;
import com.nubo.domain.user.entity.User;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BoardMapper {

  /**
   * Board → BoardResponseDto 변환
   */
  public BoardResponseDto toResponseDto(Board board) {
    return BoardResponseDto.builder()
      .id(board.getId())
      .name(board.getName())
      .boardType(board.getBoardType())
      .source(board.getSource())
      .isShared(board.isShared())
      .isFavorite(board.isFavorite())
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
      .isShared(false)
      .isFavorite(false)
      .user(user)
      .parentBoard(parentBoard)
      .build();
  }

  /**
   * Board → BoardSummaryResponseDto 변환
   */
  public BoardSummaryResponseDto toListResponseDto(Board board, long sectionCount, long cardCount,
    String thumbnailUrl) {
    return BoardSummaryResponseDto.builder()
      .id(board.getId())
      .name(board.getName())
      .source(board.getSource())
      .isShared(board.isShared())
      .isFavorite(board.isFavorite())
      .updatedAt(board.getUpdatedAt())
      .sectionCount(sectionCount)
      .cardCount(cardCount)
      .thumbnailUrl(thumbnailUrl)
      .build();
  }

  /**
   * Board + 섹션 + 카드 리스트 → BoardDetailResponseDto 변환
   */
  public BoardDetailResponseDto toDetailResponseDto(Board board,
    List<BoardSummaryResponseDto> sections,
    List<CardListResponseDto> cards) {
    return BoardDetailResponseDto.builder()
      .id(board.getId())
      .name(board.getName())
      .sections(sections)
      .cards(cards)
      .build();
  }

  /**
   * Board → BoardWithSectionsSimpleResponseDto
   */
  public BoardWithSectionsSimpleResponseDto toWithSectionsSimpleDto(Board board) {
    return BoardWithSectionsSimpleResponseDto.builder()
      .id(board.getId())
      .name(board.getName())
      .sections(board.getSections().stream()
        .map(section -> SectionSimpleDto.builder()
          .id(section.getId())
          .name(section.getName())
          .build())
        .collect(Collectors.toList()))
      .build();
  }

  /**
   * Board 리스트 → BoardWithSectionsSimpleResponseDto
   */
  public List<BoardWithSectionsSimpleResponseDto> toWithSectionsSimpleDtoList(List<Board> boards) {
    return boards.stream()
      .map(this::toWithSectionsSimpleDto)
      .collect(Collectors.toList());
  }

  /**
   * Board → BoardSimpleResponseDto
   */
  public BoardSimpleResponseDto toSimpleDto(Board board) {
    return new BoardSimpleResponseDto(board.getId(), board.getName());
  }
}
