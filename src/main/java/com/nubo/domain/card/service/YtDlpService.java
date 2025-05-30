package com.nubo.domain.card.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nubo.domain.video.dto.VideoMetadataDto;
import com.nubo.domain.video.type.Platform;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class YtDlpService {

  // yt-dlp 실행 파일 경로 (로컬 환경)
  private static final String YT_DLP_PATH = "C:\\Users\\user\\whisper-test\\venv\\Scripts\\yt-dlp"
    + ".exe";
  // 다운로드 파일 저장 경로
  private static final String DOWNLOAD_DIR = "downloads";

  /**
   * 다운로드 디렉토리를 초기화한다.
   * 디렉토리가 없으면 새로 생성한다.
   */
  public YtDlpService() {
    // downloads 디렉토리가 없으면 생성
    try {
      Files.createDirectories(Paths.get(DOWNLOAD_DIR));
    } catch (IOException e) {
      log.error("downloads 디렉토리 생성 실패", e);
    }
  }

  /**
   * YouTube 영상에서 오디오와 메타데이터를 동시에 추출한다.
   * 성능 최적화를 위해 하나의 명령어로 처리하며, 파일은 작업 후 삭제된다.
   *
   * @param url YouTube 영상 URL
   * @return 오디오 바이트와 메타데이터 DTO가 포함된 추출 결과
   * @exception IOException          yt-dlp 실행 실패 또는 파일 읽기 오류
   * @exception InterruptedException 프로세스 실행 중 인터럽트 발생
   */
  public ExtractResult extractAudioAndMetadata(String url)
    throws IOException, InterruptedException {
    String uniqueName = "shorts_" + System.currentTimeMillis();
    String outputBase = DOWNLOAD_DIR + "/" + uniqueName;
    String outputTemplate = outputBase + ".%(ext)s";

    List<String> command = new ArrayList<>();
    command.add(YT_DLP_PATH);
    command.add("-f");
    command.add("bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio"); // 더 빠른 포맷 우선
    command.add("--extract-audio");
    command.add("--audio-format");
    command.add("wav");
    command.add("--audio-quality");
    command.add("5"); // 품질을 적절히 낮춰 속도 향상 (0=최고, 9=최저)
    command.add("--write-info-json"); // 메타데이터 JSON 파일도 함께 생성
    command.add("--no-write-playlist-metafiles"); // 플레이리스트 메타파일 제외
    command.add("-o");
    command.add(outputTemplate);
    command.add(url);

    log.info("오디오 및 메타데이터 통합 추출 시작: {}", url);
    long startTime = System.currentTimeMillis();

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.inheritIO();
    Process process = pb.start();
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      throw new RuntimeException("yt-dlp 통합 추출 실패");
    }

    // 생성된 파일들 확인
    File wavFile = new File(outputBase + ".wav");
    File jsonFile = new File(outputBase + ".info.json");

    if (!wavFile.exists()) {
      throw new RuntimeException("WAV 파일이 생성되지 않았습니다: " + wavFile.getPath());
    }
    if (!jsonFile.exists()) {
      throw new RuntimeException("메타데이터 파일이 생성되지 않았습니다: " + jsonFile.getPath());
    }

    byte[] audioBytes;
    VideoMetadataDto metadata;

    try {
      // 오디오 파일 읽기
      try (FileInputStream fis = new FileInputStream(wavFile);
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

        byte[] buffer = new byte[8192];
        int len;
        while ((len = fis.read(buffer)) != -1) {
          baos.write(buffer, 0, len);
        }
        audioBytes = baos.toByteArray();
      }

      // 메타데이터 파일 읽기
      ObjectMapper mapper = new ObjectMapper();
      JsonNode info = mapper.readTree(jsonFile);

      String videoId = info.get("id").asText();
      String title = info.has("title") ? info.get("title").asText("") : "";
      String description = info.has("description") ? info.get("description").asText("") : "";
      String thumbnail = info.has("thumbnail") ? info.get("thumbnail").asText("") : "";

      metadata = VideoMetadataDto.builder()
        .videoId(videoId)
        .videoUrl(url)
        .title(title)
        .description(description)
        .thumbnailUrl(thumbnail)
        .platform(Platform.YOUTUBE)
        .build();

    } finally {
      // 파일 정리
      if (wavFile.exists()) {
        wavFile.delete();
      }
      if (jsonFile.exists()) {
        jsonFile.delete();
      }
    }

    log.info("통합 추출 완료 - 소요시간: {}ms, 오디오 크기: {}KB",
      System.currentTimeMillis() - startTime, audioBytes.length / 1024);

    return new ExtractResult(audioBytes, metadata);
  }

  /**
   * 기존 호환성을 위한 개별 오디오 추출 메서드
   *
   * @param url YouTube 영상 URL
   * @return 오디오 바이트 배열
   * @exception IOException          예외 발생 시
   * @exception InterruptedException 예외 발생 시
   * @deprecated extractAudioAndMetadata() 사용 권장
   */
  @Deprecated
  public byte[] extractAudioBytes(String url) throws IOException, InterruptedException {
    return extractAudioAndMetadata(url).getAudioBytes();
  }

  /**
   * 기존 호환성을 위한 개별 메타데이터 추출 메서드
   *
   * @param videoUrl YouTube 영상 URL
   * @return 메타데이터 DTO
   * @exception IOException          예외 발생 시
   * @exception InterruptedException 예외 발생 시
   * @deprecated extractAudioAndMetadata() 사용 권장
   */
  @Deprecated
  public VideoMetadataDto extractMetadata(String videoUrl)
    throws IOException, InterruptedException {
    return extractAudioAndMetadata(videoUrl).getMetadata();
  }

  /**
   * YouTube 영상 URL로부터 비디오 ID만 추출한다.
   * 내부적으로 URL 직접 파싱 또는 yt-dlp 호출을 통해 처리한다.
   *
   * @param videoUrl YouTube 영상 URL
   * @return 비디오 ID
   * @exception IOException          예외 발생 시
   * @exception InterruptedException 예외 발생 시
   */
  public String extractVideoIdOnly(String videoUrl) throws IOException, InterruptedException {
    // URL에서 직접 파싱할 수 있으면 더 빠름
    String directId = parseVideoIdFromUrl(videoUrl);
    if (directId != null) {
      return directId;
    }

    // yt-dlp로 추출
    List<String> command = List.of(
      YT_DLP_PATH,
      "--get-id",
      videoUrl
    );

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    Process process = pb.start();

    try (BufferedReader reader = new BufferedReader(
      new InputStreamReader(process.getInputStream()))) {
      String videoId = reader.readLine();
      process.waitFor();
      return videoId;
    }
  }

  /**
   * URL 문자열에서 직접 YouTube 영상 ID를 파싱한다.
   *
   * @param url YouTube URL
   * @return 파싱된 비디오 ID 또는 실패 시 null
   */
  private String parseVideoIdFromUrl(String url) {
    try {
      // YouTube Shorts: https://www.youtube.com/shorts/VIDEO_ID
      if (url.contains("youtube.com/shorts/")) {
        return url.substring(url.lastIndexOf("/") + 1).split("\\?")[0];
      }

      // YouTube 일반: https://www.youtube.com/watch?v=VIDEO_ID
      if (url.contains("youtube.com/watch?v=")) {
        String[] parts = url.split("v=");
        if (parts.length > 1) {
          return parts[1].split("&")[0];
        }
      }

      // YouTube 단축: https://youtu.be/VIDEO_ID
      if (url.contains("youtu.be/")) {
        return url.substring(url.lastIndexOf("/") + 1).split("\\?")[0];
      }

    } catch (Exception e) {
      log.warn("URL에서 비디오 ID 파싱 실패: {}", url, e);
    }

    return null; // 파싱 실패시 yt-dlp 사용
  }

  /**
   * yt-dlp로부터 추출한 오디오 및 메타데이터를 담는 내부 클래스
   */
  public static class ExtractResult {

    private final byte[] audioBytes;
    private final VideoMetadataDto metadata;

    public ExtractResult(byte[] audioBytes, VideoMetadataDto metadata) {
      this.audioBytes = audioBytes;
      this.metadata = metadata;
    }

    public byte[] getAudioBytes() {
      return audioBytes;
    }

    public VideoMetadataDto getMetadata() {
      return metadata;
    }
  }
}