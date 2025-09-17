package com.nubo.domain.user.entity;

import com.nubo.domain.user.type.Provider;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Provider provider;

  // 소셜 로그인에서 받은 고유 ID (예: 구글의 sub 값)
  @Column(nullable = false, unique = true)
  private String providerUserId;

  private String nickname;

  @Column(columnDefinition = "TEXT")
  private String profileImageUrl;

  private Boolean pushEnabled;

  @Column(nullable = false, unique = true)
  private String email;

  /**
   * 관심사 설정 완료 여부
   * - 최초 회원가입 직후 false
   * - /api/interests API 호출 완료 시 true
   * - 이후 재호출 불가
   */
  @Column(nullable = false)
  private boolean interestSetupCompleted = false;

  // 사용자 닉네임 수정
  public void updateNickname(String nickname) {
    this.nickname = nickname;
  }

  // 프로필 이미지 변경
  public void updateProfileImageUrl(String profileImageUrl) {
    this.profileImageUrl = profileImageUrl;
  }

  // 관심사 설정 완료로 상태 변경
  public void markInterestSetupCompleted() {
    this.interestSetupCompleted = true;
  }
}
