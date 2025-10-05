package com.nubo.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardInvitation;
import com.nubo.domain.card.entity.Card;
import com.nubo.domain.notification.entity.DeviceToken;
import com.nubo.domain.notification.type.NotificationType;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

  private final DeviceTokenService deviceTokenService;
  private final UserService userService;
  private final NotificationService notificationService;

  /**
   * NotificationType별 Android channel ID 매핑
   */
  private String resolveChannelId(NotificationType type) {
    return switch (type) {
      case REMINDER -> "reminder_channel";
      case CARD_ADDED -> "card_channel";
      case BOARD -> "board_channel";
      default -> "default_channel";
    };
  }

  /**
   * 특정 유저의 모든 기기에 알림 발송 및 DB 저장
   *
   * @param userId 알림을 받을 유저 ID
   * @param type   알림 종류
   * @param title  알림 제목
   * @param body   알림 본문
   */
  public void sendNotificationToUser(
    Long userId,
    NotificationType type,
    String title,
    String body
  ) {
    // 1. 우선 DB 저장
    notificationService.createNotification(userId, type, title, body);

    // 2. 알림 설정 확인
    User user = userService.getUserById(userId);
    if (!user.isPushEnabled()) {
      return;
    }

    // 3. 알림 발송
    List<DeviceToken> tokens = deviceTokenService.getTokensByUserId(userId);

    if (tokens.isEmpty()) {
      log.warn("푸시 발송 대상 토큰이 없습니다. userId={}", userId);
      return;
    }

    String channelId = resolveChannelId(type);
    for (DeviceToken token : tokens) {
      sendHybridMessageToToken(token.getToken(), type, title, body, channelId);
    }
  }

  /**
   * 특정 유저의 모든 기기에 알림 발송 및 DB 저장 (공유보드 관련)
   *
   * @param userId     알림을 받을 유저 ID
   * @param type       알림 종류
   * @param title      알림 제목
   * @param body       알림 본문
   * @param board      공유보드 엔티티
   * @param invitation 공유보드 초대 엔티티
   */
  public void sendNotificationToUser(
    Long userId,
    NotificationType type,
    String title,
    String body,
    Board board,
    BoardInvitation invitation
  ) {
    // 1. 우선 DB 저장
    notificationService.createNotification(userId, type, title, body, board, invitation);

    // 2. 알림 설정 확인
    User user = userService.getUserById(userId);
    if (!user.isPushEnabled()) {
      return;
    }

    // 3. 알림 발송
    List<DeviceToken> tokens = deviceTokenService.getTokensByUserId(userId);

    if (tokens.isEmpty()) {
      log.warn("푸시 발송 대상 토큰이 없습니다. userId={}", userId);
      return;
    }

    String channelId = resolveChannelId(type);
    for (DeviceToken token : tokens) {
      sendHybridMessageToToken(token.getToken(), type, title, body, channelId);
    }
  }


  /**
   * 특정 유저의 모든 기기에 알림 발송 및 DB 저장 (카드 관련)
   *
   * @param userId 알림을 받을 유저 ID
   * @param type   알림 종류
   * @param title  알림 제목
   * @param body   알림 본문
   * @param card   카드 엔티티
   */
  public void sendNotificationToUser(
    Long userId,
    NotificationType type,
    String title,
    String body,
    Card card
  ) {
    // 1. 우선 DB 저장
    notificationService.createNotification(userId, type, title, body, card);

    // 2. 알림 설정 확인
    User user = userService.getUserById(userId);
    if (!user.isPushEnabled()) {
      return;
    }

    // 3. 알림 발송
    List<DeviceToken> tokens = deviceTokenService.getTokensByUserId(userId);

    if (tokens.isEmpty()) {
      log.warn("푸시 발송 대상 토큰이 없습니다. userId={}", userId);
      return;
    }

    String channelId = resolveChannelId(type);
    for (DeviceToken token : tokens) {
      sendHybridMessageToToken(token.getToken(), type, title, body, channelId);
    }
  }

  /**
   * 단일 토큰에 알림 발송
   *
   * @param token FCM 디바이스 토큰
   * @param title 알림 제목
   * @param body  알림 본문
   */
  public void sendHybridMessageToToken(
    String token, NotificationType type, String title, String body, String channelId) {
    Notification notification = Notification.builder()
      .setTitle(title)
      .setBody(body)
      .build();

    Message message = Message.builder()
      .setToken(token)
      .setNotification(notification)
      .putData("title", title)
      .putData("body", body)
      .putData("type", type.name())
      .putData("channel_id", channelId)
      .build();

    try {
      FirebaseMessaging.getInstance().send(message);
    } catch (FirebaseMessagingException e) {
      throw new ApiException(ErrorCode.PUSH_SEND_FAILED);
    }
  }


  /**
   * 정기 리마인더 알림 발송
   *
   * @param userId 알림을 받을 유저 ID
   */
  public void sendReminderNotification(Long userId) {
    User user = userService.getUserById(userId);

    if (!user.isRemindEnabled()) {
      return;
    }

    sendNotificationToUser(
      userId,
      NotificationType.REMINDER,
      "미시청 카드 리마인드",
      "아직 열어보지 않은 카드가 있어요. 잊기 전에 확인해보세요!"
    );
  }

  /**
   * 카드 생성 완료 알림 발송
   *
   * @param userId    카드 생성한 유저 ID
   * @param cardTitle 생성된 카드 제목
   * @param card      생성된 카드 엔티티
   */
  public void sendCardCreatedNotification(Long userId, String cardTitle, Card card) {
    sendNotificationToUser(
      userId,
      NotificationType.CARD_ADDED,
      "추가하기",
      cardTitle + " 카드가 성공적으로 생성되었어요.",
      card
    );
  }

  /**
   * 공유보드 초대 알림 발송
   *
   * @param inviteeId  초대받는 유저 ID
   * @param boardName  보드 이름
   * @param memberName 초대하는 멤버 이름
   */
  public void sendBoardInviteNotification(
    Long inviteeId, String boardName, String memberName,
    Board board, BoardInvitation invitation) {

    sendNotificationToUser(
      inviteeId,
      NotificationType.BOARD,
      "공유하기",
      memberName + " 님이 '" + boardName + "' 보드를 공유하고 싶어해요.",
      board,
      invitation
    );
  }

  /**
   * 공유보드 초대 수락 알림 발송
   *
   * @param ownerId    보드 소유자 ID
   * @param memberName 초대 수락한 멤버 이름
   */
  public void sendBoardAcceptNotification(
    Long ownerId, String memberName,
    Board board) {
    sendNotificationToUser(
      ownerId,
      NotificationType.BOARD,
      "공유하기",
      memberName + " 님이 회원님의 공유 보드 초대를 수락했습니다. 이제 함께 보드를 관리할 수 있어요!",
      board,
      null
    );
  }

  /**
   * 공유보드 초대 수락 후 알림 발송
   *
   * @param inviteeId 초대받은 유저 ID
   * @param boardName 보드 이름
   */
  public void sendBoardAddedNotification(
    Long inviteeId, String boardName,
    Board board) {
    sendNotificationToUser(
      inviteeId,
      NotificationType.BOARD,
      "공유하기",
      "'" + boardName + "' 공유 보드가 내 보드에 추가되었습니다. 지금 바로 보드를 확인해 보세요.",
      board,
      null
    );
  }
}
