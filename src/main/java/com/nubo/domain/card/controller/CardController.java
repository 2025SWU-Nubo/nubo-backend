package com.nubo.domain.card.controller;

import com.nubo.domain.card.dto.CardRequestDto;
import com.nubo.domain.card.dto.CardResponseDto;
import com.nubo.domain.card.service.CardService;
import com.nubo.global.auth.UserUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

  private final CardService cardService;
  private final UserUtil userUtil;

  /**
   * 로그인한 사용자의 새 카드를 생성한다.
   *
   * @param dto 생성할 카드 정보 DTO
   * @return 생성된 카드 정보 DTO
   */
  @PostMapping
  public ResponseEntity<CardResponseDto> createCard(@RequestBody CardRequestDto dto) {
    Long userId = userUtil.getAuthenticatedUserId();
    CardResponseDto response = cardService.createCard(dto, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 로그인한 사용자의 카드 목록을 조회한다.
   *
   * @return 카드 응답 DTO 리스트
   */
  @GetMapping
  public ResponseEntity<List<CardResponseDto>> getMyCards() {
    Long userId = userUtil.getAuthenticatedUserId();
    List<CardResponseDto> response = cardService.getCardsByUser(userId);
    return ResponseEntity.ok(response);
  }

  /**
   * 특정 카드 ID에 대한 정보를 조회한다.
   *
   * @param cardId 조회할 카드 ID
   * @return 카드 응답 DTO
   */
  @GetMapping("/{cardId}")
  public ResponseEntity<CardResponseDto> getCard(@PathVariable Long cardId) {
    Long userId = userUtil.getAuthenticatedUserId();
    CardResponseDto response = cardService.getCardById(cardId, userId);
    return ResponseEntity.ok(response);
  }


}
