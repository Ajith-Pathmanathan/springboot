package com.techleadguru.phase7.day144;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day144CircuitBreakerTest {

    private Day144CircuitBreaker.CircuitBreakerConfig config() {
        return new Day144CircuitBreaker.CircuitBreakerConfig(50.0, 60_000L, 3, 10);
    }

    @Test
    void testInitialStateIsClosed() {
        Day144CircuitBreaker.CircuitBreakerSimulator cb =
                new Day144CircuitBreaker.CircuitBreakerSimulator(config());
        assertEquals(Day144CircuitBreaker.CircuitState.CLOSED, cb.getState());
    }

    @Test
    void testOpensAfterFailureThreshold() {
        Day144CircuitBreaker.CircuitBreakerSimulator cb =
                new Day144CircuitBreaker.CircuitBreakerSimulator(config());
        // Fill 10-call window with 6 failures (60% ≥ 50% threshold)
        for (int i = 0; i < 4; i++) cb.recordCall(true);
        for (int i = 0; i < 6; i++) cb.recordCall(false);
        assertEquals(Day144CircuitBreaker.CircuitState.OPEN, cb.getState());
    }

    @Test
    void testRemainsClosedBelowThreshold() {
        Day144CircuitBreaker.CircuitBreakerSimulator cb =
                new Day144CircuitBreaker.CircuitBreakerSimulator(config());
        // 4 failures in 10 calls = 40% < 50% threshold
        for (int i = 0; i < 6; i++) cb.recordCall(true);
        for (int i = 0; i < 4; i++) cb.recordCall(false);
        assertEquals(Day144CircuitBreaker.CircuitState.CLOSED, cb.getState());
    }

    @Test
    void testBlocksCallsWhenOpen() {
        Day144CircuitBreaker.CircuitBreakerSimulator cb =
                new Day144CircuitBreaker.CircuitBreakerSimulator(config());
        for (int i = 0; i < 4; i++) cb.recordCall(true);
        for (int i = 0; i < 6; i++) cb.recordCall(false);
        assertFalse(cb.isCallAllowed());
    }

    @Test
    void testResetRestoresClosed() {
        Day144CircuitBreaker.CircuitBreakerSimulator cb =
                new Day144CircuitBreaker.CircuitBreakerSimulator(config());
        for (int i = 0; i < 4; i++) cb.recordCall(true);
        for (int i = 0; i < 6; i++) cb.recordCall(false);
        cb.reset();
        assertEquals(Day144CircuitBreaker.CircuitState.CLOSED, cb.getState());
        assertTrue(cb.isCallAllowed());
    }

    @Test
    void testStateTransitions() {
        List<Day144CircuitBreaker.StateTransition> transitions =
                Day144CircuitBreaker.stateTransitions();
        assertEquals(4, transitions.size());
    }

    @Test
    void testDefaultConfig() {
        Day144CircuitBreaker.CircuitBreakerConfig cfg =
                Day144CircuitBreaker.CircuitBreakerConfig.defaultConfig();
        assertEquals(50.0, cfg.failureRateThreshold(), 1e-9);
        assertEquals(10,   cfg.slidingWindowSize());
    }
}
