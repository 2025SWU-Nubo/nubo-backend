package com.nubo.domain.notification.service;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardInvitation;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.notification.dto.NotificationResponseDto;
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
   * @return 저장된 알림
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
      .visible(true)
      .build();

    return notificationRepository.save(notification);
  }

  /**
   * 알림 생성 및 저장 (공유보드 관련)
   *
   * @param userId     알림을 받을 유저 ID
   * @param type       알림 종류
   * @param title      알림 제목
   * @param body       알림 본문
   * @param board      공유보드 엔티티
   * @param invitation 공유보드 초대 엔티티
   * @return 생성된 알림
   */
  @Transactional
  public Notification createNotification(
    Long userId,
    NotificationType type,
    String title,
    String body,
    Board board,
    BoardInvitation invitation
  ) {
    User user = userService.getUserById(userId);

    Notification notification = Notification.builder()
      .user(user)
      .type(type)
      .title(title)
      .body(body)
      .isRead(false)
      .visible(true)
      .board(board)
      .invitation(invitation)
      .build();

    return notificationRepository.save(notification);
  }

  /**
   * 알림 생성 및 저장 (카드 관련)
   *
   * @param userId 알림을 받을 유저 ID
   * @param type   알림 종류
   * @param title  알림 제목
   * @param body   알림 본문
   * @param card   카드 엔티티
   * @return 생성된 알림
   */
  @Transactional
  public Notification createNotification(
    Long userId,
    NotificationType type,
    String title,
    String body,
    Card card
  ) {
    User user = userService.getUserById(userId);

    Notification notification = Notification.builder()
      .user(user)
      .type(type)
      .title(title)
      .body(body)
      .isRead(false)
      .visible(true)
      .card(card)
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
  public List<NotificationResponseDto> getRecentNotifications(Long userId) {
    LocalDateTime since = LocalDateTime.now().minusDays(7);

    return notificationRepository.findRecentByUserId(userId, since).stream()
      .map(NotificationResponseDto::toNotificationResponseDto)
      .toList();
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

    notification.markAsRead();
  }

  /**
   * 특정 유저의 전체 알림 읽음 처리
   *
   * @param userId 요청한 유저 ID
   */
  @Transactional
  public void markAllAsRead(Long userId) {
    List<Notification> notifications = getRecentNotificationEntities(userId);
    notifications.forEach(n -> n.markAsRead());
  }

  // 전체 알림 읽음 처리를 위한 엔티티용 조회 메서드
  @Transactional(readOnly = true)
  public List<Notification> getRecentNotificationEntities(Long userId) {
    LocalDateTime since = LocalDateTime.now().minusDays(7);
    return notificationRepository.findRecentByUserId(userId, since);
  }

  // 초대 수락 후 알림 숨김 처리
  @Transactional
  public void hideByInvitationId(Long invitationId) {
    notificationRepository.findByInvitationId(invitationId)
      .ifPresent(notification -> {
        notification.setVisible(false);
        notificationRepository.save(notification);
      });
  }
}
