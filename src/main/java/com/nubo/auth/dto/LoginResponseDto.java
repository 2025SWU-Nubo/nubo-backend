package com.nubo.auth.dto;

import com.nubo.domain.user.dto.UserInfoDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {

  private String accessToken;
  private UserInfoDto user;

  private boolean reactivated;
  private boolean isNewUser;

  private boolean interestSetupCompleted;
  private boolean tutorialCompleted;
}
