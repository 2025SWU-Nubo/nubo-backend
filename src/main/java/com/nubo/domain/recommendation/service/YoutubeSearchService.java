package com.nubo.domain.recommendation.service;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.recommendation.dto.YoutubeSearchBundle;
import com.nubo.domain.recommendation.dto.YoutubeVideoResult;
import com.nubo.global.ai.OpenAiClient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeSearchService {

  public static final Map<DefaultBoard, List<String>> CATEGORY_KEYWORDS =
    new ConcurrentHashMap<>();

  private static final String SEARCH_URL =
    "https://www.googleapis.com/youtube/v3/search";

  private final RestTemplate restTemplate;
  private final OpenAiClient openAiClient;

  @Value("${youtube.api-key}")
  private String apiKey;

  public YoutubeSearchBundle searchByKeyword(String keyword) {
    UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
      .queryParam("part", "snippet")
      .queryParam("type", "video")
      .queryParam("videoDuration", "short") // Shorts만
      .queryParam("order", "viewCount")     // 인기순
      .queryParam("regionCode", "KR")
      .queryParam("maxResults", 30)
      .queryParam("q", keyword)
      .queryParam("key", apiKey);

    ResponseEntity<Map> response =
      restTemplate.getForEntity(uri.build().toUri(), Map.class);

    List<Map<String, Object>> items =
      (List<Map<String, Object>>) response.getBody().get("items");
    List<YoutubeVideoResult> results = new ArrayList<>();
    Set<String> seen = new HashSet<>();

    for (Map<String, Object> item : items) {
      Map<String, Object> id = (Map<String, Object>) item.get("id");
      if (id == null || id.get("videoId") == null) {
        continue;
      }

      String videoId = id.get("videoId").toString();
      if (!seen.add(videoId)) {
        continue;  // 중복 제거
      }

      String videoUrl = "https://www.youtube.com/shorts/" + videoId;
      results.add(new YoutubeVideoResult(videoId, videoUrl));
    }

    return new YoutubeSearchBundle(keyword, results);
  }

  public YoutubeSearchBundle searchByCategory(DefaultBoard category) {
    refreshCategoryKeywords(category);

    // 1) 해당 카테고리의 키워드 목록
    List<String> keywords = CATEGORY_KEYWORDS.getOrDefault(category, List.of());

    if (keywords.isEmpty()) {
      return new YoutubeSearchBundle(
        null,            // 검색 키워드 없음
        List.of()        // 결과 없음
      );
    }

    String targetKeyword = keywords.get(ThreadLocalRandom.current().nextInt(keywords.size()));

    UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
      .queryParam("part", "snippet")
      .queryParam("type", "video")
      .queryParam("videoDuration", "short") // Shorts만
      .queryParam("order", "viewCount")     // 인기순
      .queryParam("regionCode", "KR")
      .queryParam("maxResults", 30)
      .queryParam("q", targetKeyword)
      .queryParam("key", apiKey);

    ResponseEntity<Map> response =
      restTemplate.getForEntity(uri.build().toUri(), Map.class);

    List<Map<String, Object>> items =
      (List<Map<String, Object>>) response.getBody().get("items");
    List<YoutubeVideoResult> results = new ArrayList<>();
    Set<String> seen = new HashSet<>();

    for (Map<String, Object> item : items) {
      Map<String, Object> id = (Map<String, Object>) item.get("id");
      if (id == null || id.get("videoId") == null) {
        continue;
      }

      String videoId = id.get("videoId").toString();
      if (!seen.add(videoId)) {
        continue;  // 중복 제거
      }

      String videoUrl = "https://www.youtube.com/shorts/" + videoId;
      results.add(new YoutubeVideoResult(videoId, videoUrl));
    }

    return new YoutubeSearchBundle(targetKeyword, results);
  }

  // 카테고리별 키워드 업데이트
  public void refreshCategoryKeywords(DefaultBoard category) {
    List<String> newKeywords = openAiClient.generateTrendingKeywords(category);
    YoutubeSearchService.CATEGORY_KEYWORDS.put(category, newKeywords);

    log.info("[카테고리 키워드 업데이트] {} = {}", category, newKeywords);
  }
}
