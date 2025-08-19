package com.nubo.domain.board.type;

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

  private final String displayName;

  DefaultBoard(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
