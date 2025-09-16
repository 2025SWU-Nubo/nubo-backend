package com.nubo.domain.card.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardSummaryUpdateRequestDto {

  @NotBlank
  private String summary;
}
