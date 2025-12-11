package com.nubo.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;

  /**
   * JWT 토큰 유효성을 검증하고 인증 정보를 SecurityContext에 설정한다.
   *
   * @param request     요청 객체
   * @param response    응답 객체
   * @param filterChain 필터 체인
   * @exception ServletException 필터 처리 중 오류
   * @exception IOException      입출력 오류
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain) throws ServletException, IOException {
    String token = resolveToken(request);

    if (token != null && jwtProvider.validateAccessToken(token)) {
      Authentication authentication = jwtProvider.getAuthentication(token);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  /**
   * HTTP 요청 헤더에서 JWT access token을 추출한다.
   *
   * @param request HTTP 요청
   * @return 추출된 JWT 토큰 또는 null
   */
  private String resolveToken(HttpServletRequest request) {
    String bearer = request.getHeader("Authorization");
    if (bearer != null && bearer.startsWith("Bearer ")) {
      return bearer.substring(7);
    }
    return null;
  }
}

