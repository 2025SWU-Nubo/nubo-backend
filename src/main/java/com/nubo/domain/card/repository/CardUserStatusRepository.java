package com.nubo.domain.card.repository;

import com.nubo.domain.card.entity.CardUserStatus;
import com.nubo.domain.card.entity.CardUserStatus.CardUserStatusId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardUserStatusRepository extends JpaRepository<CardUserStatus, CardUserStatusId> {

  // 특정 사용자와 카드의 상태를 조회
  Optional<CardUserStatus> findByUserIdAndCardId(Long userId, Long cardId);

  // 특정 사용자와 카드의 상태 존재 여부 확인
  boolean existsByUserIdAndCardId(Long userId, Long cardId);
}
