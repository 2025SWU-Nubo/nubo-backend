package com.nubo.global.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nubo.domain.board.repository.BoardRepository;
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
  private final BoardRepository boardRepository;

  @Value("${openai.api-key}")
  private String apiKey;

  public AiCardMetaDto generateCardMeta(String inputText, Long userId) {
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

      Long boardId = boardRepository
        .findByUserIdAndName(userId, boardName)
        .orElseThrow(() -> new RuntimeException("해당 이름의 보드를 찾을 수 없습니다: " + boardName))
        .getId();

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
        "summary": "영상 내용을 마크다운 형식으로 정리해 주세요. 번호, 하이픈, 줄바꿈 등을 자유롭게 활용해 학습 노트처럼 정리합니다. '이 영상은 ~을 소개한다' 같은 표현은 피하고, 핵심 내용이나 팁, 개념을 직접적으로 기술해 주세요.",
        "tags": ["키워드1", "키워드2", ..., "키워드5"],
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

      🎯 요약 작성 시 참고 사항:
      선택된 보드에 어울리는 방식으로 요약해 주세요. 아래는 카테고리별 추천 요약 방식입니다:
        
      - 엔터테인먼트 & 코미디: 전개, 웃긴 포인트, 주요 흐름을 감상 포인트 중심으로 정리
      - 교육 & 정보 (테크·비즈니스 포함): 핵심 개념, 주장, 배울 점을 요점 중심으로 설명
      - 뷰티 & 패션: 추천 제품, 사용 팁, 스타일링 방법 등을 상황별로 정리
      - 요리 & 라이프스타일: 필요한 재료와 순서, 실전 팁 등을 단계별로 정리
      - 운동 & 건강: 루틴 구성, 실천 순서, 주의할 점 등을 실용적으로 정리
      - 여행 & 브이로그: 장소 소개, 추천 이유, 개인 팁 등을 간결하게 정리
      - 게임 & 취미 (공예 포함): 게임/취미의 규칙, 진행 방식, 핵심 포인트 설명
      - 음악 & 예술: 작품의 배경, 의도, 감상 포인트를 중심으로 정리
      - TV & 미디어 콘텐츠: 줄거리 요약 + 전달 메시지나 인상 깊은 장면 소개
      - 기타: 사용자가 실천하거나 이해에 도움 될 방식으로 자유롭게 요약

      📌 영상 내용:
      %s
      """, inputText);
  }
}
