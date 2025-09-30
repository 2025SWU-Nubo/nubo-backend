package com.nubo.domain.notification.service;

import com.nubo.domain.notification.entity.DeviceToken;
import com.nubo.domain.notification.repository.DeviceTokenRepository;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

  private final DeviceTokenRepository deviceTokenRepository;
  private final UserService userService;

  /**
   * FCM 토큰을 등록하거나 갱신한다.
   * - 이미 토큰이 존재하면 해당 토큰의 유저를 현재 유저로 갱신
   * - 존재하지 않으면 새로운 토큰을 생성하여 저장
   *
   * @param userId 토큰을 등록할 유저 ID
   * @param token  FCM 디바이스 토큰
   */
  @Transactional
  public void registerOrUpdateToken(Long userId, String token) {
    User user = userService.getUserById(userId);

    deviceTokenRepository.findByToken(token).ifPresentOrElse(
      existing -> {
        // 이미 있는 토큰이면 유저만 확인 (다른 유저라면 갱신)
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
   * 특정 유저의 토큰을 삭제한다.
   *
   * @param userId 토큰을 삭제할 유저 ID
   * @param token  삭제할 FCM 디바이스 토큰
   */
  @Transactional
  public void deleteToken(Long userId, String token) {
    deviceTokenRepository.deleteByUserIdAndToken(userId, token);
  }

  /**
   * 특정 유저의 모든 FCM 토큰을 조회한다.
   *
   * @param userId 조회할 유저 ID
   * @return 해당 유저의 디바이스 토큰 목록
   */
  @Transactional(readOnly = true)
  public List<DeviceToken> getTokensByUserId(Long userId) {
    return deviceTokenRepository.findByUserId(userId);
  }
}
