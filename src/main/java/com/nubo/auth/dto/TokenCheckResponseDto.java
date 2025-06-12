package com.nubo.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenCheckResponseDto {

  private boolean isValid;
  private boolean isExpired;
}