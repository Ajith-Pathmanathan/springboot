package com.techleadguru.phase4.day64;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 64 — fixedRate vs fixedDelay vs cron: The Scheduling Modes
 *
 * THREE SCHEDULING MODES:
 *
 *   fixedRate = 1000ms
 *     Fires exactly every 1000ms from the PREVIOUS START time.
 *     If execution takes 1500ms, next fires immediately after it finishes (no overlap by default).
 *     ← Good for: heartbeat, health checks, regular data sync at fixed intervals
 *     ← Risk: if execution is slow, tasks stack up (see Day 65)
 *
 *   fixedDelay = 1000ms
 *     Fires 1000ms after the PREVIOUS COMPLETION.
 *     If execution takes 1500ms, next fires at 1500ms + 1000ms = 2500ms from start.
 *     ← Good for: cleanup tasks, polling where you want breathing room after each run
 *     ← Safer: natural backpressure — if slow, subsequent runs are automatically delayed
 *
 *   cron = "0 0 3 * * *"
 *     Calendar-based scheduling (Day 70 for mastery).
 *     6 fields: second minute hour dayOfMonth month dayOfWeek
 *     ← Good for: "run every day at 3am", "every Monday at 9am"
 *     ← More expressive than fixed intervals
 *
 * THREAD MODEL:
 *   By default, all @Scheduled tasks share a SINGLE-THREAD scheduler.
 *   spring.task.scheduling.pool.size=1 (default)
 *   → If one task is slow, ALL others are blocked!
 *   → For production: increase pool size or use separate per-task schedulers.
 *
 *   spring.task.scheduling.pool.size=10  ← allows 10 concurrent scheduled tasks
 *
 * INITIAL DELAY:
 *   @Scheduled(fixedDelay = 1000, initialDelay = 5000)
 *   → Don't fire for first 5 seconds after startup (useful for warmup).
 *
 * STRING-BASED PROPERTIES (Spring EL):
 *   @Scheduled(fixedDelayString = "${app.cleanup.interval-ms:60000}")
 *   → Allows configuration without recompiling; with default fallback.
 */
@Slf4j
public class Day64ScheduledInternals {

    @Component
    @Slf4j
    public static class ScheduledTasksDemo {

        private final AtomicInteger fixedRateCount = new AtomicInteger();
        private final AtomicInteger fixedDelayCount = new AtomicInteger();
        private final AtomicInteger cronCount = new AtomicInteger();

        /**
         * fixedRate: fires every 5min from previous START.
         * Use a long interval so tests are not affected by accidental fires.
         */
        @Scheduled(fixedRate = 300_000, initialDelay = 300_000)
        public void fixedRateTask() {
            int n = fixedRateCount.incrementAndGet();
            log.info("[Day64] fixedRate #{} at {} on thread {}",
                    n, LocalDateTime.now(), Thread.currentThread().getName());
        }

        /**
         * fixedDelay: fires 5min after previous COMPLETION.
         */
        @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
        public void fixedDelayTask() {
            int n = fixedDelayCount.incrementAndGet();
            log.info("[Day64] fixedDelay #{} at {} on thread {}",
                    n, LocalDateTime.now(), Thread.currentThread().getName());
        }

        /**
         * cron: fires at :00 seconds of every minute (but only if explicitly invoked in tests).
         * Effectively disabled in tests because test runs don't last 60s.
         */
        @Scheduled(cron = "${phase4.day64.cron:0 0/30 * * * *}") // every 30 min by default
        public void cronTask() {
            int n = cronCount.incrementAndGet();
            log.info("[Day64] cron #{} at {} on thread {}",
                    n, LocalDateTime.now(), Thread.currentThread().getName());
        }

        // Expose counters for testing
        public int getFixedRateCount() { return fixedRateCount.get(); }
        public int getFixedDelayCount() { return fixedDelayCount.get(); }
        public int getCronCount() { return cronCount.get(); }

        // Publicly callable version for tests (no @Scheduled, just the logic)
        public void runFixedRateManually() { fixedRateTask(); }
        public void runFixedDelayManually() { fixedDelayTask(); }
        public void runCronManually() { cronTask(); }
    }
}
