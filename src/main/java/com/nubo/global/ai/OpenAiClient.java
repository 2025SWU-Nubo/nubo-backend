package com.nubo.global.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nubo.domain.board.service.BoardService;
import com.nubo.domain.board.type.DefaultBoard;
import com.nubo.domain.card.dto.AiCardMetaDto;
import com.nubo.domain.card.dto.CardSummaryUpdateRequestDto.HighlightRange;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.Arrays;
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
  private final BoardService boardService;

  @Value("${openai.api-key}")
  private String apiKey;

  /**
   * 카드와 원본 영상 데이터를 기반으로 summary를 생성한다.
   *
   * @param inputText 가공할 원본 메타데이터의 합본 텍스트
   * @param userId    사용자 id
   * @return 생성된 메타데이터 dto
   */
  public AiCardMetaDto generateCardMeta(String inputText, Long userId, boolean skipBoardFetch,
    boolean isRecommendation) {
    String prompt = buildPrompt(inputText);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    Map<String, Object> requestBody = Map.of(
      "model", "gpt-5.1",
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

      // JSON 형식 유효성 검사 추가
      if (!content.trim().startsWith("{")) {
        log.warn("GPT 응답이 JSON 형식이 아닙니다: {}", content);
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }

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
        if (isRecommendation) {
          log.info("추천 모드: 불충분 콘텐츠(노래/가사 등) 감지됨 -> 카드 생성 중단 (null 반환)");
          return null;
        }
        log.info("사용자 생성 모드: 불충분 콘텐츠 감지됨 → fallback 메타 적용");
        summary = "이 영상은 자동 요약이 어려워요. 필요한 내용을 직접 메모로 추가해 주세요.";

        // 태그 생략
        tags = List.of();

        // 보드 매핑은 '기타'
        boardName = "기타";
      }

      final String resolvedBoardName = boardName;

      DefaultBoard matched = Arrays.stream(DefaultBoard.values())
        .filter(b -> b.getDisplayName().equals(resolvedBoardName))
        .findFirst()
        .orElse(DefaultBoard.ETC);

      Long boardId = null;
      if (!skipBoardFetch) {
        boardId = boardService
          .getAiBoardByUserAndCategory(userId, matched)
          .getId();
      }

      return AiCardMetaDto.builder()
        .title(title)
        .summary(summary)
        .tags(tags)
        .boardId(boardId)
        .aiCategory(boardName)
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
      - title과 description 내용을 우선적으로 참고하여 summary를 작성한다.
      - 원본 제목(title)은 제목 결정 시에만 참고하며, summary/tags/board에는 절대 사용하지 않는다.
      - 새로운 사실이나 근거 없는 정보 추가는 금지하지만,
        잘못 인식된 단어나 문장의 오류는 영상의 맥락에 따라 자연스럽게 보정할 수 있다.

      [언어 혼용 및 발음 오류 처리]
      - transcript나 subtitle 내에 언어가 혼용되어 있거나 발음 인식 오류가 있는 경우,
        의미를 왜곡하지 않는 선에서 영상의 주제나 의도를 파악하기 위한 **자연스러운 수준의 유추**는 허용한다.
        (예: 영어 단어 발음 설명 영상에서 단어가 잘못 인식된 경우, 해당 단어를 복원하거나 올바른 형태로 표현 가능)
      - 단, 의미를 왜곡하거나 불확실한 정보를 추가하는 추측은 여전히 금지한다.
      - 발음, 번역, 언어 혼용을 정정할 때는 영상의 **교육 목적이나 맥락**을 우선 고려한다.

      [출력 형식]
      순수 JSON만 출력한다. (코드펜스, 추가 텍스트 금지)
      값이 없으면 title="", summary="", tags=[], board="" 로 반환한다.

      출력 스키마:
      {
        "title": "string",
        "summary": "string (Markdown 허용: ##, ###, **, -, 1.)",
        "tags": ["string", ...],
        "board": "string"
      }

      [Markdown 작성 규칙]

      1. **허용 문법**
      - Heading: `##`, `###` (단락 맨 앞에서만 사용)
      - Unordered list: `-` 사용하되 **하위 리스트는 절대 작성하지 않는다.**
      - Ordered list: 항상 `1.`로 표기하며 **하위 리스트는 절대 작성하지 않는다.**
      - Bold: `**굵게**` 표현 허용
      - 문단(heading/문장) 사이에는 빈 줄 한 줄(`\\n\\n`)을 넣어 구분한다.
        - 단, **리스트 항목(`-`, `1.`) 사이에는는 빈 줄을 넣지 않는다.**
         
      2. **금지 문법**
      - *기울임체*, _밑줄_, `인라인 코드`, ```코드블록```, [링크](url), > 인용문, HTML 태그 등은 사용하지 않는다.
      - 이미지는 생성하지 않는다.

      [노트 생성 규칙]

      3. **제목(title)**
      - description, transcript, subtitle의 내용을 바탕으로 새로운 한국어 제목을 생성한다.
      - 길이는 14자 이내로 제한한다.
      - 제목은 핵심 주제나 내용을 간결하게 표현해야 하며, 이모지/해시태그/과장/추측/광고 표현은 금지한다.
      - description, transcript, subtitle이 모두 비어 있을 경우에만 title=""

      4. **요약(summary)**
      - 위의 Markdown 규칙을 엄격히 따른다.
      - 학습 노트 스타일의 문장을 가능한 명사형 종결 어미 위주로 작성하되,
        문맥상 자연스러운 문장 흐름을 위해 일부 서술형 종결도 허용한다.
      - 불필요하게 모든 내용을 리스트로 나열하지 않는다.
      - 내용이 순차적이거나 항목 구분이 명확할 때만 리스트(`-`, `1.`)를 사용한다.
      - 광고, 홍보, 과장, 클릭 유도 금지. 사실 서술 위주로 작성한다.
      - 문단 간에는 반드시 빈 줄(`\\n\\n`)을 삽입한다. 단, **리스트 항목(`-`, `1.`) 사이에는 삽입하지 않는다.**
      - 불필요한 스타일링이나 HTML은 절대 포함하지 않는다.
      - **만약 transcript나 subtitle이 노래 가사이거나, 노래/음악 관련 단어가 주를 이룬다면:**
        - summary는 "이 영상은 자동 요약이 어려워요. 필요한 내용을 직접 메모로 추가해 주세요." 로 고정한다.
        - board는 title값이 있는 경우 해당 값을 참고하여 가장 적절한 항목을 선택하며, 판단하기 어려운 경우 '기타'로 분류한다.
        - tags는 생성하지 않는다([] 반환).

      5. **태그(tags)**
      - 최소 5개, 최대 7개 생성.
      - 영상 내용이 매우 제한적인 경우에는 5개 미만도 가능.
      - **세부적인 핵심 키워드(3~4개)**와 **포괄적·상위 개념 키워드(2~3개)**를 모두 포함할 것.
      - 각 태그는 1~2 단어의 핵심 키워드로 구성.
      - 중복, 의미 없음, 이모지, 해시태그 금지.

      6. **보드(board)**
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

        [Markdown 작성 규칙]

        1. **허용 문법**
        - Heading: `##`, `###` (단락의 맨 앞에서만 사용)
        - Unordered list: `-` 사용하되 **하위 리스트는 절대 작성하지 않는다.**
        - Ordered list: 항상 `1.`로 표기하며 **하위 리스트는 절대 작성하지 않는다.**
        - Bold: `**굵게**` 표현 허용
        - 문단(heading/문장) 사이에는 빈 줄 한 줄(`\\n\\n`)을 넣어 구분한다.
          - 단, **리스트 항목(`-`, `1.`) 사이에는 빈 줄을 넣지 않는다.**

        2. **금지 문법**
        - *기울임체*, _밑줄_, `인라인 코드`, ```코드블록```, [링크](url), > 인용문, HTML 태그 등은 사용하지 않는다.
        - 이미지는 생성하지 않는다.

        3. **요약(summary) 작성 원칙**
        - 위의 Markdown 규칙을 반드시 따른다.
        - 학습 노트 스타일의 문장을 명사형 종결 어미로 작성하되,
          문맥상 자연스러운 문장 흐름을 위해 일부 서술형 종결도 허용한다.
        - 내용이 순차적이거나 항목 구분이 명확할 때에는 리스트(`-`, `1.`)를 사용한다.
        - 광고, 홍보, 과장, 클릭 유도 표현은 금지한다.
        - **리스트 항목(`-`, `1.`) 사이에는 빈 줄을 삽입하지 않는다.**
        - 기존 내용의 의미나 사실을 임의로 삭제하거나 왜곡하지 않는다.

        4. **요청 처리 규칙**
        - 사용자의 요청이 '요약/재작성'이라면 summary를 새로 작성한다.
        - 요청에 '하이라이팅'이 포함된 경우:
          - summary는 기존 summary 내용을 최대한 유지하되, 요청에 따라 강조 구간을 지정한다.
          - 기존 문맥을 유지한 채로 필요한 부분만 수정하거나 강조 표시한다.
        - '더 간결하게', '더 자세하게' 등 요약 관련 지시가 포함되면 요청을 우선하되 의미를 훼손하지 않는다.

        5. **highlights 작성 규칙**
        - 요청에 '하이라이팅'이 포함된 경우 summary 문자열 내에서 조건에 맞는 구간 인덱스를 추출한다.
        - 요청에 하이라이팅이 포함되지 않으면 highlights = [] 로 반환한다.

        6. **심화학습 요청 처리**
        - 사용자의 요청에 '심화학습', '더 깊게 설명해줘', '배경 지식 알려줘' 등의 표현이 포함된 경우,
          GPT가 이미 알고 있는 사실 기반의 보충 설명을 포함할 수 있다.
        - 사용자의 의도를 최우선 하되, 프롬프트가 구체적이지 않거나 모호한 경우에는
          영상의 주제나 기존 요약 맥락에 따라 스스로 가장 자연스럽고 유익한 심화 방향을 선택한다.
        - 단, 아래 기준을 반드시 지킨다:
          1. 학습·교육적 이해를 돕기 위한 객관적 정보만 추가할 것.
          2. 출처가 명확하거나 일반적으로 검증된 사실(예: 사전 정의, 기본 원리, 공식 등)에 한해 허용.
          3. 추측성, 개인적 의견, 출처 불분명한 내용은 절대 포함하지 않는다.
        - 이 경우에도 Markdown 형식을 유지하며, 추가 정보는 기존 내용의 맥락과 자연스럽게 이어지도록 작성한다.

        7. **요청 유효성 검증**
        - 요청이 지나치게 짧거나 카드 내용과 무관하거나 의미 없는 경우(valid=false)
          예: 초성/특수문자/이모티콘만 존재, 한 단어 이하의 불명확한 명령어
        - 최소 기준:
          - 요청 내 한글/영문/숫자 조합이 3자 미만이거나,
          - 의미 있는 동사나 명사가 포함되지 않은 경우,
          - 문맥상 카드 내용 수정 의도가 명확하지 않은 경우,
            → summary = "", highlights = [], valid = false 로 반환한다.
        - 단, "요약해", "강조해줘" 등 명확한 단어 기반 요청은 valid=true로 간주한다.

        8. **공통**
        - summary와 highlights는 항상 세트로 반환한다.
        - 새로운 사실이나 근거 없는 정보 추가는 금지하지만,
          잘못 인식된 단어나 문장의 오류는 영상의 맥락에 따라 자연스럽게 보정할 수 있다.
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
      "model", "gpt-5.1",
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
        cleaned = cleaned.replaceAll("(?i)```(json|bash)?", "").trim();
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

  /*
   * 카테고리별 트렌드 키워드 생성
   */
  public List<String> generateTrendingKeywords(DefaultBoard category) {

    String prompt = buildKeywordPrompt(category);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    Map<String, Object> requestBody = Map.of(
      "model", "gpt-5.1-mini",
      "response_format", Map.of("type", "json_object"),
      "messages", List.of(
        Map.of("role", "user", "content", prompt)
      )
    );

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

    ResponseEntity<Map> response = restTemplate.postForEntity(
      "https://api.openai.com/v1/chat/completions",
      request,
      Map.class
    );

    Map<String, Object> choice = ((List<Map<String, Object>>) response.getBody()
      .get("choices")).get(0);
    Map<String, Object> message = (Map<String, Object>) choice.get("message");

    String contentJson = (String) message.get("content");

    try {
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> json = mapper.readValue(contentJson, Map.class);

      // "keywords": ["...", "..."]
      List<String> keywords = (List<String>) json.get("keywords");
      return keywords != null ? keywords : List.of();

    } catch (Exception e) {
      e.printStackTrace();
      return List.of();
    }
  }

  private String buildKeywordPrompt(DefaultBoard category) {
    return """
      당신은 유튜브 쇼츠 트렌드를 분석하는 도우미입니다.

      아래 카테고리에 대해 한국에서 최근 1~2주 동안
      실제로 자주 검색되었을 법한 '정보성·학습 목적의' 키워드 5개를 생성해 주세요.

      조건:
      - 엔터테인먼트/밈/브이로그/ASMR 제외
      - 브랜드명/게임명/인물명/국가명 등 고유명사 제외
      - 너무 일반적인 단어 제외
      - 길이는 3~8자, 한국어 표현 중심
      - 학습·실용 정보를 제공하는 단어만 선택

      카테고리: %s

      반드시 아래 JSON 형식으로만 답하세요.

      {
        "keywords": ["키워드1", "키워드2", ... ]
      }

      """.formatted(category.getDisplayName());
  }
}
