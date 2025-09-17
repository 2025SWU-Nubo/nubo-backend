package com.nubo.domain.interest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class InterestSetupResponseDto {

  private boolean completed; // 관심사 설정 완료 여부
  private int selectedCount; // 선택된 보드 개수
}
