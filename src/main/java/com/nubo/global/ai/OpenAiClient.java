package com.nubo.global.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.card.dto.AiCardMetaDto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

  private final RestTemplate restTemplate;

  @Value("${openai.api-key}")
  private String apiKey;

  public AiCardMetaDto generateCardMeta(String inputText) {
    String prompt = buildPrompt(inputText);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    Map<String, Object> requestBody = Map.of(
      "model", "gpt-4",
      "messages", List.of(
        Map.of("role", "system", "content",
          "You are a helpful assistant that summarizes and categorizes short-form videos."),
        Map.of("role", "user", "content", prompt)
      )
    );

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
    ResponseEntity<Map> response = restTemplate.postForEntity(
      "https://api.openai.com/v1/chat/completions",
      request,
      Map.class
    );

    try {
      Map<String, Object> choice = ((List<Map<String, Object>>) response.getBody()
        .get("choices")).get(0);
      Map<String, Object> message = (Map<String, Object>) choice.get("message");
      String content = message.get("content").toString();

      // GPT 응답 파싱
      ObjectMapper mapper = new ObjectMapper();
      JsonNode json = mapper.readTree(content);

      String summary = json.get("summary").asText();
      List<String> tags = mapper.convertValue(json.get("tags"), new TypeReference<List<String>>() {
      });
      String boardName = json.get("board").asText();

      Long boardId = DefaultBoard.getBoardIdByName(boardName);

      return AiCardMetaDto.builder()
        .summary(summary)
        .tags(tags)
        .boardId(boardId)
        .build();

    } catch (Exception e) {
      throw new RuntimeException("GPT 응답 파싱 중 오류 발생", e);
    }
  }

  private String buildPrompt(String inputText) {
    return String.format("""
      다음은 하나의 영상에서 추출된 정보입니다.

      이 내용을 바탕으로 다음 세 가지를 JSON 형식으로 만들어주세요:

      {
        "summary": "영상 내용을 한 문단으로 요약",
        "tags": ["키워드1", "키워드2", ..., "키워드6"],
        "board": "가장 적합한 보드명 (아래 중 하나)"
      }

      사용 가능한 보드 목록:
      - 엔터테인먼트 & 코미디
      - 교육 & 정보 (테크·비즈니스 포함)
      - 뷰티 & 패션
      - 요리 & 라이프스타일
      - 운동 & 건강
      - 여행 & 브이로그
      - 게임 & 취미 (공예 포함)
      - 음악 & 예술
      - TV & 미디어 콘텐츠
      - 기타

      📌 영상 내용:
      %s
      """, inputText);
  }
}
