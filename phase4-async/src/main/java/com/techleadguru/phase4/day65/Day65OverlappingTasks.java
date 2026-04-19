package com.techleadguru.phase4.day65;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 65 — Overlapping Scheduled Tasks
 *
 * THE PROBLEM: fixedRate with a slow task
 *   Task duration: 3000ms
 *   fixedRate:     1000ms
 *
 *   Spring's default scheduler is SINGLE-THREADED.
 *   Task 1 fires at T=0, runs 3000ms → completes at T=3000.
 *   Task 2 was scheduled for T=1000, but scheduler is busy → fires at T=3000.
 *   Task 3 was scheduled for T=2000, but scheduler is busy → fires at T=3000.
 *   THREE tasks fire simultaneously at T=3000! That's overlap.
 *
 *   With a multi-thread scheduler (pool.size > 1), tasks CAN overlap:
 *   Task 1 runs T=0→3000ms, task 2 starts at T=1000ms despite task 1 running.
 *   If tasks are NOT idempotent (e.g., deduplication job, counter update), this is a BUG.
 *
 * THREE SOLUTIONS:
 *
 *   1. fixedDelay instead of fixedRate:
 *      → task2 starts fixedDelay ms AFTER task1 finishes — no overlap possible.
 *      → Correct for most cleanup/polling jobs.
 *
 *   2. @Async + guard flag (tryLock pattern):
 *      @Scheduled(fixedRate = 1000)
 *      public void task() {
 *          if (!running.compareAndSet(false, true)) return; // skip if previous still running
 *          try { doWork(); } finally { running.set(false); }
 *      }
 *
 *   3. ShedLock (Day 67):
 *      Distributed lock — only ONE instance across the entire cluster runs the task.
 *      Automatically prevents overlap on both single and multi-node setups.
 */
@Slf4j
public class Day65OverlappingTasks {

    @Component
    @Slf4j
    public static class ReportGenerator {

        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicInteger startCount = new AtomicInteger();
        private final AtomicInteger skipCount = new AtomicInteger();
        private final AtomicInteger completedCount = new AtomicInteger();

        /**
         * BROKEN — can overlap if scheduling pool > 1 or previous run is still active.
         * (interval is very long so this won't interfere with tests)
         */
        @Scheduled(fixedRate = 600_000, initialDelay = 600_000)
        public void generateReportBroken() {
            int n = startCount.incrementAndGet();
            log.warn("[Day65] BROKEN: report run #{} started — could overlap!", n);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            completedCount.incrementAndGet();
        }

        /**
         * FIXED — compareAndSet ensures only one execution at a time.
         * If a previous run is still active, this invocation is simply skipped.
         */
        @Scheduled(fixedRate = 600_000, initialDelay = 600_000)
        public void generateReportFixed() {
            if (!running.compareAndSet(false, true)) {
                skipCount.incrementAndGet();
                log.info("[Day65] FIXED: skipping — previous run still active (skipped={})", skipCount.get());
                return;
            }
            int n = startCount.get();
            try {
                log.info("[Day65] FIXED: starting run #{}", n);
                Thread.sleep(50); // simulate work
                completedCount.incrementAndGet();
                log.info("[Day65] FIXED: completed run #{}", n);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                running.set(false); // ALWAYS release in finally
            }
        }

        // Expose state for tests
        public boolean tryStartManually() {
            return running.compareAndSet(false, true);
        }

        public void finishManually() {
            running.set(false);
            completedCount.incrementAndGet();
        }

        public boolean isRunning() { return running.get(); }
        public int getSkipCount() { return skipCount.get(); }
        public int getCompletedCount() { return completedCount.get(); }
    }
}
