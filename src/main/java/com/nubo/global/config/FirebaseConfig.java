package com.nubo.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {

  @Value("${firebase.config.path}")
  private String firebaseConfigPath;

  @Bean
  public FirebaseApp firebaseApp() throws IOException {
    FileInputStream serviceAccount = new FileInputStream(firebaseConfigPath);

    FirebaseOptions options = FirebaseOptions.builder()
      .setCredentials(GoogleCredentials.fromStream(serviceAccount))
      .build();

    return FirebaseApp.initializeApp(options);
  }
}
