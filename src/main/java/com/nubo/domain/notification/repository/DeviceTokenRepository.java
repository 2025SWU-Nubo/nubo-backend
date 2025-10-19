package com.nubo.domain.notification.repository;

import com.nubo.domain.notification.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

  // 특정 토큰 값으로 조회
  Optional<DeviceToken> findByToken(String token);

  // 특정 유저의 모든 토큰 조회
  List<DeviceToken> findByUserId(Long userId);

  // 특정 유저의 토큰 삭제
  void deleteByUserIdAndToken(Long userId, String token);

  // 특정 토큰 삭제
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM DeviceToken d WHERE d.token = :token")
  void deleteByToken(@Param("token") String token);
}
