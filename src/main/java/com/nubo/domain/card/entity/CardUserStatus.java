package com.nubo.domain.card.entity;

import com.nubo.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(CardUserStatus.CardUserStatusId.class)
public class CardUserStatus {

  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "card_id", nullable = false)
  private Card card;

  private Boolean isFavorite = false;  // 즐겨찾기 여부
  private LocalDateTime viewedAt;      // 열람 시각 (null = 미열람)

  @Getter
  @Setter
  @NoArgsConstructor
  @EqualsAndHashCode
  public static class CardUserStatusId implements Serializable {

    private Long user;
    private Long card;
  }
}
