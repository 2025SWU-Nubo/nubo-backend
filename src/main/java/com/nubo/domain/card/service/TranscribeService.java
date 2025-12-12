package com.nubo.domain.card.service;

import com.nubo.domain.card.dto.WhisperResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class TranscribeService {

  private final RestTemplate restTemplate;

  /**
   * 오디오 바이트 배열을 Whisper 서버에 전송하고 자막을 받아온다.
   *
   * @param audioBytes .wav 형식의 오디오
   * @return WhisperResponseDto (transcript, language)
   */
  public WhisperResponseDto transcribe(byte[] audioBytes) {
    // Whisper에 전송할 파일 생성
    ByteArrayResource resource = new ByteArrayResource(audioBytes) {
      @Override
      public String getFilename() {
        return "audio.wav"; // 파일명 지정
      }
    };

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", resource);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    // Whisper 서버 호출
    String whisperUrl = "https://nubo-whisper.fly.dev";
    ResponseEntity<WhisperResponseDto> response = restTemplate.postForEntity(
      whisperUrl,
      requestEntity,
      WhisperResponseDto.class
    );

    return response.getBody();
  }
}
