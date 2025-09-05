package com.nubo.domain.card.service;

import com.nubo.domain.card.entity.Card;
import com.nubo.domain.card.entity.CardUserStatus;
import com.nubo.domain.card.repository.CardUserStatusRepository;
import com.nubo.domain.user.service.UserService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardUserStatusService {

  private final CardUserStatusRepository cardUserStatusRepository;
  private final UserService userService;

  /**
   * 지정된 카드에 대해 사용자의 열람 상태를 기록한다.
   * 카드 상세 조회 시 진입하면 자동으로 호출되어,
   * 해당 카드가 처음 열람된 경우 viewedAt 시각을 저장한다.
   *
   * @param userId 열람한 사용자 ID
   * @param card   열람된 카드 엔티티
   */
  @Transactional
  public void markAsViewed(Long userId, Card card) {
    CardUserStatus status = cardUserStatusRepository
      .findByUserIdAndCardId(userId, card.getId())
      .orElseGet(() -> CardUserStatus.builder()
        .user(userService.getUserById(userId))
        .card(card)
        .build());

    if (status.getViewedAt() == null) {
      status.setViewedAt(Instant.now());
      cardUserStatusRepository.save(status);
    }
  }
}
