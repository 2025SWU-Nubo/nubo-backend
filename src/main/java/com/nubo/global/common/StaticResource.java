package com.nubo.global.common;

/**
 * S3/CloudFront의 정적 리소스 URL을 카테고리별로 관리
 */
public enum StaticResource {

  // Dashboard
  DASHBOARD_BACKGROUND(
    "https://nubo-static-assets.s3.ap-northeast-2.amazonaws.com/dashboard/dashboard_background_03"
      + ".glb"),
  ;

  private final String url;

  StaticResource(String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }
}