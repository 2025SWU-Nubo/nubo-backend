package com.nubo.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "recommendationExecutor")
  public Executor recommendationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);         // 동시에 처리 가능한 비동기 작업의 기본 개수
    executor.setMaxPoolSize(20);          // 동시에 처리 가능한 최대 동시 작업 수
    executor.setQueueCapacity(100);       // 대기열 크기
    executor.setThreadNamePrefix("rec-"); // 실행되는 스레드 이름
    executor.setWaitForTasksToCompleteOnShutdown(true); // 애플리케이션 종료 시, 큐에 남은 작업이 다 끝날 때까지 기다려줌
    executor.setAwaitTerminationSeconds(60);            // 최대 60초까지만 기다려줌 (무한 대기 방지)
    executor.initialize();
    return executor;
  }
}
