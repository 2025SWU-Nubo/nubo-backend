package com.nubo.domain.stat.controller;

import com.nubo.domain.stat.dto.DashboardResponseDto;
import com.nubo.domain.stat.service.GrowthService;
import com.nubo.global.auth.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 성장/통계 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

  private final GrowthService growthService;
  private final UserUtil userUtil;

  /**
   * 대시보드 데이터 조회
   *
   * @return DashboardResponseDto (주간 시청 영상 수, 오늘 시청 수, 성장 단계, 성장률, 누베리 개수)
   */
  @GetMapping("/dashboard")
  public ResponseEntity<DashboardResponseDto> getDashboard() {
    Long userId = userUtil.getAuthenticatedUserId();
    DashboardResponseDto dashboard = growthService.getDashboard(userId);
    return ResponseEntity.ok(dashboard);
  }
}
