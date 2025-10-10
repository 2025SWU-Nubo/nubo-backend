package com.nubo.domain.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardDeleteResultDto {

  private Long cardId;
  private String status;

  public static CardDeleteResultDto deleted(Long id) {
    return new CardDeleteResultDto(id, "DELETED");
  }

  public static CardDeleteResultDto detached(Long id) {
    return new CardDeleteResultDto(id, "DETACHED");
  }

  public static CardDeleteResultDto forbidden(Long id) {
    return new CardDeleteResultDto(id, "FORBIDDEN");
  }

  public static CardDeleteResultDto notFound(Long id) {
    return new CardDeleteResultDto(id, "NOT_FOUND");
  }

  public static CardDeleteResultDto alreadyDeleted(Long id) {
    return new CardDeleteResultDto(id, "ALREADY_DELETED");
  }
}
