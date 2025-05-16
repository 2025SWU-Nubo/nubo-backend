package com.nubo.global.jwt;

import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration-ms}")
  private long expirationMs;

  private Key key;

  /**
   * JWT 서명을 위한 키를 초기화한다.
   */
  @PostConstruct
  public void init() {
    if (secret == null || secret.length() < 32) {
      throw new ApiException(ErrorCode.INVALID_JWT_TOKEN);
    }
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
  }

  /**
   * 사용자 ID를 기반으로 JWT access token을 생성한다.
   *
   * @param userId 사용자 고유 ID
   * @return 생성된 JWT access token
   */
  public String createAccessToken(Long userId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);

    return Jwts.builder()
      .setSubject(userId.toString()) // 사용자 ID를 payload에 저장
      .setIssuedAt(now)
      .setExpiration(expiry)
      .signWith(key, SignatureAlgorithm.HS256)
      .compact();
  }

  /**
   * JWT 토큰에서 사용자 ID를 추출한다.
   *
   * @param token JWT access token
   * @return 추출된 사용자 ID
   */
  public Long extractUserId(String token) {
    try {
      Claims claims = parseToken(token);
      return Long.parseLong(claims.getSubject());
    } catch (Exception e) {
      throw new ApiException(ErrorCode.INVALID_JWT_SECRET);
    }
  }

  /**
   * 주어진 JWT 토큰의 유효성을 검사한다.
   *
   * @param token 검사할 JWT access token
   * @return 유효하면 true, 그렇지 않으면 false
   */
  public boolean validateToken(String token) {
    try {
      parseToken(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private Claims parseToken(String token) {
    return Jwts.parserBuilder()
      .setSigningKey(key)
      .build()
      .parseClaimsJws(token)
      .getBody();
  }
}
