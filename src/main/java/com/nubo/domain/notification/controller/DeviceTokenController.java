package com.nubo.domain.notification.controller;

import com.nubo.domain.notification.service.DeviceTokenService;
import com.nubo.global.auth.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push/token")
@RequiredArgsConstructor
public class DeviceTokenController {

  private final DeviceTokenService deviceTokenService;
  private final UserUtil userUtil;

  /**
   * FCM 토큰 등록 또는 갱신
   */
  @PostMapping
  public ResponseEntity<Void> registerToken(@RequestBody TokenRequestDto request) {
    Long userId = userUtil.getAuthenticatedUserId();
    deviceTokenService.registerOrUpdateToken(userId, request.token());
    return ResponseEntity.ok().build();
  }

  /**
   * FCM 토큰 삭제
   */
  @DeleteMapping
  public ResponseEntity<Void> deleteToken(@RequestBody TokenRequestDto request) {
    Long userId = userUtil.getAuthenticatedUserId();
    deviceTokenService.deleteToken(userId, request.token());
    return ResponseEntity.noContent().build();
  }

  /**
   * 요청 DTO (내부 클래스로 정의)
   */
  record TokenRequestDto(String token) {

  }
}
