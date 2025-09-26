package com.nubo.domain.card.service;

import com.nubo.domain.card.entity.Card;
import com.nubo.domain.card.entity.CardUserStatus;
import com.nubo.domain.card.repository.CardRepository;
import com.nubo.domain.card.repository.CardUserStatusRepository;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardUserStatusService {

  private final CardUserStatusRepository cardUserStatusRepository;
  private final CardRepository cardRepository;
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
  public boolean markAsViewed(Long userId, Card card) {
    CardUserStatus status = cardUserStatusRepository
      .findByUserIdAndCardId(userId, card.getId())
      .orElse(null);

    if (status == null) {
      // 완전 처음 보는 카드 → INSERT
      status = CardUserStatus.builder()
        .user(userService.getUserById(userId))
        .card(card)
        .viewedAt(Instant.now())
        .build();
      cardUserStatusRepository.save(status);
      return true;
    }

    if (status.getViewedAt() == null) {
      // 레코드는 있는데 viewedAt이 비어있을 때 → UPDATE
      status.setViewedAt(Instant.now());
      return true;
    }

    return false; // 이미 본 카드
  }

  /**
   * 특정 카드에 대해 사용자의 즐겨찾기 상태를 업데이트한다.
   *
   * @param userId   현재 사용자 ID
   * @param cardId   대상 카드 ID
   * @param favorite true → 즐겨찾기 추가, false → 즐겨찾기 해제
   * @return 최종 반영된 즐겨찾기 상태
   */
  @Transactional
  public boolean updateFavorite(Long userId, Long cardId, boolean favorite) {
    // 기존 상태 조회 (없으면 새로 생성)
    CardUserStatus status = cardUserStatusRepository
      .findByUserIdAndCardId(userId, cardId)
      .orElseGet(() -> CardUserStatus.builder()
        .user(userService.getUserById(userId))
        .card(cardRepository.findById(cardId)
          .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND)))
        .build());

    // 즐겨찾기 상태 변경
    status.setIsFavorite(favorite);

    // 저장
    cardUserStatusRepository.save(status);

    // 최종 반영된 값 리턴
    return status.getIsFavorite();
  }

  /**
   * 지정된 카드 목록에 대한 사용자의 상태를 모두 조회한다.
   *
   * @param userId  사용자 ID
   * @param cardIds 카드 ID 리스트
   * @return cardId → CardUserStatus 매핑
   */
  @Transactional(readOnly = true)
  public Map<Long, CardUserStatus> getStatusMap(Long userId, List<Long> cardIds) {
    return cardUserStatusRepository.findByUserIdAndCardIdIn(userId, cardIds)
      .stream()
      .collect(Collectors.toMap(cus -> cus.getCard().getId(), cus -> cus));
  }
}
