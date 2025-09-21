package com.nubo.domain.stat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 물방울 증가 결과 DTO
 * - stage: 현재 단계 (0~4)
 * - berryGained: 누베리 획득 여부
 * - stageUp: 이번 열람으로 단계 달성 여부
 */
@Getter
@AllArgsConstructor
public class DropResultDto {

  private final int stage;        // 현재 단계
  private final boolean berryGained; // 열매 획득 여부
  private final boolean stageUp;     // 단계 달성 여부
}