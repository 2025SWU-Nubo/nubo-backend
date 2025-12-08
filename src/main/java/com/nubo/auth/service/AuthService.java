package com.nubo.auth.service;

import com.nubo.auth.client.GoogleOAuthClient;
import com.nubo.auth.dto.GoogleTokenResponseDto;
import com.nubo.auth.dto.GoogleUserInfoDto;
import com.nubo.auth.dto.LoginResponseDto;
import com.nubo.auth.dto.TokenCheckResponseDto;
import com.nubo.auth.entity.RefreshToken;
import com.nubo.auth.repository.RefreshTokenRepository;
import com.nubo.domain.user.dto.UserWithStatusDto;
import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.mapper.UserMapper;
import com.nubo.domain.user.service.UserService;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import com.nubo.global.jwt.JwtProvider;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserService userService;
  private final JwtProvider jwtProvider;
  private final UserMapper userMapper;
  private final GoogleOAuthClient googleOAuthClient;
  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * Google OAuth 인증 코드를 받아 사용자 인증을 수행한다.
   *
   * @param authCode 클라이언트로부터 전달받은 Google 인증 코드
   * @return JWT 및 사용자 정보가 포함된 응답 DTO
   */
  @Transactional
  public LoginResponseDto loginWithGoogle(String authCode) {
    // 1. accessToken 요청
    GoogleTokenResponseDto tokenResponse = googleOAuthClient.requestAccessToken(authCode);

    // 2. 사용자 정보 조회
    GoogleUserInfoDto userInfo = googleOAuthClient.requestUserInfo(tokenResponse.getAccess_token());

    // 3. DB에 사용자 등록 또는 조회
    User candidate = userMapper.fromGoogleUserInfo(userInfo);
    UserWithStatusDto userStatus = userService.getOrCreateUser(candidate);
    User user = userStatus.getUser();

    // 4. 서버 토큰(JWT) 발급
    String accessToken = jwtProvider.createAccessToken(user.getId());
    String refreshToken = jwtProvider.createRefreshToken(user.getId());

    // 5. RefreshToken DB 저장 (기존 토큰 삭제 후 새로 저장하는 방식 권장)
    refreshTokenRepository.deleteAllByUserId(user.getId());

    LocalDateTime expiresAt = LocalDateTime.now()
      .plus(jwtProvider.getRefreshExpirationMs(), ChronoUnit.MILLIS);

    RefreshToken savedRefreshToken = RefreshToken.of(
      refreshToken,
      user,
      expiresAt
    );

    refreshTokenRepository.save(savedRefreshToken);

    return new LoginResponseDto(
      accessToken,
      refreshToken,
      userMapper.toInfoDto(userStatus.getUser()),
      userStatus.isReactivated(),
      userStatus.isNewUser(),
      userStatus.getUser().isInterestSetupCompleted(),
      userStatus.getUser().isTutorialCompleted()
    );
  }

  /**
   * 주어진 JWT access token의 유효성과 만료 여부를 검사한다.
   *
   * @param accessToken 검사할 JWT access token
   * @return 유효성 및 만료 여부를 담은 TokenCheckResponseDto
   */
  public TokenCheckResponseDto checkTokenValidity(String accessToken) {
    boolean isValid = jwtProvider.validateAccessToken(accessToken);
    boolean isExpired = !isValid && jwtProvider.isAccessTokenExpired(accessToken);

    boolean interestSetupCompleted = false;
    boolean tutorialCompleted = false;

    if (isValid) {
      Long userId = jwtProvider.extractUserId(accessToken);
      User user = userService.getUserById(userId);

      interestSetupCompleted = user.isInterestSetupCompleted();
      tutorialCompleted = user.isTutorialCompleted();
    }

    return new TokenCheckResponseDto(
      isValid,
      isExpired,
      interestSetupCompleted,
      tutorialCompleted
    );
  }

  /**
   * Refresh Token을 검증하고 새로운 Access/Refresh Token을 재발급한다.
   * (이 메서드는 곧 AuthController의 /refresh 에서 호출될 예정)
   *
   * @param refreshToken 클라이언트가 보유한 Refresh Token
   * @return 새 AccessToken + 새 RefreshToken 포함한 LoginResponseDto
   */
  @Transactional
  public LoginResponseDto refreshTokens(String refreshToken) {

    // 1. refreshToken 형식 및 서명 검증
    if (!jwtProvider.validateRefreshToken(refreshToken)) {
      throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 2. DB에 저장된 RT인지 확인
    RefreshToken saved = refreshTokenRepository.findByToken(refreshToken)
      .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

    if (saved.isRevoked() || saved.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new ApiException(ErrorCode.EXPIRED_REFRESH_TOKEN);
    }

    User user = saved.getUser();

    // 3. 새 AT/RT 생성
    String newAccess = jwtProvider.createAccessToken(user.getId());
    String newRefresh = jwtProvider.createRefreshToken(user.getId());

    // 4. 기존 RT는 revoke
    saved.revoke();
    refreshTokenRepository.save(saved);

    // 5. 새로운 RT 저장
    LocalDateTime expiresAt = LocalDateTime.now()
      .plus(jwtProvider.getRefreshExpirationMs(), ChronoUnit.MILLIS);

    refreshTokenRepository.save(
      RefreshToken.of(
        newRefresh,
        user,
        expiresAt
      )
    );

    // 6. 응답 구성
    return new LoginResponseDto(
      newAccess,
      newRefresh,
      userMapper.toInfoDto(user),
      false,
      false,
      user.isInterestSetupCompleted(),
      user.isTutorialCompleted()
    );
  }
}
