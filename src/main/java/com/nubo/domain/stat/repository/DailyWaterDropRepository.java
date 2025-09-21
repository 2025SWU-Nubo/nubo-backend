package com.nubo.domain.stat.repository;

import com.nubo.domain.stat.entity.DailyWaterDrop;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyWaterDropRepository extends JpaRepository<DailyWaterDrop, Long> {

  /**
   * 특정 유저 + 날짜 기준으로 데이터 조회
   */
  Optional<DailyWaterDrop> findByUserIdAndDate(Long userId, LocalDate date);

  /**
   * 특정 유저의 최근 7일치 기록 조회 (대시보드용)
   */
  @Query("""
    SELECT d
    FROM DailyWaterDrop d
    WHERE d.user.id = :userId
      AND d.date BETWEEN :startDate AND :endDate
    ORDER BY d.date ASC
    """)
  List<DailyWaterDrop> findWeeklyRecords(
    @Param("userId") Long userId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
  );
}
