package com.nubo.domain.card.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
}
