package com.nubo.domain.video.type;

import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;

public enum Platform {
  YOUTUBE,
  INSTAGRAM,
  TIKTOK;

  public static Platform fromUrl(String url) {
    if (url == null) {
      throw new IllegalArgumentException("URL must not be null");
    }

    String lowerUrl = url.toLowerCase();

    if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be")) {
      return YOUTUBE;
    } else if (lowerUrl.contains("instagram.com/reel/") || lowerUrl.contains("instagram.com/p/")) {
      return INSTAGRAM;
    } else if (lowerUrl.contains("tiktok.com")) {
      return TIKTOK;
    }

    throw new ApiException(ErrorCode.UNSUPPORTED_PLATFORM);
  }
}
