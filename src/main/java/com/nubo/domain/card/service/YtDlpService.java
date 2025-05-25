package com.nubo.domain.card.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nubo.domain.video.VideoMetadataDto;
import com.nubo.domain.video.type.Platform;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class YtDlpService {

  /**
   * YouTube URL에서 오디오 추출 후 바이트 배열로 반환
   *
   * @param url YouTube 영상 URL
   * @return .wav 오디오의 byte[]
   */
  public byte[] extractAudioBytes(String url) throws IOException, InterruptedException {
    String uniqueName = "shorts_audio_" + System.currentTimeMillis();
    String outputBase = "downloads/" + uniqueName;
    String outputTemplate = outputBase + ".%(ext)s";

    List<String> command = new ArrayList<>();
    command.add("C:\\Users\\user\\whisper-test\\venv\\Scripts\\yt-dlp.exe");
    command.add("-f");
    command.add("bestaudio");
    command.add("--extract-audio");
    command.add("--audio-format");
    command.add("wav");
    command.add("--write-auto-sub");
    command.add("--sub-lang");
    command.add("ko,en");
    command.add("-o");
    command.add(outputTemplate);
    command.add(url);

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.inheritIO(); // 콘솔 출력 확인용
    Process process = pb.start();
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      throw new RuntimeException("yt-dlp 실행 실패");
    }

    // 실제로 생성된 파일 경로
    File wavFile = new File(outputBase + ".wav");

    // wav 파일을 byte[]로 읽기
    try (FileInputStream fis = new FileInputStream(wavFile);
      ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      byte[] buffer = new byte[4096];
      int len;
      while ((len = fis.read(buffer)) != -1) {
        baos.write(buffer, 0, len);
      }

      // wav 파일 삭제 (메모리 반환만 하고 저장 안함)
      wavFile.delete();

      return baos.toByteArray();
    }
  }

  public VideoMetadataDto extractMetadata(String videoUrl)
    throws IOException, InterruptedException {
    String uniqueName = "shorts_meta_" + System.currentTimeMillis();
    String outputPath = "downloads/" + uniqueName + ".info.json";

    List<String> command = List.of(
      "C:\\Users\\user\\whisper-test\\venv\\Scripts\\yt-dlp.exe",
      "--skip-download",
      "--write-info-json",
      "-o", "downloads/" + uniqueName + ".%(ext)s",
      videoUrl
    );

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.inheritIO();
    Process process = pb.start();
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      throw new RuntimeException("yt-dlp 메타데이터 추출 실패");
    }

    // JSON 읽기
    File jsonFile = new File(outputPath);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode info = mapper.readTree(jsonFile);

    // 필요한 필드 추출
    String videoId = info.get("id").asText();
    String title = info.get("title").asText("");
    String description = info.get("description").asText("");
    String thumbnail = info.get("thumbnail").asText("");

    // 다운로드 후 json 파일은 삭제해도 됨
    jsonFile.delete();

    return VideoMetadataDto.builder()
      .videoId(videoId)
      .videoUrl(videoUrl)
      .title(title)
      .description(description)
      .thumbnailUrl(thumbnail)
      .platform(Platform.YOUTUBE) // 필요 시 platform 추출 추가
      .build();
  }

  public String extractVideoIdOnly(String videoUrl) throws IOException, InterruptedException {
    List<String> command = List.of(
      "C:\\Users\\user\\whisper-test\\venv\\Scripts\\yt-dlp.exe",
      "--get-id",
      videoUrl
    );

    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    Process process = pb.start();

    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    String videoId = reader.readLine();
    process.waitFor();

    return videoId;
  }

}
