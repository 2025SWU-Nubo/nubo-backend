package com.nubo.domain.video.type;

import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.net.URI;
import java.util.Set;

public enum Platform {
  YOUTUBE,
  INSTAGRAM,
  TIKTOK;

  // 지원 도메인 집합 (하위/단축/모바일 포함)
  private static final Set<String> YT_HOSTS = Set.of(
    "youtube.com", "www.youtube.com", "m.youtube.com",
    "youtu.be", "www.youtu.be"
  );
  private static final Set<String> IG_HOSTS = Set.of(
    "instagram.com", "www.instagram.com", "m.instagram.com"
  );
  private static final Set<String> TT_HOSTS = Set.of(
    "tiktok.com", "www.tiktok.com", "m.tiktok.com",
    "vt.tiktok.com", "vm.tiktok.com"
  );

  public static Platform fromUrl(String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("URL must not be null or blank");
    }

    // 스킴이 없으면 임시로 붙여서 URI 파싱 안정화
    String normalized = url.trim();
    if (!normalized.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
      normalized = "https://" + normalized;
    }

    URI uri;
    try {
      uri = URI.create(normalized);
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.UNSUPPORTED_PLATFORM); // or a specific INVALID_URL error
    }

    String host = uri.getHost();
    if (host == null) {
      throw new ApiException(ErrorCode.UNSUPPORTED_PLATFORM);
    }
    String h = host.toLowerCase();

    if (YT_HOSTS.contains(h)) {
      return YOUTUBE;
    }
    if (IG_HOSTS.contains(h)) {
      return INSTAGRAM;
    }
    if (TT_HOSTS.contains(h)) {
      return TIKTOK;
    }

    throw new ApiException(ErrorCode.UNSUPPORTED_PLATFORM);
  }
}
