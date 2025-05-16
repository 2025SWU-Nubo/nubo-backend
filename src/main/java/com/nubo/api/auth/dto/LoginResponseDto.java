package com.nubo.api.auth.dto;

public record LoginResponseDto(
  String accessToken,
  UserInfoDto user
) {

}
