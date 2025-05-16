package com.nubo.api.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoDto {

  Long id;
  String nickname;
  String profileImage;
}
