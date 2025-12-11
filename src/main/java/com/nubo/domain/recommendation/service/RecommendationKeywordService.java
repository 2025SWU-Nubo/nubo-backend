package com.nubo.domain.recommendation.service;

import com.nubo.domain.card.entity.Card;
import com.nubo.domain.card.service.CardService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationKeywordService {

  // 검색 가치가 낮은 불용어 및 조사/어미
  private static final Set<String> KOREAN_STOPWORDS = Set.of(
    // 1. 조사/어미/접미사 (문법적 요소 및 끝처리에서 남을 수 있는 것)
    "은", "는", "이", "가", "을", "를", "의", "에", "에서", "에게", "께",
    "도", "만", "까지", "부터", "와", "과", "랑", "하고", "으로", "로",
    "다", "하다", "했다", "하는", "되는", "된", "할", "함", "있습니다",
    "입니다", "이다", "이에요", "있다", "없다", "않다",

    // 2. 검색 가치가 낮은 일반 명사/동사 (콘텐츠를 꾸미는 말)
    "추천", "영상", "콘텐츠", "방법", "정보", "이야기", "시간", "소개",
    "정리", "기록", "모음", "리뷰", "사용", "시청", "해보기", "만들기",
    "배우기", "알아보기", "카드", "꿀팁", "팁", "노하우", "업데이트", "공개",
    "대박", "꿀", "꼭", "이것", "저것", "나의", "내돈내산", "필수", "하는법",
    "시리즈", "이유", "부분", "가지", "제목", "키워드",
    "해보자", "보자", "알려", "드립니다", "들입니다", "해봤습니다",
    "시작", "끝", "아닙니다", "아닌", "만들어", "필요한", "알고", "싶은", "모든",
    "이런", "그런", "무엇", "어떤", "지금", "바로", "함께", "같이", "우리", "저희",
    "나타났다", "됩니다", "할수", "했습니다", "나왔다", "나왔습니다",
    "경우", "문제", "사항", "내용", "참고", "확인", "관련", "공유", "수준", "가능", "불가",

    // 3. 시간/빈도/수량 관련
    "매일", "항상", "잠시", "최신", "오늘", "내일", "요즘", "자주", "벌써",
    "다시", "또", "다음에", "먼저", "마지막", "전부", "모두", "하나", "둘", "셋",
    "번째", "개", "번", "만큼", "회", "분", "초",

    // 4. 감탄/형용/부사 관련
    "진짜", "완전", "최고", "절대", "엄청", "너무", "많이", "쉽게", "간단하게",
    "좋은", "쉬운", "힘든", "새로운", "특별한", "다양한", "신기한", "가장",
    "굉장히", "정말", "매우", "솔직히", "솔직한"
  );

  // 조사/어미만 모아둔 목록 (단어 끝부분 제거용)
  private static final Set<String> ENDING_STOPWORDS = Set.of(
    "은", "는", "이", "가", "을", "를", "의", "에", "에서", "으로", "로", "다", "하고"
  );

  private final CardService cardService;

  /**
   * 유저가 가진 카드의 title, summary, tags 기반으로 대표 키워드 추출
   */
  public List<String> extractTopKeywords(Long userId, int limit) {
    List<Card> cards = cardService.getAllCardsByUser(userId);

    Map<String, Integer> freq = new HashMap<>();

    for (Card card : cards) {
      // 1) 제목 단어 (가중치 3)
      if (card.getTitle() != null) {
        for (String word : splitWords(card.getTitle())) {
          freq.merge(word, 3, Integer::sum);
        }
      }
      // 2) 요약 단어 (가중치 1)
      if (card.getSummary() != null) {
        for (String word : splitWords(card.getSummary())) {
          freq.merge(word, 1, Integer::sum);
        }
      }
      // 3) 태그 목록 (가중치 5)
      if (card.getTags() != null) {
        for (String tag : parseTags(card.getTags())) {
          // 태그도 splitWords 필터링을 거치지 않아도 되지만, 길이 제한 적용을 위해 splitWords 재사용
          for (String word : splitWords(tag)) {
            freq.merge(word.toLowerCase(), 5, Integer::sum);
          }
        }
      }
    }

    // 최종 불용어 필터링과 정렬, 제한
    return freq.entrySet().stream()
      .filter(entry -> {
        String key = entry.getKey();
        // 한글은 3글자 이상, 영문은 4글자 이상만 허용 (너무 짧은 모호한 키워드 제거)
        if (key.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*")) {
          return key.length() >= 3;
        } else {
          return key.length() >= 4;
        }
      })
      .filter(entry -> !KOREAN_STOPWORDS.contains(entry.getKey()))
      .sorted((a, b) -> b.getValue() - a.getValue())
      .limit(limit)
      .map(Map.Entry::getKey)
      .toList();
  }

  private List<String> parseTags(String tags) {
    return Arrays.stream(tags.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .toList();
  }

  private List<String> splitWords(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }

    return Arrays.stream(
        text
          // 한글/영문/숫자 제외 문자 제거 → 공백 처리
          .replaceAll("[^가-힣a-zA-Z0-9\\s]", " ")
          .replaceAll("\\s+", " ")
          .trim()
          .split(" ")
      )
      .map(String::trim)
      .map(this::removeEndingStopword) // 단어 끝에 붙은 조사 제거
      .filter(s -> !s.isBlank())
      .filter(s -> !KOREAN_STOPWORDS.contains(s)) // 단독 불용어 제거
      .toList();
  }

  private String removeEndingStopword(String word) {
    // 단어가 너무 짧으면 그냥 반환
    if (word.length() <= 2) {
      return word;
    }

    // 불용어가 단어 끝에 있으면 제거
    for (String stop : ENDING_STOPWORDS) {
      if (word.endsWith(stop) && word.length() > stop.length()) {
        return word.substring(0, word.length() - stop.length());
      }
    }

    return word;
  }
}
