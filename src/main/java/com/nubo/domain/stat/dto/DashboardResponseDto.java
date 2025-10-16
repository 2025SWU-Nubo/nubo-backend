package com.nubo.domain.stat.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponseDto {

  // 주간 날짜별 시청 영상 수
  private List<DateCountDto> weeklyVideoCounts;

  // 오늘 시청 영상 수
  private int todayVideoCount;

  // 오늘 구름 밑 물방울 표시용 (최대 5)
  private int todayWaterDrops;

  // 성장 단계 (0=새싹, 1=키 큰 새싹, 2=꽃봉오리, 3=꽃, 4=열매)
  private int stage;

  // 성장률 (0~100 %)
  private int growthRate;

  // 누적 누베리 개수
  private int berryCount;

  // 배경 url
  private String dashboardBackground;
}
