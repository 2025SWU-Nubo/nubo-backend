package com.nubo.global.common;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimeUtil {

  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  public static LocalDateTime toKst(LocalDateTime utcTime) {
    if (utcTime == null) {
      return null;
    }

    return utcTime
      .atZone(UTC)
      .withZoneSameInstant(KST)
      .toLocalDateTime();
  }
}
