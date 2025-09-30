package com.nubo.domain.notification.service;

import com.nubo.domain.notification.entity.Notification;
import com.nubo.domain.notification.repository.NotificationRepository;
import com.nubo.domain.notification.type.NotificationType;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserService userService;

  /**
   * 알림 생성 및 저장
   *
   * @param userId 알림을 받을 유저 ID
   * @param type   알림 종류
   * @param title  알림 제목
   * @param body   알림 본문
   * @return 저장된 알림 DTO
   */
  @Transactional
  public Notification createNotification(
    Long userId,
    NotificationType type,
    String title,
    String body) {
    User user = userService.getUserById(userId);

    Notification notification = Notification.builder()
      .user(user)
      .type(type)
      .title(title)
      .body(body)
      .isRead(false)
      .build();

    return notificationRepository.save(notification);
  }

  /**
   * 특정 유저의 최근 일주일 알림 조회
   *
   * @param userId 조회할 유저 ID
   * @return 알림 DTO 리스트
   */
  @Transactional(readOnly = true)
  public List<Notification> getRecentNotifications(Long userId) {
    LocalDateTime since = LocalDateTime.now().minusDays(7);
    return notificationRepository.findRecentByUserId(userId, since);
  }

  /**
   * 단건 알림 읽음 처리
   *
   * @param notificationId 알림 ID
   * @param userId         요청한 유저 ID
   */
  @Transactional
  public void markAsRead(Long notificationId, Long userId) {
    Notification notification = notificationRepository.findById(notificationId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    if (!notification.getUser().getId().equals(userId)) {
      throw new ApiException(ErrorCode.ACCESS_DENIED);
    }

    notification.setRead(true);
  }

  /**
   * 특정 유저의 전체 알림 읽음 처리
   *
   * @param userId 요청한 유저 ID
   */
  @Transactional
  public void markAllAsRead(Long userId) {
    List<Notification> notifications = getRecentNotifications(userId);
    notifications.forEach(n -> n.setRead(true));
  }
}
