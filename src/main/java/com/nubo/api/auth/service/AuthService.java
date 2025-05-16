package com.nubo.api.auth.service;

import com.nubo.api.auth.dto.GoogleUserInfoDto;
import com.nubo.api.auth.dto.LoginResponseDto;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.mapper.UserMapper;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final GoogleOAuthService googleOAuthService;
  private final UserService userService;
  private final JwtProvider jwtProvider;
  private final UserMapper userMapper;

  /**
   * Google access token으로 사용자 정보를 조회하고 JWT 토큰을 발급한다.
   *
   * @param accessToken Google OAuth access token
   * @return 로그인 응답 DTO (JWT 토큰 + 사용자 정보)
   */
  public LoginResponseDto loginWithGoogle(String accessToken) {
    // 1. 구글 access token으로 사용자 정보 조회
    GoogleUserInfoDto userInfo = googleOAuthService.getUserInfo(accessToken);

    // 2. 우리 서비스 DB에 사용자 등록 or 조회
    User userCandidate = UserMapper.fromGoogleUserInfo(userInfo);
    User user = userService.getOrCreateUser(userCandidate);

    // 3. JWT access token 발급
    String jwt = jwtProvider.createAccessToken(user.getId());

    return new LoginResponseDto(
      jwt,
      userMapper.toDto(user)
    );
  }
}
