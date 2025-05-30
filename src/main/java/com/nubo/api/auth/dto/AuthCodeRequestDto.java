package com.nubo.api.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuthCodeRequestDto {

  private String authCode;
}
