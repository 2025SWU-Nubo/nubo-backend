package com.nubo.domain.card.controller;

import com.nubo.domain.card.dto.CardRequestDto;
import com.nubo.domain.card.dto.CardResponseDto;
import com.nubo.domain.card.service.CardService;
import com.nubo.global.auth.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

}
