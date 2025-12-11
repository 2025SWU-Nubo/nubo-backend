package com.nubo.global.jwt;

import com.nubo.domain.user.entity.User;
import com.nubo.domain.user.repository.UserRepository;
import com.nubo.global.auth.CustomUserDetails;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

  private final UserRepository userRepository;
  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.access-expiration-ms}")
  private long accessExpirationMs;

  @Value("${jwt.refresh-expiration-ms}")
  private long refreshExpirationMs;

  private Key key;

  public JwtProvider(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Refresh Token 만료 시간(ms)을 반환한다.
   *
   * @return refresh token expiration milliseconds
   */
  public long getRefreshExpirationMs() {
    return refreshExpirationMs;
  }

  /**
   * JWT 서명을 위한 Key를 초기화한다.
   * secret 길이가 32자 미만이면 HMAC-SHA256에 적합하지 않기 때문에 예외를 발생시킨다.
   */
  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
  }

  /**
   * Access/Refresh Token을 공통 로직으로 생성한다.
   *
   * @param userId 사용자 ID
   * @param exp    토큰 만료 시간(ms)
   * @param type   토큰 타입("ACCESS" 또는 "REFRESH")
   * @return 생성된 JWT 문자열
   */
  private String createToken(Long userId, long exp, String type) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + exp);

    return Jwts.builder()
      .setSubject(userId.toString()) // 사용자 ID를 payload에 저장
      .claim("type", type) // ACCESS / REFRESH 구분
      .setIssuedAt(now)
      .setExpiration(expiry)
      .signWith(key, SignatureAlgorithm.HS256)
      .compact();
  }

  /**
   * Access Token을 생성한다.
   *
   * @param userId 사용자 ID
   * @return Access Token(JWT)
   */
  public String createAccessToken(Long userId) {
    return createToken(userId, accessExpirationMs, "ACCESS");
  }

  /**
   * Refresh Token을 생성한다.
   *
   * @param userId 사용자 ID
   * @return Refresh Token(JWT)
   */
  public String createRefreshToken(Long userId) {
    return createToken(userId, refreshExpirationMs, "REFRESH");
  }

  /**
   * JWT 문자열을 Claims 형태로 파싱한다.
   *
   * @param token JWT 토큰 문자열
   * @return 토큰에 포함된 Claims (payload 정보)
   */
  private Claims parseToken(String token) {
    return Jwts.parserBuilder()
      .setSigningKey(key)
      .build()
      .parseClaimsJws(token)
      .getBody();
  }

  /**
   * Access Token의 유효성을 검사한다.
   *
   * @param token 검사할 JWT
   * @return 유효하면 true, 아니면 false
   */
  public boolean validateAccessToken(String token) {
    try {
      Claims claims = parseToken(token);
      return "ACCESS".equals(claims.get("type", String.class))
        && claims.getExpiration().after(new Date());
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Refresh Token의 유효성을 검사한다.
   *
   * @param token 검사할 JWT
   * @return 유효하면 true, 아니면 false
   */
  public boolean validateRefreshToken(String token) {
    try {
      Claims claims = parseToken(token);
      return "REFRESH".equals(claims.get("type", String.class))
        && claims.getExpiration().after(new Date());
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * JWT 토큰에서 사용자 ID를 추출한다.
   *
   * @param token JWT access token
   * @return 추출된 사용자 ID
   */
  public Long extractUserId(String token) {
    Claims claims = parseToken(token);
    return Long.parseLong(claims.getSubject());
  }

  /**
   * JWT 기반으로 Authentication 객체를 생성하여 SecurityContext에서 사용할 수 있게 한다.
   *
   * @param token JWT
   * @return Authentication 객체
   */
  public Authentication getAuthentication(String token) {
    Long userId = extractUserId(token); // 토큰에서 userId 추출
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED_CLIENT));

    CustomUserDetails userDetails = new CustomUserDetails(user);

    return new UsernamePasswordAuthenticationToken(
      userDetails,
      null,
      userDetails.getAuthorities()
    );
  }

  /**
   * Access Token이 만료되었는지 확인한다.
   *
   * @param token JWT
   * @return 만료 시 true
   */
  public boolean isAccessTokenExpired(String token) {
    try {
      Claims claims = parseToken(token);
      return claims.getExpiration().before(new Date());
    } catch (ExpiredJwtException e) {
      return true; // 명시적으로 만료된 경우
    } catch (JwtException | IllegalArgumentException e) {
      return false; // 유효하지 않지만 만료로 판단하지는 않음
    }
  }
}
