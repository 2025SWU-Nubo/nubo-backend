package com.nubo.api.auth.service;

import com.nubo.api.auth.dto.GoogleUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

  private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";
  private final RestTemplate restTemplate = new RestTemplate();

  /**
   * Google access token을 이용해 사용자 정보를 요청한다.
   *
   * @param accessToken Google OAuth access token
   * @return Google 사용자 정보 DTO
   * @exception HttpClientErrorException 인증 실패 또는 API 호출 오류 시 발생
   */
  public GoogleUserInfoDto getUserInfo(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken); // Authorization: Bearer {token}

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<GoogleUserInfoDto> response = restTemplate.exchange(
        USERINFO_ENDPOINT,
        HttpMethod.GET,
        entity,
        GoogleUserInfoDto.class
      );
      return response.getBody();
    } catch (HttpClientErrorException e) {
      System.out.println("🔴 Google API 호출 실패: " + e.getStatusCode());
      System.out.println("🔴 Response: " + e.getResponseBodyAsString());
      throw e;
    }
  }
}
