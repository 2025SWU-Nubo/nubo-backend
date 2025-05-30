package com.nubo.api.auth.controller;

import com.nubo.api.auth.dto.AuthCodeRequestDto;
import com.nubo.api.auth.dto.LoginResponseDto;
import com.nubo.api.auth.service.AuthService;
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
}
