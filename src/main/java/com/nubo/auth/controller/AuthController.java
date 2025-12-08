package com.nubo.auth.controller;

import com.nubo.auth.dto.AuthCodeRequestDto;
import com.nubo.auth.dto.LoginResponseDto;
import com.nubo.auth.dto.RefreshTokenRequestDto;
import com.nubo.auth.dto.TokenCheckRequestDto;
import com.nubo.auth.dto.TokenCheckResponseDto;
import com.nubo.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /**
   * Google 소셜 로그인 요청을 처리한다.
   *
   * 프론트엔드(Android)로부터 전달받은 Google 인증 코드(authCode)를 사용하여
   * Google OAuth 서버에서 access token을 교환하고 사용자 정보를 조회한다.
   * 이후 자체 서버의 로그인 처리를 수행하고 JWT access token을 발급한다.
   *
   * @param requestDto Google 인증 코드가 담긴 요청 본문
   * @return JWT access token과 사용자 정보가 포함된 응답 객체
   */
  @PostMapping("/login/google")
  public ResponseEntity<LoginResponseDto> loginWithGoogle(
    @RequestBody AuthCodeRequestDto requestDto) {
    return ResponseEntity.ok(authService.loginWithGoogle(requestDto.getAuthCode()));
  }

  /**
   * 클라이언트로부터 전달받은 JWT access token의 유효성을 검사한다.
   *
   * @param request access token 정보를 담은 요청 객체
   * @return 토큰의 유효성 및 만료 여부를 담은 응답 DTO
   */
  @PostMapping("/check-token")
  public ResponseEntity<TokenCheckResponseDto> checkToken(
    @RequestBody TokenCheckRequestDto request) {
    TokenCheckResponseDto result = authService.checkTokenValidity(request.getAccessToken());
    return ResponseEntity.ok(result);
  }

  /**
   * Refresh Token을 검증하고 새로운 Access/Refresh Token을 재발급한다.
   *
   * @param requestDto refreshToken 정보
   * @return 새 AccessToken + 새 RefreshToken + 사용자 정보
   */
  @PostMapping("/refresh")
  public ResponseEntity<LoginResponseDto> refreshTokens(
    @RequestBody RefreshTokenRequestDto requestDto
  ) {
    return ResponseEntity.ok(authService.refreshTokens(requestDto.getRefreshToken()));
  }
}
