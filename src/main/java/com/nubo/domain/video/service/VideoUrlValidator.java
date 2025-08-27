package com.nubo.domain.video.service;

import com.nubo.domain.video.type.Platform;
import com.nubo.global.error.ErrorCode;
import com.nubo.global.error.exception.ApiException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class VideoUrlValidator {

  // 유튜브 쇼츠 (예: https://youtube.com/shorts/abc123?si=xxxx)
  private static final Pattern YOUTUBE_SHORTS =
    Pattern.compile("^https:\\/\\/(www\\.)?youtube\\.com\\/shorts\\/[A-Za-z0-9_-]+(\\?.*)?$");

  // 인스타그램 릴스 (예: https://www.instagram.com/reel/abc123/?utm_source=ig_web_copy_link)
  private static final Pattern INSTAGRAM_REELS =
    Pattern.compile("^https:\\/\\/(www\\.)?instagram\\.com\\/reel\\/[A-Za-z0-9_-]+(\\/)?(\\?.*)?$");

  // 틱톡 (예: https://www.tiktok.com/@username/video/1234567890)
  private static final Pattern TIKTOK =
    Pattern.compile("^https:\\/\\/(www\\.)?tiktok\\.com\\/@[\\w\\.-]+\\/video\\/[0-9]+(\\?.*)?$");

  /**
   * URL을 받아서 지원하는 플랫폼인지 검사한다.
   *
   * @param url 사용자 입력 URL
   * @return Platform (YOUTUBE, INSTAGRAM, TIKTOK)
   * @exception ApiException INVALID_VIDEO_URL (지원하지 않는 경우)
   */
  public Platform validatePlatform(String url) {
    if (url == null || url.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_VIDEO_URL);
    }
    if (YOUTUBE_SHORTS.matcher(url).matches()) {
      return Platform.YOUTUBE;
    }
    if (INSTAGRAM_REELS.matcher(url).matches()) {
      return Platform.INSTAGRAM;
    }
    if (TIKTOK.matcher(url).matches()) {
      return Platform.TIKTOK;
    }

    throw new ApiException(ErrorCode.INVALID_VIDEO_URL);
  }
}
