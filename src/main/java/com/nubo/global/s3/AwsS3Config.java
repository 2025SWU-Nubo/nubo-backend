package com.nubo.global.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

  @Value("${cloud.aws.region.static}")
  private String region;

  /**
   * S3 Presigner Bean 등록
   * Presigned URL 발급 시 사용
   */
  @Bean
  public S3Presigner s3Presigner() {
    return S3Presigner.builder()
      .region(Region.of(region))
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build();
  }
}
