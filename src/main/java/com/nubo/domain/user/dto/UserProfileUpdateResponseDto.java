package com.nubo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileUpdateResponseDto {

  private Long id;
  private String email;
  private String nickname;
  private String profileImageUrl;
}
