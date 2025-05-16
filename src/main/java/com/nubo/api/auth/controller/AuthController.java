package com.nubo.api.auth.controller;

import com.nubo.api.auth.dto.LoginRequestDto;
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
   * Google 로그인 요청을 처리한다.
   *
   * @param request 클라이언트로부터 받은 Google access token
   * @return JWT 토큰과 사용자 정보가 담긴 응답
   */
  @PostMapping("/login/google")
  public ResponseEntity<LoginResponseDto> loginWithGoogle(@RequestBody LoginRequestDto request) {
    LoginResponseDto response = authService.loginWithGoogle(request.accessToken());
    return ResponseEntity.ok(response);
  }
}
