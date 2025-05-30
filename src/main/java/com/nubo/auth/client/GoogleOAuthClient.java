package com.nubo.auth.client;

import com.nubo.auth.dto.GoogleTokenResponseDto;
import com.nubo.auth.dto.GoogleUserInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

  private final RestTemplate restTemplate = new RestTemplate();
  @Value("${oauth.google.client-id}")
  private String clientId;
  @Value("${oauth.google.client-secret}")
  private String clientSecret;
  @Value("${oauth.google.redirect-uri}")
  private String redirectUri;

  /**
   * Google authorization code로 accessToken, idToken 등을 요청한다.
   */
  public GoogleTokenResponseDto requestAccessToken(String authCode) {
    String url = "https://oauth2.googleapis.com/token";

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("code", authCode);
    params.add("client_id", clientId);
    params.add("client_secret", clientSecret);
    params.add("grant_type", "authorization_code");
    params.add("redirect_uri", redirectUri);

    log.info("▶ [Google 토큰 요청] code={}, redirectUri={}, clientId={}, clientSecret={}",
      authCode, redirectUri, clientId, clientSecret);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    return restTemplate.postForObject(url, request, GoogleTokenResponseDto.class);
  }

  /**
   * accessToken을 이용해 Google 사용자 정보를 조회한다.
   */
  public GoogleUserInfoDto requestUserInfo(String accessToken) {
    String url = "https://www.googleapis.com/oauth2/v3/userinfo";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken); // Authorization: Bearer access_token

    HttpEntity<Void> request = new HttpEntity<>(headers);

    ResponseEntity<GoogleUserInfoDto> response =
      restTemplate.exchange(url, HttpMethod.GET, request, GoogleUserInfoDto.class);

    return response.getBody();
  }
}
