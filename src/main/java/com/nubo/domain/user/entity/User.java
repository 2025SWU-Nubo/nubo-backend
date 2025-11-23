package com.nubo.domain.user.entity;

import com.nubo.domain.user.type.Provider;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

  // 관심사 설정 정보
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserInterest> interests = new ArrayList<>();

  /**
   * 대시보드용 정보
   */
  @Column(nullable = false)
  private int currentDrops;   // 현재 사이클에서 누적된 물방울 개수 (0~25)

  @Column(nullable = false)
  private int berryCount;     // 누적 누베리 개수

  /**
   * 푸시알림 설정 여부 정보
   */
  // 전체 알림
  @Setter
  @Column(nullable = false)
  private boolean pushEnabled = true;

  // 리마인더
  @Setter
  @Column(nullable = false)
  private boolean remindEnabled = true;

  /**
   * 탈퇴 시각 (Soft Delete)
   * null이면 활성 상태
   */
  @Setter
  private LocalDateTime deletedAt;

  // ===== 비즈니스 메서드 =====

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

  // === 성장 관련 메서드 ===

  // 물방울 1개 추가 (하루 최대 5개까지만 반영하는 로직은 서비스에서 처리)
  public void addDrop() {
    if (this.currentDrops < 25) {
      this.currentDrops++;
    }
  }

  // 누베리 1개 획득 후 사이클 초기화
  public void gainBerry() {
    this.berryCount++;
    this.currentDrops = 0;
  }

  // === 회원탈퇴 관련 메서드 ===

  /**
   * 회원탈퇴 처리 (Soft Delete)
   */
  public void markAsDeleted() {
    this.deletedAt = LocalDateTime.now();
  }

  /**
   * 활성 사용자 여부 확인
   */
  public boolean isActive() {
    return this.deletedAt == null;
  }
}
