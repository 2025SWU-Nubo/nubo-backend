package com.nubo.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {

  @Value("${firebase.config.path:}")
  private String firebaseConfigPath;

  @Value("${firebase.config.json:}")
  private String firebaseConfigJson;

  @Bean
  public FirebaseApp firebaseApp() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
      FirebaseOptions options;

      // JSON 환경변수(FIREBASE_CONFIG_JSON)가 설정된 경우
      if (firebaseConfigJson != null && !firebaseConfigJson.isEmpty()) {
        options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(
            new ByteArrayInputStream(firebaseConfigJson.getBytes())
          ))
          .build();
      }
      // 로컬 개발환경 (파일 경로로 읽기)
      else if (firebaseConfigPath != null && !firebaseConfigPath.isEmpty() && new File(
        firebaseConfigPath).exists()) {
        FileInputStream serviceAccount = new FileInputStream(firebaseConfigPath);
        options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(serviceAccount))
          .build();
      }
      // 둘 다 없으면 예외 처리
      else {
        throw new IllegalStateException(
          "Firebase configuration not found. Provide either firebase.config.json or firebase"
            + ".config.path");
      }

      return FirebaseApp.initializeApp(options);
    } else {
      return FirebaseApp.getInstance();
    }
  }
}
