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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class YtDlpService {

  // yt-dlp 실행 파일 경로 (로컬 환경)
//  private static final String YT_DLP_PATH = "C:\\Users\\user\\whisper-test\\venv\\Scripts\\yt-dlp"
//    + ".exe";

  // 다운로드 파일 저장 경로
  private static final String DOWNLOAD_DIR = "downloads";
  @Value("${ext.ytdlp.path}")
  private String YT_DLP_PATH;
  @Value("${ext.ffmpeg.path:ffmpeg}") // PATH에 ffmpeg 있으면 그대로 사용
  private String FFMPEG_PATH;
  @Value("${ext.cookies.path:}") // 인스타 쿠키 필요 시 설정
  private String COOKIES_PATH;

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

  // 보조
  private static String text(JsonNode n, String key) {
    return (n != null && n.has(key) && !n.get(key).isNull()) ? n.get(key).asText() : null;
  }

  private static String firstNonEmpty(String... vals) {
    for (String v : vals) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
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
   * 플랫폼 공용: 메타데이터만 추출 (yt-dlp -J --skip-download)
   * Instagram 공개/일부 제한 컨텐츠는 쿠키 필요할 수 있음.
   */
  public VideoMetadataDto extractMetadataOnly(String url, Platform platform)
    throws IOException, InterruptedException {

    List<String> cmd = new ArrayList<>();
    cmd.add(YT_DLP_PATH);
    cmd.add("-J");
    cmd.add("--skip-download");

    // 인스타는 쿠키가 있으면 성공률이 올라감 (선택)
    if (platform == Platform.INSTAGRAM && COOKIES_PATH != null && !COOKIES_PATH.isBlank()) {
      cmd.add("--cookies");
      cmd.add(COOKIES_PATH);
    }

    cmd.add(url);

    ProcessBuilder pb = new ProcessBuilder(cmd);
//    pb.redirectErrorStream(true);
    pb.redirectErrorStream(false); // stderr는 따로 두기
    Process proc = pb.start();

    StringBuilder out = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
      String line;
      while ((line = br.readLine()) != null) {
        out.append(line);
      }
    }

    try (BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
      String errLine;
      while ((errLine = err.readLine()) != null) {
        log.warn("yt-dlp stderr: {}", errLine);
      }
    }
    
    int exit = proc.waitFor();
    if (exit != 0) {
      throw new IOException("yt-dlp metadata failed, exit=" + exit);
    }

    ObjectMapper om = new ObjectMapper();
    JsonNode root = om.readTree(out.toString());

    // 안전 파싱 (플랫폼별 결측 대비)
    String videoId = text(root, "id");
    String title = firstNonEmpty(text(root, "title"), text(root, "description"), "(제목 없음)");
    String thumbnail = text(root, "thumbnail");
    if ((thumbnail == null || thumbnail.isBlank()) && root.has("thumbnails")) {
      JsonNode thumbs = root.get("thumbnails");
      if (thumbs.isArray() && thumbs.size() > 0) {
        thumbnail = text(thumbs.get(thumbs.size() - 1), "url"); // 가장 큰 걸로 추정
      }
    }
    String webpageUrl = firstNonEmpty(text(root, "webpage_url"), url);

    return VideoMetadataDto.builder()
      .videoId(videoId)
      .videoUrl(webpageUrl)
      .title(title)
      .description(firstNonEmpty(text(root, "description"), ""))
      .thumbnailUrl(thumbnail)
      .platform(platform)
      .build();
  }

  /**
   * 플랫폼 공용: URL에서 mp4 내려받고 ffmpeg로 wav 변환 (인스타 호환)
   * 반환값: wav 파일 (호출측에서 사용 후 삭제 권장)
   */
  public File downloadAudioAsWav(String url, String baseName, Platform platform)
    throws IOException, InterruptedException {

    String mp4Path = DOWNLOAD_DIR + "/" + baseName + ".mp4";
    String wavPath = DOWNLOAD_DIR + "/" + baseName + ".wav";

    // 1) mp4 다운로드
    List<String> dl = new ArrayList<>();
    dl.add(YT_DLP_PATH);
    // 인스타 쿠키 필요 시
    if (platform == Platform.INSTAGRAM && COOKIES_PATH != null && !COOKIES_PATH.isBlank()) {
      dl.add("--cookies");
      dl.add(COOKIES_PATH);
    }
    dl.add("-o");
    dl.add(mp4Path);
    dl.add(url);

    Process p1 = new ProcessBuilder(dl).redirectErrorStream(true).start();
    // 로그 흡수
    try (BufferedReader br = new BufferedReader(new InputStreamReader(p1.getInputStream()))) {
      while (br.readLine() != null) {
      }
    }
    int exit1 = p1.waitFor();
    if (exit1 != 0) {
      throw new IOException("yt-dlp mp4 download failed, exit=" + exit1);
    }

    // 2) ffmpeg로 wav 변환 (16kHz, mono)
    List<String> ff = List.of(
      FFMPEG_PATH, "-y", "-i", mp4Path,
      "-ac", "1", "-ar", "16000", wavPath
    );
    Process p2 = new ProcessBuilder(ff).redirectErrorStream(true).start();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(p2.getInputStream()))) {
      while (br.readLine() != null) {
      }
    }
    int exit2 = p2.waitFor();
    if (exit2 != 0) {
      throw new IOException("ffmpeg transcode failed, exit=" + exit2);
    }

    // mp4는 용량 절약을 위해 즉시 삭제
    try {
      Files.deleteIfExists(Paths.get(mp4Path));
    } catch (Exception ignore) {
    }

    return new File(wavPath);
  }

  /**
   * 플랫폼 공용: 메타데이터 → 오디오(wav) → 바이트 읽기 → 파일 정리까지 한 번에
   * - YouTube: 기존 로직도 가능하나, 공용화 위해 동일 파이프라인 사용 권장
   * - Instagram: mp4→ffmpeg 필수
   */
  public ExtractResult extractAllForPlatform(String url, Platform platform)
    throws IOException, InterruptedException {

    long start = System.currentTimeMillis();
    String baseName = "media_" + System.currentTimeMillis();
    byte[] audioBytes = null;
    VideoMetadataDto metadata = null;
    File wavFile = null;

    try {
      // 1) 메타데이터
      metadata = extractMetadataOnly(url, platform);

      // 2) 오디오 wav
      wavFile = downloadAudioAsWav(url, baseName, platform);

      // 3) 바이트 로드
      try (FileInputStream fis = new FileInputStream(wavFile);
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        byte[] buf = new byte[8192];
        int len;
        while ((len = fis.read(buf)) != -1) {
          baos.write(buf, 0, len);
        }
        audioBytes = baos.toByteArray();
      }

      log.info("extractAllForPlatform 완료: platform={}, ms={}, wavKB={}",
        platform, (System.currentTimeMillis() - start),
        (audioBytes != null ? audioBytes.length / 1024 : -1));

      return new ExtractResult(audioBytes, metadata);

    } finally {
      // 임시 파일 정리
      if (wavFile != null && wavFile.exists()) {
        try {
          Files.deleteIfExists(wavFile.toPath());
        } catch (Exception ignore) {
        }
      }
    }
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

