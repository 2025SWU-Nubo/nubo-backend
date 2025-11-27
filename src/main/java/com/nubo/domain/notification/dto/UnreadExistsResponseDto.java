package com.nubo.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UnreadExistsResponseDto {

  private boolean exists;
}