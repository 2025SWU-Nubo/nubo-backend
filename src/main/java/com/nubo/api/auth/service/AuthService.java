package com.nubo.api.auth.service;

import com.nubo.api.auth.client.GoogleOAuthClient;
import com.nubo.api.auth.dto.GoogleTokenResponseDto;
import com.nubo.api.auth.dto.GoogleUserInfoDto;
import com.nubo.api.auth.dto.LoginResponseDto;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.mapper.UserMapper;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserService userService;
  private final JwtProvider jwtProvider;
  private final UserMapper userMapper;
  private final GoogleOAuthClient googleOAuthClient;

  /**
   * Google OAuth 인증 코드를 받아 사용자 인증을 수행한다.
   *
   * @param authCode 클라이언트로부터 전달받은 Google 인증 코드
   * @return JWT 및 사용자 정보가 포함된 응답 DTO
   */
  @Transactional
  public LoginResponseDto loginWithGoogle(String authCode) {
    // 1. accessToken 요청
    GoogleTokenResponseDto tokenResponse = googleOAuthClient.requestAccessToken(authCode);

    // 2. 사용자 정보 조회
    GoogleUserInfoDto userInfo = googleOAuthClient.requestUserInfo(tokenResponse.getAccess_token());

    // 3. DB에 사용자 등록 또는 조회
    User userCandidate = userMapper.fromGoogleUserInfo(userInfo);
    User user = userService.getOrCreateUser(userCandidate);

    // 4. 서버 토큰(JWT) 발급
    String jwt = jwtProvider.createAccessToken(user.getId());

    return new LoginResponseDto(jwt, userMapper.toDto(user));
  }
}
