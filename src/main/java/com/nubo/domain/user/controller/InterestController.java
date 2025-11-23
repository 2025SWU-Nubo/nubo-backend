package com.nubo.domain.user.controller;

import com.nubo.domain.user.dto.InterestSetupRequestDto;
import com.nubo.domain.user.dto.InterestSetupResponseDto;
import com.nubo.domain.user.service.InterestService;
import com.nubo.global.auth.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interest")
@RequiredArgsConstructor
public class InterestController {

  private final InterestService interestService;
  private final UserUtil userUtil;

  /**
   * 관심사(기본 보드) 설정
   *
   * @param requestDto 선택된 보드 ID 리스트 / skip 여부
   * @return 설정 완료 여부 및 선택 개수
   */
  @PostMapping
  public ResponseEntity<InterestSetupResponseDto> setupInterests(
    @RequestBody InterestSetupRequestDto requestDto
  ) {
    Long userId = userUtil.getAuthenticatedUserId();
    InterestSetupResponseDto response =
      interestService.setupInterests(userId, requestDto);
    return ResponseEntity.ok(response);
  }
}
