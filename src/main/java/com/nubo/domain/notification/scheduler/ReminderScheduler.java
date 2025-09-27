package com.nubo.domain.notification.scheduler;

import com.nubo.domain.notification.service.FcmService;
import com.nubo.domain.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

  private final FcmService fcmService;
  private final UserService userService;

  /**
   * 매일 오후 8시에 미시청 카드 리마인드 알림 발송
   */
  @Scheduled(cron = "0 0 20 * * *")
  public void sendDailyReminders() {
    List<Long> userIds = userService.getUserIdsForReminder();

    for (Long userId : userIds) {
      try {
        fcmService.sendReminderNotification(userId);
      } catch (Exception e) {
        log.error("Failed to send reminder to userId={}", userId, e);
      }
    }
  }

}