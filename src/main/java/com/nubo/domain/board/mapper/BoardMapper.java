package com.nubo.domain.board.mapper;

import com.nubo.domain.board.dto.BoardCreateRequestDto;
import com.nubo.domain.board.dto.BoardResponseDto;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.lang.Nullable;

@Mapper(componentModel = "spring")
public interface BoardMapper {

  /**
   * Board → BoardResponseDto 변환
   */
  BoardResponseDto toResponseDto(Board board);

  /**
   * BoardCreateRequestDto → Board Entity 변환
   * 사용자 생성 보드만 해당되므로 source는 항상 USER, 기본값 필드도 고정
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "source", constant = "USER")
  @Mapping(target = "isShared", constant = "false")
  @Mapping(target = "isFavorite", constant = "false")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Board toEntity(BoardCreateRequestDto dto, User user, @Nullable Board parentBoard);

}
