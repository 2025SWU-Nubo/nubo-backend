package com.nubo.domain.notification.controller;

import com.nubo.domain.notification.dto.NotificationResponseDto;
import com.nubo.domain.notification.service.NotificationService;
import com.nubo.global.auth.UserUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;
  private final UserUtil userUtil;

  /**
   * 최근 일주일 알림 목록 조회
   *
   * @return 알림 DTO 리스트
   */
  @GetMapping
  public ResponseEntity<List<NotificationResponseDto>> getNotifications() {
    Long userId = userUtil.getAuthenticatedUserId();
    List<NotificationResponseDto> notifications = notificationService.getRecentNotifications(
      userId);
    return ResponseEntity.ok(notifications);
  }

  /**
   * 단건 알림 읽음 처리
   *
   * @param id 알림 ID
   * @return 200 OK
   */
  @PatchMapping("/{id}/read")
  public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
    Long userId = userUtil.getAuthenticatedUserId();
    notificationService.markAsRead(id, userId);
    return ResponseEntity.ok().build();
  }

  /**
   * 전체 알림 읽음 처리
   *
   * @return 200 OK
   */
  @PatchMapping("/read-all")
  public ResponseEntity<Void> markAllAsRead() {
    Long userId = userUtil.getAuthenticatedUserId();
    notificationService.markAllAsRead(userId);
    return ResponseEntity.ok().build();
  }
}
