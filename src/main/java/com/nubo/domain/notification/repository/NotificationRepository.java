package com.nubo.domain.notification.repository;

import com.nubo.domain.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  /**
   * 특정 유저의 최근 일주일 알림 조회 (최신순)
   */
  @Query("""
        SELECT n
        FROM Notification n
        WHERE n.user.id = :userId
          AND n.visible = true
          AND n.createdAt >= :since
        ORDER BY n.createdAt DESC
    """)
  List<Notification> findRecentByUserId(Long userId, LocalDateTime since);

  Optional<Notification> findByInvitationId(Long invitationId);
}
