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

  // 필터링용 불용어
  private static final Set<String> KOREAN_STOPWORDS = Set.of(
    "은", "는", "이", "가",
    "을", "를",
    "의",
    "에", "에서", "에게",
    "도", "만", "까지", "부터",
    "와", "과", "랑", "하고",
    "으로", "로",
    "다", "하다", "했다"
  );
  private final CardService cardService;

  /**
   * 유저가 가진 카드의 title, summary, tags 기반으로 대표 키워드 추출
   */
  public List<String> extractTopKeywords(Long userId, int limit) {
    List<Card> cards = cardService.getAllCardsByUser(userId);

    Map<String, Integer> freq = new HashMap<>();

    for (Card card : cards) {

      // 1) 제목 단어
      if (card.getTitle() != null) {
        for (String word : splitWords(card.getTitle())) {
          freq.merge(word, 1, Integer::sum);
        }
      }

      // 2) 요약 단어
      if (card.getSummary() != null) {
        for (String word : splitWords(card.getSummary())) {
          freq.merge(word, 1, Integer::sum);
        }
      }

      // 3) 태그 목록
      if (card.getTags() != null) {
        for (String tag : parseTags(card.getTags())) {
          freq.merge(tag.toLowerCase(), 1, Integer::sum);
        }
      }
    }

    return freq.entrySet().stream()
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
      .filter(s -> s.length() > 1) // 자모/한 글자 제거
      .map(this::removeEndingStopword) // 단어 끝에 붙은 조사 제거
      .filter(s -> !s.isBlank())
      .filter(s -> !KOREAN_STOPWORDS.contains(s)) // 단독 불용어 제거
      .toList();
  }

  private String removeEndingStopword(String word) {
    // 단어가 너무 짧으면 그냥 반환
    if (word.length() <= 1) {
      return word;
    }

    // 불용어가 단어 끝에 있으면 제거
    for (String stop : KOREAN_STOPWORDS) {
      if (word.endsWith(stop) && word.length() > stop.length()) {
        return word.substring(0, word.length() - stop.length());
      }
    }

    return word;
  }
}
