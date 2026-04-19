package com.techleadguru.phase4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Phase 4 — Async, Scheduling & Concurrency (Days 57–77)
 *
 * Topics:
 *   @Async internals, ThreadPoolTaskExecutor, CompletableFuture, virtual threads (Days 57-63)
 *   @Scheduled, ShedLock, Quartz, dynamic scheduling (Days 64-70)
 *   Deadlock, livelock, ConcurrentHashMap, BlockingQueue (Days 71-77)
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Phase4Application {
    public static void main(String[] args) {
        SpringApplication.run(Phase4Application.class, args);
    }
}
