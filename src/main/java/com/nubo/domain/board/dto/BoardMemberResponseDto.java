package com.nubo.domain.board.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardMemberResponseDto {

  private Long userId;
  private String nickname;
}
