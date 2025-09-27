package com.nubo.domain.notification.service;

import com.nubo.domain.notification.entity.DeviceToken;
import com.nubo.domain.notification.repository.DeviceTokenRepository;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

  private final DeviceTokenRepository deviceTokenRepository;
  private final UserService userService;

  /**
   * 토큰 등록 또는 갱신
   */
  @Transactional
  public void registerOrUpdateToken(Long userId, String token) {
    User user = userService.getUserById(userId);

    deviceTokenRepository.findByToken(token).ifPresentOrElse(
      existing -> {
        // 이미 있는 토큰이면 유저만 확인 (다른 유저라면 변경)
        if (!existing.getUser().getId().equals(userId)) {
          existing.setUser(user);
          deviceTokenRepository.save(existing);
        }
      },
      () -> {
        // 없으면 새로 저장
        DeviceToken newToken = DeviceToken.builder()
          .user(user)
          .token(token)
          .build();
        deviceTokenRepository.save(newToken);
      }
    );
  }

  /**
   * 토큰 삭제
   */
  @Transactional
  public void deleteToken(Long userId, String token) {
    deviceTokenRepository.deleteByUserIdAndToken(userId, token);
  }

  /**
   * 유저의 모든 토큰 조회
   */
  @Transactional
  public List<DeviceToken> getTokensByUserId(Long userId) {
    return deviceTokenRepository.findByUserId(userId);
  }
}
