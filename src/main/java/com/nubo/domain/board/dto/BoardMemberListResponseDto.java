package com.nubo.domain.board.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardMemberListResponseDto {

  private Long boardId;
  private List<BoardMemberResponseDto> members;
}
