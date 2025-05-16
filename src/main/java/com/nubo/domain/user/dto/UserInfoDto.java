package com.nubo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoDto {

  Long id;
  String nickname;
  String profileImage;
}
