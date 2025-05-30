package com.nubo.domain.card.entity;

import com.nubo.domain.board.entity.Board;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.video.entity.Video;
import com.nubo.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 카드 작성자 (누가 이 카드를 만들었는지)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // 원본 영상 참조
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

  // 속한 보드 (카드는 반드시 1개의 보드에 속함)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  public void updateMeta(String summary, List<String> tags) {
    this.summary = summary;
    this.tags = String.join(",", tags);
  }
}
