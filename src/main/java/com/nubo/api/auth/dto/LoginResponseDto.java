package com.nubo.api.auth.dto;

import com.nubo.domain.user.dto.UserInfoDto;

public record LoginResponseDto(
  String accessToken,
  UserInfoDto user
) {

}
