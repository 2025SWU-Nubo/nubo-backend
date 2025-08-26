package com.nubo.domain.card.entity;

import com.nubo.domain.board.entity.BoardCard;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.video.entity.Video;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 카드 작성자 (소유자)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // 원본 영상
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "video_id", nullable = false)
  private Video video;

  @Column(nullable = false)
  private String title; // 카드 제목 (초기값은 영상 제목, 이후 수정 가능)

  @Column(columnDefinition = "TEXT")
  private String summary; // AI 요약 텍스트

  @Column(columnDefinition = "TEXT")
  private String tags; // 태그 목록 (JSON 문자열 형태)

  @Column(nullable = false)
  private boolean isFavorite = false; // 즐겨찾기 여부

  /**
   * 보드와의 M:N 관계 → 중간 엔티티 BoardCard로 관리
   */
  @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<BoardCard> boardCards = new HashSet<>();

  /**
   * 소프트 삭제 관련 필드
   * - 삭제된 시각
   * - 삭제한 사용자 id
   */
  private Instant deletedAt;
  private Long deletedBy;

  public void updateMeta(String summary, List<String> tags) {
    this.summary = summary;
    this.tags = String.join(",", tags);
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }
}
