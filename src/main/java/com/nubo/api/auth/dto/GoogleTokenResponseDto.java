package com.nubo.api.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleTokenResponseDto {

  private String access_token;
  private String expires_in;
  private String refresh_token;
  private String scope;
  private String token_type;
  private String id_token;
}
