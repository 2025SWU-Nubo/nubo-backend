package com.nubo.domain.card.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardSummaryPromptRequestDto {

  @NotBlank
  private String prompt;
}
