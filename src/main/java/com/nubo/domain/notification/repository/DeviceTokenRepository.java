package com.nubo.domain.notification.repository;

import com.nubo.domain.notification.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

  // 특정 토큰 값으로 조회
  Optional<DeviceToken> findByToken(String token);

  // 특정 유저의 모든 토큰 조회
  List<DeviceToken> findByUserId(Long userId);

  // 특정 유저의 토큰 삭제
  void deleteByUserIdAndToken(Long userId, String token);
}
