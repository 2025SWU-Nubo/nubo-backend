package com.nubo.domain.user.dto;

import com.nubo.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserWithStatusDto {

  private final User user;
  private final boolean reactivated;
  private final boolean isNewUser;
}
