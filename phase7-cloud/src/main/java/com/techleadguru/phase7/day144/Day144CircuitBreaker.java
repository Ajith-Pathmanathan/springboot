package com.techleadguru.phase7.day144;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Day 144 — Circuit Breaker with Resilience4j
 *
 * States: CLOSED → OPEN (after threshold failures) → HALF_OPEN → CLOSED (success)
 *                                                            ↘ OPEN   (failure)
 *
 * Sliding window: COUNT_BASED or TIME_BASED.
 * Failure rate threshold: percentage of calls in the window that failed.
 */
public class Day144CircuitBreaker {

    // ─────────────────────────────────────────────────────────────────────────
    // State machine
    // ─────────────────────────────────────────────────────────────────────────

    public enum CircuitState { CLOSED, OPEN, HALF_OPEN }

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────

    public record CircuitBreakerConfig(
            double failureRateThreshold,         // e.g. 50.0 → 50% failures triggers OPEN
            long   waitDurationMs,               // time to stay OPEN before HALF_OPEN
            int    permittedCallsInHalfOpen,     // calls allowed in HALF_OPEN state
            int    slidingWindowSize) {

        public static CircuitBreakerConfig defaultConfig() {
            return new CircuitBreakerConfig(50.0, 5_000L, 3, 10);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pure-Java circuit breaker simulator
    // ─────────────────────────────────────────────────────────────────────────

    public static class CircuitBreakerSimulator {

        private final CircuitBreakerConfig config;
        private final Deque<Boolean>       window    = new ArrayDeque<>(); // true=success
        private volatile CircuitState      state     = CircuitState.CLOSED;
        private volatile long              openedAtMs;
        private final AtomicLong           callCount = new AtomicLong(0);

        public CircuitBreakerSimulator(CircuitBreakerConfig config) {
            this.config = config;
        }

        /**
         * Record a call result.
         * Returns the state AFTER processing this result.
         */
        public CircuitState recordCall(boolean success) {
            callCount.incrementAndGet();
            maybeTransitionFromOpen();

            if (state == CircuitState.OPEN) {
                return CircuitState.OPEN; // still blocked
            }

            // Record result in sliding window
            window.addLast(success);
            while (window.size() > config.slidingWindowSize()) {
                window.pollFirst();
            }

            // Evaluate after window is full
            if (window.size() == config.slidingWindowSize()) {
                double failureRate = failureRate();
                if (failureRate >= config.failureRateThreshold()) {
                    state     = CircuitState.OPEN;
                    openedAtMs = System.currentTimeMillis();
                    window.clear();
                }
            }

            return state;
        }

        /** Returns whether the circuit is currently allowing calls. */
        public boolean isCallAllowed() {
            maybeTransitionFromOpen();
            return state != CircuitState.OPEN;
        }

        public CircuitState getState() {
            maybeTransitionFromOpen();
            return state;
        }

        private void maybeTransitionFromOpen() {
            if (state == CircuitState.OPEN) {
                long elapsed = System.currentTimeMillis() - openedAtMs;
                if (elapsed >= config.waitDurationMs()) {
                    state = CircuitState.HALF_OPEN;
                }
            }
        }

        private double failureRate() {
            long failures = window.stream().filter(s -> !s).count();
            return (double) failures / window.size() * 100.0;
        }

        public void reset() {
            state = CircuitState.CLOSED;
            window.clear();
            callCount.set(0);
        }

        public long callCount() { return callCount.get(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State transitions guide
    // ─────────────────────────────────────────────────────────────────────────

    public record StateTransition(CircuitState from, CircuitState to, String trigger) {}

    public static List<StateTransition> stateTransitions() {
        return List.of(
            new StateTransition(CircuitState.CLOSED,    CircuitState.OPEN,
                "Failure rate >= failureRateThreshold over last slidingWindowSize calls"),
            new StateTransition(CircuitState.OPEN,      CircuitState.HALF_OPEN,
                "waitDuration elapsed since entering OPEN state"),
            new StateTransition(CircuitState.HALF_OPEN, CircuitState.CLOSED,
                "All permittedCallsInHalfOpen succeed"),
            new StateTransition(CircuitState.HALF_OPEN, CircuitState.OPEN,
                "Any call fails during HALF_OPEN probe")
        );
    }
}
