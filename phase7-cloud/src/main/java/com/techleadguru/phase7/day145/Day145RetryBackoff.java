package com.techleadguru.phase7.day145;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Day 145 — Retry with exponential backoff
 *
 * Resilience4j Retry: max attempts, wait duration, exponential backoff, jitter.
 *
 * Backoff formula (exponential with jitter):
 *   delay(attempt) = min(maxDelay, initialDelay * multiplier^(attempt-1)) ± jitter
 */
public class Day145RetryBackoff {

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────

    public record RetryConfig(
            int    maxAttempts,
            long   initialDelayMs,
            double multiplier,
            long   maxDelayMs,
            double jitterFactor) {

        public static RetryConfig defaultRetryConfig() {
            return new RetryConfig(3, 500L, 2.0, 10_000L, 0.1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Backoff calculator
    // ─────────────────────────────────────────────────────────────────────────

    public static class BackoffCalculator {

        private final Random rng;

        public BackoffCalculator()              { this.rng = new Random(); }
        public BackoffCalculator(Random random) { this.rng = random; }

        /**
         * Calculate wait time before the given attempt (1-based).
         * attempt=1 waits initialDelay.
         * attempt=2 waits initialDelay * multiplier.
         * etc.
         */
        public long calculateDelay(int attempt, RetryConfig config) {
            if (attempt <= 1) return 0L; // first attempt — no wait
            int waitAttempt = attempt - 1;
            double raw = config.initialDelayMs()
                    * Math.pow(config.multiplier(), waitAttempt - 1);
            long capped = Math.min((long) raw, config.maxDelayMs());
            // add random jitter ±jitterFactor
            double jitter = (rng.nextDouble() * 2 - 1) * config.jitterFactor() * capped;
            return Math.max(0L, capped + (long) jitter);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Retry simulator (no real Thread.sleep; records delays instead)
    // ─────────────────────────────────────────────────────────────────────────

    public static class RetrySimulator<T> {

        private final RetryConfig       config;
        private final BackoffCalculator backoff;
        private final AtomicInteger     attempts = new AtomicInteger(0);
        private final List<Long>        delays   = new ArrayList<>();

        public RetrySimulator(RetryConfig config) {
            this.config  = config;
            this.backoff = new BackoffCalculator(new Random(42)); // deterministic
        }

        /**
         * Executes supplier, retrying on RuntimeException up to maxAttempts.
         * Returns the result or throws the last exception.
         */
        public T execute(Supplier<T> supplier) {
            attempts.set(0);
            delays.clear();
            RuntimeException last = null;
            for (int i = 1; i <= config.maxAttempts(); i++) {
                attempts.set(i);
                long delay = backoff.calculateDelay(i, config);
                delays.add(delay);
                try {
                    return supplier.get();
                } catch (RuntimeException ex) {
                    last = ex;
                }
            }
            throw Objects.requireNonNull(last);
        }

        public int attemptsMade() { return attempts.get(); }
        public List<Long> recordedDelays() { return Collections.unmodifiableList(delays); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comparison: retry strategies
    // ─────────────────────────────────────────────────────────────────────────

    public record BackoffStrategy(String name, String formula, String bestFor) {}

    public static List<BackoffStrategy> backoffStrategies() {
        return List.of(
            new BackoffStrategy("Fixed delay",
                "delay = initialDelay (constant)",
                "Predictable retry cadence; transient network blips"),
            new BackoffStrategy("Exponential backoff",
                "delay = initialDelay * multiplier^(attempt-1)",
                "Reducing load on an overwhelmed downstream"),
            new BackoffStrategy("Exponential backoff + jitter",
                "delay = exponential ± (rand * jitterFactor * delay)",
                "Prevents thundering herd when many instances retry simultaneously")
        );
    }
}
