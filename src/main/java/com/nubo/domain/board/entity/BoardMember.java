package com.nubo.domain.board.entity;

import com.nubo.domain.board.type.BoardMemberRole;
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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
  name = "board_member",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_board_member_board_user", columnNames = {"board_id", "user_id"})
  },
  indexes = {
    @Index(name = "idx_board_member_board", columnList = "board_id"),
    @Index(name = "idx_board_member_user", columnList = "user_id")
  }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BoardMember extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 다대다 중간 테이블: 보드 N : 멤버십 : N 유저
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BoardMemberRole role;

  @Column(name = "favorite", nullable = false)
  private boolean favorite = false;

  public void updateFavorite(boolean favorite) {
    this.favorite = favorite;
  }
}
