package com.nubo.domain.card.repository;

import com.nubo.domain.card.entity.CardUserStatus;
import com.nubo.domain.card.entity.CardUserStatus.CardUserStatusId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CardUserStatusRepository extends JpaRepository<CardUserStatus, CardUserStatusId> {

  // 특정 사용자와 카드의 상태를 조회
  Optional<CardUserStatus> findByUserIdAndCardId(Long userId, Long cardId);

  // 지정된 카드 목록에 대한 사용자의 상태를 모두 조회
  @Query("""
      SELECT cus
      FROM CardUserStatus cus
      WHERE cus.user.id = :userId
        AND cus.card.id IN :cardIds
    """)
  List<CardUserStatus> findByUserIdAndCardIdIn(Long userId, List<Long> cardIds);
}
