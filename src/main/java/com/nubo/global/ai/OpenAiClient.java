package com.nubo.global.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nubo.domain.board.repository.BoardRepository;
import com.nubo.domain.card.dto.AiCardMetaDto;
import com.nubo.domain.card.dto.CardSummaryUpdateRequestDto.HighlightRange;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

  private final RestTemplate restTemplate;
  private final BoardRepository boardRepository;

  @Value("${openai.api-key}")
  private String apiKey;

  /**
   * 카드와 원본 영상 데이터를 기반으로 summary를 생성한다.
   *
   * @param inputText 가공할 원본 메타데이터의 합본 텍스트
   * @param userId    사용자 id
   * @return 생성된 메타데이터 dto
   */
  public AiCardMetaDto generateCardMeta(String inputText, Long userId) {
    String prompt = buildPrompt(inputText);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    Map<String, Object> requestBody = Map.of(
      "model", "gpt-4o",
      "response_format", Map.of("type", "json_object"),
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

      log.info("GPT JSON Extracted: {}", content);

      // GPT 응답 파싱
      ObjectMapper mapper = new ObjectMapper();
      JsonNode json = mapper.readTree(content);

      String title = json.has("title") ? json.get("title").asText("") : "";
      String summary = json.has("summary") ? json.get("summary").asText("") : "";
      List<String> tags = mapper.convertValue(json.get("tags"), new TypeReference<List<String>>() {
      });
      String boardName = json.has("board") ? json.get("board").asText("") : "";

      // 불충분한 콘텐츠 감지
      boolean insufficient = (inputText == null || inputText.strip().length() < 30)
        || summary.isBlank()
        || boardName.isBlank()
        || (tags == null || tags.isEmpty());

      if (insufficient) {
        log.info("불충분 콘텐츠 감지됨 → fallback 메타 적용");
        summary = "이 영상은 자동 요약이 어려워요. 필요한 내용을 직접 메모로 추가해 주세요.";

        // 태그 생략
        tags = List.of();

        // 보드 매핑은 '기타'
        boardName = "기타";
      }

      final String resolvedBoardName = boardName;

      Long boardId = boardRepository
        .findByUserIdAndName(userId, resolvedBoardName)
        .orElseThrow(() -> new RuntimeException("해당 이름의 보드를 찾을 수 없습니다: " + resolvedBoardName))
        .getId();

      return AiCardMetaDto.builder()
        .title(title)
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
      당신은 영상 학습 노트를 작성하는 보조자이다.  
      주어진 원본 제목(title), 소개글(description), 음성 텍스트(transcript), 자막(subtitle)을 분석하여  
      학습자가 한눈에 보기 쉬운 "재열람용 요약 카드" JSON을 생성한다.

      [입력/근거]
      - summary, tags, board 는 오직 description, transcript, subtitle 에 등장하는 내용만을 근거로 작성한다.
      - 원본 제목(title)은 제목 결정 시에만 참고하며, summary/tags/board에는 절대 사용하지 않는다.
      - 새로운 개념, 맥락, 추측, 과장은 금지한다.

      [출력 형식]
      순수 JSON만 출력한다. (코드펜스, 추가 텍스트 금지)
      값이 없으면 title="", summary="", tags=[], board="" 로 반환한다.

      출력 스키마:
      {
        "title": "string",
        "summary": "string (Markdown 허용: ###, -, 1., 표 |A|B|)",
        "tags": ["string", ...],
        "board": "string"
      }

      [규칙]

      1. title
      - 다음 조건 중 하나라도 해당하면 원본 제목은 폐기하고 description/transcript/subtitle 기반으로 12~32자의 한국어 제목을 새로 생성한다:
        (1) 원본 제목과 description+transcript+subtitle 사이의 의미적 관련성이 매우 낮은 경우
        (2) 원본 제목이 플레이스홀더/계정/파일명/URL/해시태그 위주인 경우 (예: "Video by …", "Original audio", "IMG_1234")
        (3) 원본 제목이 비어 있거나, "제목 없음", "Untitled", "No title", "(제목 없음)" 등 의미 없는 문자열인 경우
      - 새 제목은 반드시 입력 텍스트에 등장한 내용만 바탕으로 한다. (이모지/해시태그/과장/추측/광고 금지)
      - title이 완전히 비어 있는 경우에도 “(제목 없음)” 같은 플레이스홀더를 사용하지 말고, 입력 텍스트를 바탕으로 가능한 한 간결한 제목을 만든다. \s
      - description/transcript/subtitle이 모두 비어 있을 경우에만 title=""

      2. summary
      - Markdown을 활용해 가독성 있게 요약한다. (h2(##), h3(###), 굵게(**), 불릿(-), 숫자목록(1. 2. …)까지만 사용한다.)
      - 길이는 유연하다. 정보가 적으면 짧게, 많으면 길게.
      - 광고, 홍보, 과장, 클릭 유도 금지. 사실 서술 위주.
      - 학습 노트 스타일의 문장을 명사형 종결 어미로 작성한다.

      3. tags
      - 최소 3개, 최대 5개.
      - 1~2 단어의 핵심 키워드.
      - 중복, 의미 없음, 이모지, 해시태그 금지.

      4. board
      - 아래 보드 목록 중 정확히 하나 선택한다. (철자와 띄어쓰기까지 동일해야 한다.)
      - 선택된 보드 외의 값은 절대 반환하지 않는다.

      보드 목록:
      - "교육"
      - "테크 & 프로그래밍"
      - "비즈니스 & 생산성"
      - "뷰티 & 패션"
      - "요리 & 라이프스타일"
      - "운동 & 건강"
      - "여행 & 브이로그"
      - "게임"
      - "취미 & 공예"
      - "음악"
      - "예술 & 디자인"
      - "엔터테인먼트"
            
      *원본 텍스트:
      %s
      """, inputText);
  }

  /**
   * 카드와 원본 영상 데이터를 기반으로 summary를 재가공한다.
   *
   * @param cardTitle      카드 제목
   * @param currentSummary 현재 카드 summary
   * @param description    영상 설명
   * @param transcript     영상 음성 텍스트
   * @param subtitle       영상 자막
   * @param prompt         사용자 프롬프트 (예: "더 자세하게 요약해줘")
   * @return
   */
  public Map<String, Object> regenerateSummary(
    String cardTitle,
    String currentSummary,
    String description,
    String transcript,
    String subtitle,
    String prompt
  ) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    String userPrompt = String.format("""
        당신은 학습 노트 요약 보조자다.
        사용자의 요청: %s

        [카드 데이터]
        - 카드 제목: %s
        - 현재 요약: %s

        [원본 영상 메타데이터]
        - 설명: %s
        - 자막: %s
        - 음성 텍스트: %s

        [출력 형식]
        반드시 순수 JSON만 출력한다. (코드펜스, 불필요한 텍스트 금지)

        {
          "summary": "string",
          "highlights": [
            { "rangeStart": number, "rangeEnd": number }
          ]
          "valid": true | false
        }

        규칙:
           1. summary
           - Markdown을 활용한다. (h2(##), h3(###), 굵게(**), 불릿(-), 숫자목록(1. 2. …)까지만 사용한다.)
           - 사용자의 요청이 '요약/재작성'에 해당하면, summary를 새로 작성한다.
           - 사용자의 요청이 '하이라이팅'을 포함하면, summary는 반드시 기존 summary 내용을 최대한 유지한다.
             - 단, '더 간결하게' '더 자세하게' 등 요약/재작성 요구와 함께 있을 경우, 요청을 따르되 기존 맥락을 보존하며 큰 변형 없이 수정한다.
           - 어떤 경우에도 불필요하게 원문 정보를 삭제하거나 누락하지 않는다.

           2. highlights
           - 사용자의 요청에 '하이라이팅'이 포함된 경우, summary 문자열 내에서 조건에 맞는 구간의 인덱스를 highlights로 추출한다.
           - 요청에 하이라이팅이 포함되지 않으면 highlights = []로 반환한다.

           3. 연관성 검증
           - 사용자의 요청이 카드 요약/재작성/하이라이팅과 무관하다고 판단되면,
             summary = "", highlights = [], valid = false 로 반환한다.
           - 정상적인 요청일 경우 valid = true 로 반환한다.

           4.입력 유효성 검증
           - 사용자의 요청이 지나치게 짧거나 의미를 파악하기 어려운 경우(valid=false로 간주한다).
             예: 초성 한 글자, 특수문자/이모티콘만 존재, 한 단어 이하의 불명확한 명령어 등
           - 최소 기준:
             - 요청 내 한글/영문/숫자 조합이 3자 미만이거나,
             - 의미 있는 동사나 명사가 포함되지 않은 경우,
             - 문맥상 카드 내용 수정 의도가 명확하지 않은 경우,
               → summary = "", highlights = [], valid = false 로 반환한다.
           - 단, 명확한 단어 기반 지시(예: "요약해", "강조해줘")는 예외로 valid=true 처리한다.

           5. 공통
           - summary와 highlights는 항상 세트로 반환한다.
           - 새로운 개념, 없는 정보, 과장된 내용은 절대 추가하지 않는다.
           - highlights의 인덱스는 summary 문자열 기준으로 정확히 계산한다.
        """,
      prompt,
      cardTitle != null ? cardTitle : "",
      currentSummary != null ? currentSummary : "",
      description != null ? description : "",
      subtitle != null ? subtitle : "",
      transcript != null ? transcript : ""
    );

    Map<String, Object> requestBody = Map.of(
      "model", "gpt-4o",
      "messages", List.of(
        Map.of("role", "system", "content",
          "You are a helpful assistant that rewrites summaries according to user instructions "
            + "and optionally extracts highlight ranges."),
        Map.of("role", "user", "content", userPrompt)
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

      log.info("Regenerated summary + highlights: {}", content);

      // 정제
      String cleaned = content.trim();
      if (cleaned.startsWith("```")) {
        cleaned = cleaned.replaceAll("```json", "")
          .replaceAll("```", "")
          .trim();
      }

      // JSON 파싱
      ObjectMapper mapper = new ObjectMapper();
      JsonNode json = mapper.readTree(cleaned);

      String summary = json.has("summary") ? json.get("summary").asText() : "";
      List<HighlightRange> highlights = List.of();
      boolean valid = json.has("valid") && json.get("valid").asBoolean();

      if (json.has("highlights")) {
        highlights = mapper.convertValue(
          json.get("highlights"),
          new TypeReference<List<HighlightRange>>() {
          }
        );
      }

      return Map.of(
        "summary", summary,
        "highlights", highlights,
        "valid", valid
      );

    } catch (Exception e) {
      log.error("GPT summary regeneration failed", e);
      throw new RuntimeException("GPT summary regeneration failed", e);
    }
  }

}
