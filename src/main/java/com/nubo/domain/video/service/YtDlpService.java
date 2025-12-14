package com.nubo.domain.video.service;

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
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

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
  //  @Value("${ext.cookies.path:}") // 인스타 쿠키 필요 시 설정
  //  private String COOKIES_PATH;
  @Value("${YTDLP_COOKIES:}") // 1. Fly Secret 환경 변수를 통째로 받음
  private String COOKIES_SECRET_CONTENT;

  @Value("${YTDLP_COOKIES_BASE64:}")
  private String COOKIES_SECRET_BASE64;

  private String ACTUAL_COOKIES_PATH = null; // 2. 실제 사용될 임시 파일 경로

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

  @PostConstruct
  public void initCookiesFromSecret() {

    String cookiesContent = null;

    if (COOKIES_SECRET_BASE64 != null && !COOKIES_SECRET_BASE64.isBlank()) {
      try {
        byte[] decodedBytes = java.util.Base64.getDecoder()
          .decode(COOKIES_SECRET_BASE64.trim());
        cookiesContent = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
        log.info("✅ Cookies decoded from Base64. Length: {} bytes", cookiesContent.length());
      } catch (IllegalArgumentException e) {
        log.error("❌ Failed to decode Base64 cookies", e);
        return;
      }
    }
    // Plain text fallback
    else if (COOKIES_SECRET_CONTENT != null && !COOKIES_SECRET_CONTENT.isBlank()) {
      cookiesContent = COOKIES_SECRET_CONTENT.trim();
      log.info("⚠️ Using plain text cookies. Length: {} bytes", cookiesContent.length());
    }

    if (cookiesContent == null || cookiesContent.isBlank()) {
      log.warn("⚠️ No cookies found in environment variables");
      return;
    }

    // 쿠키 형식 검증
    long validLines = cookiesContent.lines()
      .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
      .count();

    log.info("🔍 Cookie file contains {} valid lines", validLines);

    if (validLines == 0) {
      log.error("❌ No valid cookie lines found!");
      return;
    }

    // 첫 번째 쿠키 라인 검증
    String firstCookie = cookiesContent.lines()
      .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
      .findFirst()
      .orElse("");

    if (!firstCookie.isEmpty()) {
      String[] parts = firstCookie.split("\t");
      log.info("🔍 First cookie has {} tab-separated fields (expected: 7)", parts.length);

      if (parts.length != 7) {
        log.error("❌ Cookie format is INVALID! Got {} fields instead of 7", parts.length);
        log.error("First line preview: {}",
          firstCookie.substring(0, Math.min(150, firstCookie.length())));

        // 공백으로 구분되어 있는지 체크
        String[] spaceParts = firstCookie.split("\\s+");
        if (spaceParts.length > parts.length) {
          log.error("⚠️ Cookie appears to be SPACE-separated instead of TAB-separated!");
        }
        return;
      }

      log.info("✅ Cookie format is valid (7 tab-separated fields)");
    }

    try {
      File tempCookieFile = File.createTempFile("ytdlp_cookies", ".txt",
        new File(DOWNLOAD_DIR));
      Files.writeString(tempCookieFile.toPath(), cookiesContent);

      ACTUAL_COOKIES_PATH = tempCookieFile.getAbsolutePath();
      log.info("✅ Cookies file generated at: {}", ACTUAL_COOKIES_PATH);
      log.info("📦 File size: {} bytes", tempCookieFile.length());

    } catch (IOException e) {
      log.error("❌ Failed to create temporary cookies file", e);
    }

    // // 💡 디버깅 로직 추가: 환경 변수 내용이 주입되었는지 확인
    // if (COOKIES_SECRET_CONTENT == null) {
    //   log.error("DEBUG: COOKIES_SECRET_CONTENT is NULL.");
    // } else if (COOKIES_SECRET_CONTENT.isBlank()) {
    //   log.error("DEBUG: COOKIES_SECRET_CONTENT is BLANK (length 0).");
    // } else {
    //   log.info("DEBUG: COOKIES_SECRET_CONTENT loaded successfully. Length: {} bytes.",
    //     COOKIES_SECRET_CONTENT.length());
    // }

    // if (COOKIES_SECRET_CONTENT != null && !COOKIES_SECRET_CONTENT.isBlank()) {
    //   try {
    //     // 3. 서버 실행 시 /downloads/ 폴더에 임시 파일 생성
    //     File tempCookieFile = File.createTempFile("ytdlp_cookies", ".txt", new File
    //     (DOWNLOAD_DIR));
    //     Files.writeString(tempCookieFile.toPath(), COOKIES_SECRET_CONTENT);

    //     // 4. yt-dlp 명령어에 전달할 실제 경로 설정
    //     ACTUAL_COOKIES_PATH = tempCookieFile.getAbsolutePath();
    //     log.info("Cookies file generated at: {}", ACTUAL_COOKIES_PATH);
    //   } catch (IOException e) {
    //     log.error("Failed to create temporary cookies file from secret", e);
    //   }
    // }
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
    String uniqueName = "shorts_" + UUID.randomUUID().toString();
    String outputBase = DOWNLOAD_DIR + "/" + uniqueName;
    String outputTemplate = outputBase + ".%(ext)s";

    List<String> command = new ArrayList<>();
    command.add(YT_DLP_PATH);

    // 💡 쿠키 경로가 설정되어 있으면 커맨드에 추가합니다.
    if (ACTUAL_COOKIES_PATH != null && !ACTUAL_COOKIES_PATH.isBlank()) {
      command.add("--cookies");
      command.add(ACTUAL_COOKIES_PATH);
    }
    // 2. OAuth2 적용 (서버 IP 차단 시 가장 효과적)
    // command.add("--username");
    // command.add("oauth2");
    // command.add("--password");
    // command.add("");
    // 💡 IP 차단 회피를 위해 사용자 에이전트 추가
    command.add("--user-agent");
    command.add(
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
        + "Chrome/120.0.0.0 Safari/537.36");

    // 1. 클라이언트 위장 (가장 중요)
    // 쿠키가 없는 경우 서버 IP 차단을 피하기 위해 안드로이드 앱으로 위장합니다.
    if (ACTUAL_COOKIES_PATH == null || ACTUAL_COOKIES_PATH.isBlank()) {
      log.info("No cookies available, using android client workaround");
      command.add("--extractor-args");
      command.add("youtube:player_client=android");
    }

    // 2. 프래그먼트 다운로드 안정화
    command.add("--no-part"); // .part 파일 생성 방지 (선택 사항)

    // 3. IPv4 강제 (IPv6 대역이 차단된 경우 유효, 필요시 주석 해제)
    // command.add("--force-ipv4");

    command.add("-f");
    command.add("bestaudio/best");
//    command.add("bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio"); // 더 빠른 포맷 우선
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
      throw new RuntimeException("yt-dlp 통합 추출 실패 (Exit Code: " + exitCode + ")");
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

    // 인스타/틱톡은 쿠키가 있으면 성공률이 올라감 (선택)
    if ((platform == Platform.INSTAGRAM || platform == Platform.TIKTOK)
      && ACTUAL_COOKIES_PATH != null && !ACTUAL_COOKIES_PATH.isBlank()) {
      cmd.add("--cookies");
      cmd.add(ACTUAL_COOKIES_PATH);
    }

    cmd.add(url);

    ProcessBuilder pb = new ProcessBuilder(cmd);
//    pb.redirectErrorStream(true);
    pb.redirectErrorStream(false); // stderr는 따로 두기
    Process proc = pb.start();

    // 💡 stderr 처리 및 로깅을 백그라운드 스레드에서 모두 처리
    new Thread(() -> {
      try (BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
        String errLine;
        while ((errLine = err.readLine()) != null) {
          log.warn("yt-dlp stderr (Background): {}", errLine); // 경고 로그를 여기서 처리
        }
      } catch (IOException e) {
        log.error("stderr 리더 오류", e);
      }
    }).start();

    StringBuilder out = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
      String line;
      while ((line = br.readLine()) != null) {
        out.append(line);
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
    // 인스타/틱톡 쿠키 필요 시
    if ((platform == Platform.INSTAGRAM || platform == Platform.TIKTOK)
      && ACTUAL_COOKIES_PATH != null && !ACTUAL_COOKIES_PATH.isBlank()) {
      dl.add("--cookies");
      dl.add(ACTUAL_COOKIES_PATH);
    }
    dl.add("-f");
//    dl.add("bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio/mp4");
    dl.add("bestaudio/best");

    dl.add("-o");
    dl.add(mp4Path);
    dl.add(url);

    ProcessBuilder pb = new ProcessBuilder(dl);
    pb.redirectErrorStream(false);
    Process p = pb.start();

    // stdout
    try (BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      String line;
      while ((line = out.readLine()) != null) {
        log.info("[yt-dlp out] {}", line);
      }
    }

    // stderr
    try (BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
      String line;
      while ((line = err.readLine()) != null) {
        log.warn("[yt-dlp err] {}", line);
      }
    }

    // yt-dlp 프로세스 대기
    p.waitFor();

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
      log.error("FFmpeg transcode failed with exit code: {}", exit2); // 기존 오류 메시지에 exit code 포함
      log.error("FFMPEG PATH: {}", FFMPEG_PATH); // FFMPEG 경로 추가 로깅
      log.error("FFmpeg command failed: {}", ff); // 실행된 전체 명령어
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
    byte[] audioBytes = null;
    VideoMetadataDto metadata = null;

    // 유튜브는 통합 추출 메서드를 사용 (더 안정적일 가능성 높음)
    if (platform == Platform.YOUTUBE) {
      ExtractResult r = extractAudioAndMetadata(url);
      log.info("YouTube extract 완료, ms={}", (System.currentTimeMillis() - start));
      return r;
    }

    // 나머지 플랫폼 (인스타/틱톡)은 기존 MP4->WAV 파이프라인 유지

    String baseName = "media_" + System.currentTimeMillis() + "_" + UUID.randomUUID();
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

