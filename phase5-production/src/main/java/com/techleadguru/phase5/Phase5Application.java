package com.techleadguru.phase5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Phase 5 — Production Problem Solving (Days 78–105)
 *
 * Topics:
 *   Memory leaks: heap, static fields, ThreadLocal, unbounded caches (Days 78-84)
 *   Stuck threads: pool exhaustion, no timeouts, OpenSessionInView, graceful shutdown (Days 85-91)
 *   GC tuning: G1GC, ZGC, off-heap, profiling, JMH benchmarks (Days 92-98)
 *   Actuator, health probes, Redisson locks, feature flags (Days 99-105)
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class Phase5Application {
    public static void main(String[] args) {
        SpringApplication.run(Phase5Application.class, args);
    }
}
