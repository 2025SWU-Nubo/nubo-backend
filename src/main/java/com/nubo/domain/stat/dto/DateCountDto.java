package com.nubo.domain.stat.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DateCountDto {

  // 날짜 (YYYY-MM-DD)
  private LocalDate date;

  // 해당 날짜의 시청 영상 수
  private int count;
}
