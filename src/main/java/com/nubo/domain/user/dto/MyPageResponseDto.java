package com.nubo.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageResponseDto {

  private String name;         // 사용자 이름
  private String email;        // 사용자 이메일
  private String profileImageUrl; // 프로필 이미지 URL
}
