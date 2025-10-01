package com.nubo.domain.notification.dto;

import com.nubo.domain.notification.entity.Notification;
import com.nubo.domain.notification.type.NotificationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponseDto {

  private Long notificationId;
  private NotificationType type;
  private String title;
  private String body;
  private boolean read;
  private LocalDateTime createdAt;

  private Long boardId;
  private Long invitationId;

  public static NotificationResponseDto toNotificationResponseDto(Notification notification) {
    return NotificationResponseDto.builder()
      .notificationId(notification.getId())
      .type(notification.getType())
      .title(notification.getTitle())
      .body(notification.getBody())
      .read(notification.isRead())
      .createdAt(notification.getCreatedAt())
      .boardId(notification.getBoard() != null ? notification.getBoard().getId() : null)
      .invitationId(
        notification.getInvitation() != null ? notification.getInvitation().getId() : null)
      .build();
  }
}
