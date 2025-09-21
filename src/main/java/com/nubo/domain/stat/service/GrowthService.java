package com.nubo.domain.stat.service;

import com.nubo.domain.stat.dto.DashboardResponseDto;
import com.nubo.domain.stat.dto.DateCountDto;
import com.nubo.domain.stat.dto.DropResultDto;
import com.nubo.domain.stat.entity.DailyWaterDrop;
import com.nubo.domain.stat.repository.DailyWaterDropRepository;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.repository.UserRepository;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 성장 로직 서비스
 * - 카드 열람 시 물방울 획득 처리
 * - 대시보드 데이터 제공
 */
@Service
@RequiredArgsConstructor
public class GrowthService {

  private final DailyWaterDropRepository dailyWaterDropRepository;
  private final UserRepository userRepository;

  /**
   * 카드 열람 시 물방울 1개 증가 처리
   *
   * @param userId 사용자 ID
   * @exception ApiException 사용자가 존재하지 않으면 예외 발생
   */
  @Transactional
  public DropResultDto addDrop(Long userId) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // drop 반영 전 단계
    int oldStage = Math.min(user.getCurrentDrops() / 5, 4);

    // 오늘 기록 조회
    LocalDate today = LocalDate.now();
    DailyWaterDrop record = dailyWaterDropRepository.findByUserIdAndDate(userId, today)
      .orElseGet(() -> DailyWaterDrop.builder()
        .user(user)
        .date(today)
        .count(0)
        .build()
      );

    // count 증가
    record.increaseCount();
    dailyWaterDropRepository.save(record);

    // currentDrops는 하루 5개까지만 반영
    if (record.getCount() <= 5) {
      user.addDrop();
    }

    // 25개 달성 → 열매 획득 처리
    boolean berryGained = false;
    if (user.getCurrentDrops() == 25) {
      user.gainBerry();
      berryGained = true;
    }

    userRepository.save(user);

    int newStage = Math.min(user.getCurrentDrops() / 5, 4);
    boolean stageUp = (newStage > oldStage);

    return new DropResultDto(newStage, berryGained, stageUp);
  }

  /**
   * 대시보드 데이터 조회
   *
   * @param userId 사용자 ID
   * @return DashboardResponseDto (주간 시청 영상 수, 오늘 시청 수, 성장 단계, 성장률, 누베리 개수)
   * @exception ApiException 사용자가 존재하지 않으면 예외 발생
   */
  @Transactional(readOnly = true)
  public DashboardResponseDto getDashboard(Long userId) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ApiException(ErrorCode.ENTITY_NOT_FOUND));

    // 이번 주 범위 계산 (일요일 ~ 토요일)
    LocalDate today = LocalDate.now();
    LocalDate startOfWeek = today.with(DayOfWeek.SUNDAY);
    LocalDate endOfWeek = startOfWeek.plusDays(6);

    List<DailyWaterDrop> weeklyRecords =
      dailyWaterDropRepository.findWeeklyRecords(userId, startOfWeek, endOfWeek);

    // 날짜별 count 맵핑 (빈 날짜는 0)
    Map<LocalDate, Integer> countMap = weeklyRecords.stream()
      .collect(Collectors.toMap(DailyWaterDrop::getDate, DailyWaterDrop::getCount));

    List<DateCountDto> weeklyVideoCounts = new ArrayList<>();
    for (LocalDate d = startOfWeek; !d.isAfter(endOfWeek); d = d.plusDays(1)) {
      weeklyVideoCounts.add(
        new DateCountDto(d, countMap.getOrDefault(d, 0))
      );
    }

    // 오늘 시청 영상 수
    int todayCount = countMap.getOrDefault(today, 0);

    // 단계 계산 (0~4)
    int stage = Math.min(user.getCurrentDrops() / 5, 4);

    // 성장률 (0~100 %)
    int growthRate = Math.min((user.getCurrentDrops() * 100) / 25, 100);

    return DashboardResponseDto.builder()
      .weeklyVideoCounts(weeklyVideoCounts)
      .todayVideoCount(todayCount)
      .stage(stage)
      .growthRate(growthRate)
      .berryCount(user.getBerryCount())
      .build();
  }
}
