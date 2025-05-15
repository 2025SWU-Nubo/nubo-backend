package com.nubo.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 생성일자(createdAt)와 수정일자(updatedAt)를 자동으로 관리하는 공통 엔티티 클래스입니다.
 * 모든 엔티티에 상속시켜 시간 정보 자동 설정에 사용됩니다.
 */
@MappedSuperclass
@Getter
@Setter
public class BaseTimeEntity {

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

}
