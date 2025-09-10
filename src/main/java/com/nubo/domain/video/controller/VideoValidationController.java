package com.nubo.domain.video.controller;

import com.nubo.domain.video.dto.ValidateLinkResponseDto;
import com.nubo.domain.video.service.VideoUrlValidator;
import com.nubo.domain.video.type.Platform;
import com.nubo.global.auth.UserUtil;
import com.nubo.global.error.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoValidationController {

  private final VideoUrlValidator validator;
  private final UserUtil userUtil;

  /**
   * 입력된 링크가 유효한 플랫폼 링크인지 검사한다.
   *
   * @param url 검사할 URL
   * @return { valid: true/false, platform: "YOUTUBE"/"INSTAGRAM"/"TIKTOK" }
   */
  @GetMapping("/validate-link")
  public ResponseEntity<ValidateLinkResponseDto> validateLink(@RequestParam String url) {
    Long userId = userUtil.getAuthenticatedUserId();

    try {
      Platform platform = validator.validatePlatform(url, userId);
      return ResponseEntity.ok(new ValidateLinkResponseDto(true, platform.name()));
    } catch (ApiException e) {
      return ResponseEntity.ok(new ValidateLinkResponseDto(false, null));
    }
  }
}
