package com.nubo.domain.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardDeleteResultDto {

  private Long cardId;
  private String status;
  private String action;

  public static CardDeleteResultDto notFound(Long id) {
    return CardDeleteResultDto.builder().cardId(id).status("NOT_FOUND").build();
  }

  public static CardDeleteResultDto forbidden(Long id) {
    return CardDeleteResultDto.builder().cardId(id).status("FORBIDDEN").build();
  }

  public static CardDeleteResultDto alreadyDeleted(Long id) {
    return CardDeleteResultDto.builder().cardId(id).status("ALREADY_DELETED").build();
  }

  public static CardDeleteResultDto deleted(Long id) {
    return CardDeleteResultDto.builder().cardId(id).status("OK").action("SOFT_DELETED").build();
  }
}
