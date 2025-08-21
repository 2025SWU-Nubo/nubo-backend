package com.nubo.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchResponseDto {

  private Long id;
  private String nickname;
  private String email;
  private String profileImage;
}
