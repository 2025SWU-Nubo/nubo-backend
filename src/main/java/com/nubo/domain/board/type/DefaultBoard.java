package com.nubo.domain.board.type;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public enum DefaultBoard {

  EDUCATION("교육"),
  TECH("테크 & 프로그래밍"),
  BUSINESS("비즈니스 & 생산성"),
  BEAUTY("뷰티 & 패션"),
  LIFESTYLE("요리 & 라이프스타일"),
  HEALTH("운동 & 건강"),
  TRAVEL("여행 & 브이로그"),
  GAME("게임"),
  HOBBY("취미 & 공예"),
  MUSIC("음악"),
  ART("예술 & 디자인"),
  ENTERTAINMENT("엔터테인먼트(코미디/TV/쇼)"),
  ETC("기타");

  // 건너뛰기 시 기본으로 노출할 보드 지정
  private static final EnumSet<DefaultBoard> DEFAULT_SKIP_BOARDS =
    EnumSet.of(EDUCATION, TECH, BUSINESS, HEALTH, LIFESTYLE);
  private final String displayName;

  DefaultBoard(String displayName) {
    this.displayName = displayName;
  }

  public static List<DefaultBoard> getSkipDefaults() {
    return DEFAULT_SKIP_BOARDS.stream().collect(Collectors.toList());
  }

  public String getDisplayName() {
    return displayName;
  }
}
