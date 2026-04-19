package com.techleadguru.phase7.day147;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Day 147 — Rate Limiter with Resilience4j
 *
 * RateLimiter controls how many calls a service can make in a given period —
 * protecting downstream services from being overwhelmed.
 *
 * Two algorithms:
 *   1. AtomicRateLimiter (default): refresh-period window, grant up to limitForPeriod calls
 *   2. SemaphoreBasedRateLimiter: semaphore with timeout (legacy)
 *
 * Sliding window rate limiter: uses a circular buffer of timestamps.
 * Token bucket rate limiter: refills tokens at a constant rate.
 */
public class Day147RateLimiter {

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────

    public record RateLimiterConfig(
            int  limitForPeriod,          // allowed calls per period
            long refreshPeriodNanos,      // period length in nanoseconds
            long timeoutMs) {             // how long to wait for permission

        public static RateLimiterConfig defaultConfig() {
            return new RateLimiterConfig(10, 1_000_000_000L, 0L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sliding-window rate limiter
    // ─────────────────────────────────────────────────────────────────────────

    public static class SlidingWindowRateLimiter {

        private final int       limitForPeriod;
        private final long      windowNanos;
        private final Deque<Long> callTimestamps = new ArrayDeque<>();
        private final AtomicLong rejectedCount   = new AtomicLong(0);

        public SlidingWindowRateLimiter(int limitForPeriod, long windowNanos) {
            this.limitForPeriod = limitForPeriod;
            this.windowNanos    = windowNanos;
        }

        public synchronized boolean tryAcquire(long nowNanos) {
            evictOld(nowNanos);
            if (callTimestamps.size() < limitForPeriod) {
                callTimestamps.addLast(nowNanos);
                return true;
            }
            rejectedCount.incrementAndGet();
            return false;
        }

        private void evictOld(long nowNanos) {
            while (!callTimestamps.isEmpty()
                    && nowNanos - callTimestamps.peekFirst() > windowNanos) {
                callTimestamps.pollFirst();
            }
        }

        public int currentCount(long nowNanos) {
            evictOld(nowNanos);
            return callTimestamps.size();
        }

        public long rejectedCount() { return rejectedCount.get(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token-bucket rate limiter
    // ─────────────────────────────────────────────────────────────────────────

    public static class TokenBucketRateLimiter {

        private final double replenishRatePerNano; // tokens per nanosecond
        private final int    burstCapacity;
        private double       tokens;
        private long         lastRefillNanos = -1L; // -1 = not yet initialized
        private final AtomicLong rejectedCount = new AtomicLong(0);

        public TokenBucketRateLimiter(int replenishRatePerSecond, int burstCapacity) {
            this.replenishRatePerNano = (double) replenishRatePerSecond / 1_000_000_000.0;
            this.burstCapacity        = burstCapacity;
            this.tokens               = burstCapacity;
        }

        public synchronized boolean tryAcquire(long nowNanos) {
            refill(nowNanos);
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            rejectedCount.incrementAndGet();
            return false;
        }

        private void refill(long nowNanos) {
            if (lastRefillNanos < 0) {
                lastRefillNanos = nowNanos; // initialise on first call
                return;
            }
            double elapsed = nowNanos - lastRefillNanos;
            if (elapsed > 0) {
                tokens = Math.min(burstCapacity, tokens + elapsed * replenishRatePerNano);
                lastRefillNanos = nowNanos;
            }
        }

        public long rejectedCount() { return rejectedCount.get(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comparison: RateLimiter vs CircuitBreaker
    // ─────────────────────────────────────────────────────────────────────────

    public record PatternComparison(
            String aspect,
            String rateLimiter,
            String circuitBreaker) {}

    public static List<PatternComparison> rateLimiterVsCircuitBreaker() {
        return List.of(
            new PatternComparison("Purpose",
                "Throttle outgoing call rate to protect downstream",
                "Stop calls when downstream is failing to allow recovery"),
            new PatternComparison("Trigger",
                "Exceeding a calls-per-period quota",
                "Failure rate exceeding a threshold"),
            new PatternComparison("Action on trip",
                "Reject or queue excess calls immediately",
                "Short-circuit: fail fast for waitDuration, then probe"),
            new PatternComparison("Focus",
                "Throughput control; SLA enforcement",
                "Fault tolerance; cascading failure prevention"),
            new PatternComparison("Used together",
                "Yes — apply rate limiter first, circuit breaker second",
                "Yes — outer CB, inner rate limiter")
        );
    }
}
