package com.nubo.domain.board.dto;

import com.nubo.domain.board.type.BoardMemberRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardMemberResponseDto {

  private Long userId;
  private String nickname;
  private BoardMemberRole role;
}
