package com.nubo.domain.board.entity;

import com.nubo.domain.board.type.BoardSource;
import com.nubo.domain.board.type.BoardType;
import com.nubo.domain.user.entity.User;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
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
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
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

  // 모든 보드는 사용자 소유 (기본 제공 보드도 유저별로 복제됨)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  // SECTION인 경우에만 상위 보드 존재
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_board_id")
  private Board parentBoard;

  @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<BoardCard> boardCards = new HashSet<>();

  @OneToMany(mappedBy = "parentBoard", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Board> sections = new HashSet<>();

  // 마지막 수정시간 업데이트를 위한 dummy 변경
  public void touch() {
    this.setUpdatedAtForTouch(LocalDateTime.now());
  }

  // === 연관관계 편의 메서드 ===
  public void addSection(Board section) {
    this.sections.add(section);
    section.setParentBoard(this);
  }

  public void removeSection(Board section) {
    this.sections.remove(section);
    section.setParentBoard(null);
  }
}
