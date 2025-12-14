package com.nubo.domain.video.service;

import com.nubo.domain.video.type.Platform;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.regex.Matcher;
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

    String normalizedUrl = normalizeUrl(url);

    if (YOUTUBE_SHORTS.matcher(normalizedUrl).matches()) {
      return Platform.YOUTUBE;
    }
    if (INSTAGRAM_REELS.matcher(normalizedUrl).matches()) {
      return Platform.INSTAGRAM;
    }
//    if (TIKTOK.matcher(url).matches()) {
//      return Platform.TIKTOK;
//    }

    throw new ApiException(ErrorCode.INVALID_VIDEO_URL);
  }

  /**
   * 공유 문자열 등에서 실제 URL만 추출하여 정규화한다.
   *
   * 예:
   * "@xxx님의 Instagram 게시물 보기 https://www.instagram.com/reel/ABC/?utm=..."
   * → "https://www.instagram.com/reel/ABC/?utm=..."
   */
  public String normalizeUrl(String raw) {
    if (raw == null) {
      return null;
    }

    Pattern urlPattern = Pattern.compile("(https?://[^\\s]+)");
    Matcher matcher = urlPattern.matcher(raw);

    if (matcher.find()) {
      return matcher.group(1);
    }

    // URL을 찾지 못하면 원본 그대로 반환 (validate 단계에서 걸러짐)
    return raw;
  }
}
