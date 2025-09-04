package com.nubo.global.s3;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

  private final S3Presigner s3Presigner;

  @Value("${cloud.aws.s3.bucket}")
  private String bucketName;

  /**
   * 주어진 폴더에 Presigned URL 발급
   *
   * @param folder           업로드될 상위 폴더 (예: "profile", "cards/{cardId}")
   * @param originalFileName 업로드할 원본 파일 이름
   * @return presigned URL
   */
  public String generateUploadUrl(String folder, String originalFileName) {
    // 확장자 추출
    String extension = "";
    int dotIndex = originalFileName.lastIndexOf(".");
    if (dotIndex > 0) {
      extension = originalFileName.substring(dotIndex + 1).toLowerCase();
    }

    // Content-Type 결정
    String contentType;
    switch (extension) {
      case "png":
        contentType = "image/png";
        break;
      case "jpg":
      case "jpeg":
        contentType = "image/jpeg";
        break;
      case "gif":
        contentType = "image/gif";
        break;
      default:
        contentType = "application/octet-stream"; // 기본값
    }

    // UUID 기반 파일명 → 중복 방지
    String key = folder + "/" + UUID.randomUUID() + "." + extension;

    // PutObject 요청
    PutObjectRequest objectRequest = PutObjectRequest.builder()
      .bucket(bucketName)
      .key(key)
      .contentType(contentType)
      .build();

    // Presigned URL 생성 (5분 유효)
    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
      .signatureDuration(Duration.ofMinutes(5))
      .putObjectRequest(objectRequest)
      .build();

    PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

    return presignedRequest.url().toString();
  }
}
