package com.nubo.domain.stat.entity;

import com.nubo.domain.user.entity.User;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저별 일일 물방울 기록 엔티티
 * - 날짜(date) 단위로 물방울 개수를 기록한다.
 * - 하루에 여러 개를 모아도 row는 1개이며, count 값만 증가한다.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
  name = "daily_water_drop",
  uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "date"})
  }
)
public class DailyWaterDrop extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private LocalDate date;   // 해당 날짜 (YYYY-MM-DD)

  @Column(nullable = false)
  private int count;        // 그날 획득한 물방울 수 (5 이상도 저장)

  // 물방울 증가
  public void increaseCount() {
    this.count += 1;
  }
}
