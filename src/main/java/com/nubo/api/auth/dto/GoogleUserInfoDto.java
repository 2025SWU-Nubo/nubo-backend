package com.nubo.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserInfoDto(
  String sub,       // 고유 사용자 ID
  String name,
  String picture
) {

}
