package com.nubo.domain.board.type;

public enum DefaultBoard {

  ENTERTAINMENT("엔터테인먼트 & 코미디"),
  EDUCATION("교육 & 정보 (테크·비즈니스 포함)"),
  BEAUTY("뷰티 & 패션"),
  LIFESTYLE("요리 & 라이프스타일"),
  HEALTH("운동 & 건강"),
  TRAVEL("여행 & 브이로그"),
  HOBBY("게임 & 취미 (공예 포함)"),
  ART("음악 & 예술"),
  MEDIA("TV & 미디어 콘텐츠"),
  ETC("기타");

  private final String displayName;

  DefaultBoard(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
