package com.nubo.domain.home.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UnviewedCardsRequestDto {

  private List<Long> boardIds;
  private Integer limit;
}
