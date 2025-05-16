package com.nubo.global.auth;

import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserUtil {

  public Long getAuthenticatedUserId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    if (principal instanceof CustomUserDetails userDetails) {
      return userDetails.getId();
    }

    throw new ApiException(ErrorCode.UNAUTHORIZED_CLIENT);
  }
}
