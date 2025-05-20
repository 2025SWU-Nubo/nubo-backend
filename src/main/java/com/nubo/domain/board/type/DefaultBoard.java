package com.nubo.domain.board.type;

import java.util.Map;

public class DefaultBoard {

  private static final Map<String, Long> boardNameToIdMap = Map.of(
    "엔터테인먼트 & 코미디", 1L,
    "교육 & 정보 (테크·비즈니스 포함)", 2L,
    "뷰티 & 패션", 3L,
    "요리 & 라이프스타일", 4L,
    "운동 & 건강", 5L,
    "여행 & 브이로그", 6L,
    "게임 & 취미 (공예 포함)", 7L,
    "음악 & 예술", 8L,
    "TV & 미디어 콘텐츠", 9L,
    "기타", 10L
  );

  public static Long getBoardIdByName(String boardName) {
    return boardNameToIdMap.getOrDefault(boardName, 10L); // 기타로 fallback
  }
}
