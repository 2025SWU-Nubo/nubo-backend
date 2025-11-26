package com.nubo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoDto {

  Long id;
  String email;
  String nickname;
  String profileImageUrl;
}
