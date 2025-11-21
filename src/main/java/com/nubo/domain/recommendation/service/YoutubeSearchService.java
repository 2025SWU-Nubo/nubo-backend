package com.nubo.domain.recommendation.service;

import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.recommendation.dto.YoutubeVideoResult;
import com.nubo.global.ai.OpenAiClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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

  private static final String VIDEOS_URL =
    "https://www.googleapis.com/youtube/v3/videos";

  private final RestTemplate restTemplate;
  private final OpenAiClient openAiClient;


  @Value("${youtube.api-key}")
  private String apiKey;

  public List<YoutubeVideoResult> searchByKeyword(String keyword) {

    // 1) 파라미터 구성
    UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
      .queryParam("part", "snippet")
      .queryParam("q", keyword)
      .queryParam("maxResults", 5)
      .queryParam("type", "video")
      .queryParam("key", apiKey);

    // 2) 호출
    ResponseEntity<Map> response = restTemplate.getForEntity(
      uri.toUriString(), Map.class
    );

    // 3) 결과 파싱
    List<Map<String, Object>> items =
      (List<Map<String, Object>>) response.getBody().get("items");

    if (items == null) {
      return List.of();
    }

    return items.stream()
      .map(item -> {
        Map<String, Object> id = (Map<String, Object>) item.get("id");
        Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");

        String videoId = id.get("videoId").toString();
        String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

        return new YoutubeVideoResult(videoId, videoUrl);
      })
      .toList();
  }

  public List<YoutubeVideoResult> searchPopularVideos() {

    UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(VIDEOS_URL)
      .queryParam("part", "snippet,contentDetails")
      .queryParam("chart", "mostPopular")
      .queryParam("regionCode", "KR")
      .queryParam("maxResults", 100)
      .queryParam("key", apiKey);

    ResponseEntity<Map> response = restTemplate.getForEntity(
      uri.toUriString(), Map.class
    );

    List<Map<String, Object>> items =
      (List<Map<String, Object>>) response.getBody().get("items");

    if (items == null) {
      return List.of();
    }

    return items.stream()
      .map(item -> {

        // videoId
        String videoId = item.get("id").toString();

        // duration
        Map<String, Object> content = (Map<String, Object>) item.get("contentDetails");
        String duration = (content != null && content.get("duration") != null)
          ? content.get("duration").toString()
          : null;

        // Shorts만 필터링
        if (duration == null || !isShort(duration)) {
          return null;
        }

        // --- title ---
        Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
        String title = snippet != null && snippet.get("title") != null
          ? snippet.get("title").toString()
          : "";

        // 2) 유용성 판단
//        if (!isUsefulForLearning(title, null)) {
//          return null;
//        }

        String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

        return new YoutubeVideoResult(videoId, videoUrl);
      })
      .filter(Objects::nonNull)
      .toList();
  }

  public List<YoutubeVideoResult> searchPopularByCategory(DefaultBoard category) {
    refreshCategoryKeywords(category);

    // 1) 해당 카테고리의 키워드 목록
    List<String> keywords = CATEGORY_KEYWORDS.getOrDefault(category, List.of());

    if (keywords.isEmpty()) {
      return List.of();
    }

    String targetKeyword = keywords.get(0);

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

    for (Map<String, Object> item : items) {
      Map<String, Object> id = (Map<String, Object>) item.get("id");
      if (id == null || id.get("videoId") == null) {
        continue;
      }

      String videoId = id.get("videoId").toString();
      String videoUrl = "https://www.youtube.com/shorts/" + videoId;

      results.add(new YoutubeVideoResult(videoId, videoUrl));
    }

    // 중복 제거 + 상위 5개만 리턴
    return results.stream()
      .distinct()
      .toList();
  }


  private boolean isShort(String duration) {
    // duration: "PT45S", "PT1M05S" 등
    return parseDurationSeconds(duration) <= 121;
  }

  private int parseDurationSeconds(String isoDuration) {

    if (isoDuration == null || isoDuration.isBlank()) {
      return 0;
    }

    // ISO8601 Duration: PT#H#M#S
    // 예: PT1H3M55S, PT15S, PT2M
    int hours = 0;
    int minutes = 0;
    int seconds = 0;

    String temp = isoDuration;

    try {
      // PT 제거
      if (temp.startsWith("PT")) {
        temp = temp.substring(2);
      }

      // 시간
      if (temp.contains("H")) {
        String[] split = temp.split("H");
        hours = Integer.parseInt(split[0]);
        temp = split.length > 1 ? split[1] : "";
      }

      // 분
      if (temp.contains("M")) {
        String[] split = temp.split("M");
        minutes = Integer.parseInt(split[0]);
        temp = split.length > 1 ? split[1] : "";
      }

      // 초
      if (temp.contains("S")) {
        String[] split = temp.split("S");
        seconds = Integer.parseInt(split[0]);
      }

    } catch (Exception e) {
      // 혹시 이상한 형식이 들어오면 0초 처리
      return 0;
    }

    return hours * 3600 + minutes * 60 + seconds;
  }

  public void refreshCategoryKeywords(DefaultBoard category) {
    List<String> newKeywords = openAiClient.generateTrendingKeywords(category);
    YoutubeSearchService.CATEGORY_KEYWORDS.put(category, newKeywords);

    log.info("[카테고리 키워드 업데이트] {} = {}", category, newKeywords);
  }
}
