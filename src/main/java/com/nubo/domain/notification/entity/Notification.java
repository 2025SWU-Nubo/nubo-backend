package com.nubo.domain.notification.entity;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.board.entity.BoardInvitation;
import com.nubo.domain.notification.type.NotificationType;
import com.nubo.domain.user.entity.User;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "user_notification")
public class Notification extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NotificationType type;  // 알림 종류 (REMINDER, CARD_ADDED, BOARD)

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, length = 255)
  private String body;

  @Column(nullable = false)
  private boolean isRead = false; // 읽음 여부

  // -- 공유보드 알림 관련 연관관계 --
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_id")
  private Board board;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "invitation_id")
  private BoardInvitation invitation;

  public void markAsRead() {
    this.isRead = true;
  }
}
