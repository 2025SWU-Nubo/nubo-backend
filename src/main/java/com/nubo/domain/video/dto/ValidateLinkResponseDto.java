package com.nubo.domain.video.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidateLinkResponseDto {

  private boolean valid;
  private String platform;
}
