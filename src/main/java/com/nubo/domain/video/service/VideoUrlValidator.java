package com.nubo.domain.video.service;

import com.nubo.domain.video.type.Platform;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class VideoUrlValidator {

  /**
   * YouTube Shorts
   * - supports: www / m / no-www
   * - supports query params / trailing slash
   */
  private static final Pattern YOUTUBE_SHORTS =
    Pattern.compile(
      "^https:\\/\\/(www\\.|m\\.)?youtube\\.com\\/shorts\\/[A-Za-z0-9_-]+(\\/)?(\\?.*)?$");

  /**
   * Instagram Reels
   * - supports: www / m / no-www
   * - supports query params / trailing slash
   */
  private static final Pattern INSTAGRAM_REELS =
    Pattern.compile(
      "^https:\\/\\/(www\\.|m\\.)?instagram\\.com\\/reel\\/[A-Za-z0-9_-]+(\\/)?(\\?.*)?$");

  /**
   * Tiktok
   * - 현재버전 미지원
   */
//  private static final Pattern TIKTOK =
//    Pattern.compile("^https:\\/\\/(www\\.)?tiktok\\.com\\/@[\\w\\.-]+\\/video\\/[0-9]+(\\?.*)?$");

  /**
   * URL을 받아서 지원하는 플랫폼인지 검사한다.
   *
   * @param url 사용자 입력 URL
   * @return Platform (YOUTUBE, INSTAGRAM, TIKTOK)
   * @exception ApiException INVALID_VIDEO_URL (지원하지 않는 경우)
   */
  public Platform validatePlatform(String url, Long userId) {
    if (url == null || url.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_VIDEO_URL);
    }
    if (YOUTUBE_SHORTS.matcher(url).matches()) {
      return Platform.YOUTUBE;
    }
    if (INSTAGRAM_REELS.matcher(url).matches()) {
      return Platform.INSTAGRAM;
    }
//    if (TIKTOK.matcher(url).matches()) {
//      return Platform.TIKTOK;
//    }

    throw new ApiException(ErrorCode.INVALID_VIDEO_URL);
  }
}
