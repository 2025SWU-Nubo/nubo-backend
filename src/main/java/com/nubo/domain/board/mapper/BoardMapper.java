package com.nubo.domain.board.mapper;

import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardResponseDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.user.entity.User;
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
      .lastVisitedAt(board.getLastVisitedAt())
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
      .user(user)
      .parentBoard(parentBoard)
      .build();
  }
}
