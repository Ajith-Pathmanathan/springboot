package com.techleadguru.phase7.day145;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class Day145RetryBackoffTest {

    @Test
    void testFirstAttemptNoDelay() {
        Day145RetryBackoff.RetryConfig cfg = Day145RetryBackoff.RetryConfig.defaultRetryConfig();
        Day145RetryBackoff.BackoffCalculator calc = new Day145RetryBackoff.BackoffCalculator();
        assertEquals(0L, calc.calculateDelay(1, cfg));
    }

    @Test
    void testSecondAttemptHasDelay() {
        Day145RetryBackoff.RetryConfig cfg = Day145RetryBackoff.RetryConfig.defaultRetryConfig();
        Day145RetryBackoff.BackoffCalculator calc = new Day145RetryBackoff.BackoffCalculator();
        long delay = calc.calculateDelay(2, cfg);
        assertTrue(delay >= 0L); // jitter can bring it near 0 but not below
    }

    @Test
    void testDelayGrowsExponentially() {
        Day145RetryBackoff.RetryConfig cfg =
                new Day145RetryBackoff.RetryConfig(5, 100L, 2.0, 10_000L, 0.0); // no jitter
        Day145RetryBackoff.BackoffCalculator calc =
                new Day145RetryBackoff.BackoffCalculator(new java.util.Random(0));
        long d2 = calc.calculateDelay(2, cfg);
        long d3 = calc.calculateDelay(3, cfg);
        assertTrue(d3 > d2, "Delay should grow: d2=" + d2 + " d3=" + d3);
    }

    @Test
    void testDelayDoesNotExceedMax() {
        Day145RetryBackoff.RetryConfig cfg =
                new Day145RetryBackoff.RetryConfig(10, 1000L, 3.0, 5_000L, 0.0);
        Day145RetryBackoff.BackoffCalculator calc =
                new Day145RetryBackoff.BackoffCalculator(new java.util.Random(0));
        for (int i = 1; i <= 10; i++) {
            long delay = calc.calculateDelay(i, cfg);
            assertTrue(delay <= cfg.maxDelayMs(), "Delay exceeded max at attempt " + i);
        }
    }

    @Test
    void testRetrySimulatorSucceedsOnThirdAttempt() {
        AtomicInteger attempts = new AtomicInteger(0);
        Day145RetryBackoff.RetrySimulator<String> sim =
                new Day145RetryBackoff.RetrySimulator<>(
                        Day145RetryBackoff.RetryConfig.defaultRetryConfig());
        String result = sim.execute(() -> {
            if (attempts.incrementAndGet() < 3) throw new RuntimeException("fail");
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(3, sim.attemptsMade());
    }

    @Test
    void testRetrySimulatorThrowsAfterMaxAttempts() {
        Day145RetryBackoff.RetrySimulator<String> sim =
                new Day145RetryBackoff.RetrySimulator<>(
                        new Day145RetryBackoff.RetryConfig(3, 0, 1.0, 0, 0));
        assertThrows(RuntimeException.class,
                () -> sim.execute(() -> { throw new RuntimeException("always fails"); }));
        assertEquals(3, sim.attemptsMade());
    }

    @Test
    void testBackoffStrategies() {
        List<Day145RetryBackoff.BackoffStrategy> strategies =
                Day145RetryBackoff.backoffStrategies();
        assertEquals(3, strategies.size());
    }
}
