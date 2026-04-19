package com.techleadguru.phase7.day146;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day 146 — Bulkhead pattern with Resilience4j
 *
 * Bulkhead isolates resources so that a slow downstream cannot exhaust all
 * threads/connections and bring down the entire service.
 *
 * Two Resilience4j bulkhead types:
 *   1. SemaphoreBulkhead  — limits concurrent calls (no queuing)
 *   2. ThreadPoolBulkhead — each service gets a dedicated thread pool
 */
public class Day146Bulkhead {

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────

    public record BulkheadConfig(
            int maxConcurrentCalls,   // semaphore permits
            int maxWaitDurationMs) {} // max wait for a permit (0 = fail fast)

    public record ThreadPoolBulkheadConfig(
            int coreSize,
            int maxSize,
            int queueCapacity,
            long keepAliveMs) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Semaphore-based bulkhead simulator
    // ─────────────────────────────────────────────────────────────────────────

    public static class BulkheadSimulator {

        private final BulkheadConfig config;
        private final Semaphore semaphore;
        private final AtomicInteger blockedCount  = new AtomicInteger(0);
        private final AtomicInteger rejectedCount = new AtomicInteger(0);

        public BulkheadSimulator(BulkheadConfig config) {
            this.config    = config;
            this.semaphore = new Semaphore(config.maxConcurrentCalls());
        }

        /**
         * Attempt to acquire a permit.
         * Returns true if acquired within maxWaitDurationMs, false if rejected.
         */
        public boolean acquire() throws InterruptedException {
            if (config.maxWaitDurationMs() == 0) {
                boolean acquired = semaphore.tryAcquire();
                if (!acquired) rejectedCount.incrementAndGet();
                return acquired;
            }
            blockedCount.incrementAndGet();
            try {
                boolean acquired = semaphore.tryAcquire(config.maxWaitDurationMs(),
                        TimeUnit.MILLISECONDS);
                if (!acquired) rejectedCount.incrementAndGet();
                return acquired;
            } finally {
                blockedCount.decrementAndGet();
            }
        }

        /** Release a permit back to the bulkhead. */
        public void release() {
            semaphore.release();
        }

        public int availableSlots()  { return semaphore.availablePermits(); }
        public int blockedCount()    { return blockedCount.get(); }
        public int rejectedCount()   { return rejectedCount.get(); }
        public int maxConcurrent()   { return config.maxConcurrentCalls(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comparison: Semaphore vs Thread-pool bulkhead
    // ─────────────────────────────────────────────────────────────────────────

    public record BulkheadComparison(
            String aspect,
            String semaphore,
            String threadPool) {}

    public static List<BulkheadComparison> bulkheadVsThreadPool() {
        return List.of(
            new BulkheadComparison("Resource type",
                "Semaphore permits (no thread switch)",
                "Dedicated thread pool per downstream"),
            new BulkheadComparison("Execution thread",
                "Caller's own thread; non-blocking callers work well",
                "Offloads to pool thread; caller thread released immediately"),
            new BulkheadComparison("Overhead",
                "Low — no context switching",
                "Higher — thread creation and management"),
            new BulkheadComparison("Timeout support",
                "Limited — only wait-for-permit timeout",
                "Full task timeout via future cancellation"),
            new BulkheadComparison("Best for",
                "Reactive/non-blocking services; low-latency paths",
                "Synchronous blocking I/O; isolating slow external calls")
        );
    }
}
