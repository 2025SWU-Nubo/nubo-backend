package com.nubo.domain.board.entity;

import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
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
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
public class Board extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  // BOARD(1차 분류), SECTION(2차 분류)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BoardType boardType;

  // AI 또는 USER
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BoardSource source;

  // 공유 여부 (USER 보드만 true 가능)
  @Column(nullable = false)
  private boolean isShared;

  // 즐겨찾기 여부
  @Column(nullable = false)
  private boolean isFavorite = false;

  // 모든 보드는 사용자 소유 (기본 제공 보드도 유저별로 복제됨)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  // SECTION인 경우에만 상위 보드 존재
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_board_id")
  private Board parentBoard;

  // 연관관계 역방향 (하위 섹션 리스트)
  @OneToMany(mappedBy = "parentBoard")
  private List<Board> sections = new ArrayList<>();

  // 마지막 수정시간 업데이트를 위한 dummy 변경
  public void touch() {
    this.setUpdatedAtForTouch(LocalDateTime.now());
  }
}
