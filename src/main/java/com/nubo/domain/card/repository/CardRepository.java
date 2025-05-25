package com.nubo.domain.card.repository;

import com.nubo.domain.card.entity.Card;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.video.entity.Video;
import io.micrometer.common.KeyValues;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

  // 해당 사용자의 모든 카드 목록을 조회
  List<Card> findAllByUser(User user);

  // 해당 사용자의 특정 카드 조회
  Optional<Card> findByIdAndUser(Long cardId, User user);

  // 중복 카드 방지용
  boolean existsByUserAndVideo(User user, Video video);

  // 보드에 소속된 카드 리스트 조회
  List<Card> findByBoardId(Long boardId);
}
