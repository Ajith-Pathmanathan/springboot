package com.techleadguru.phase7.day147;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day147RateLimiterTest {

    @Test
    void testSlidingWindowAllowsWithinLimit() {
        Day147RateLimiter.SlidingWindowRateLimiter limiter =
                new Day147RateLimiter.SlidingWindowRateLimiter(5, 1_000_000_000L);
        long now = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire(now + i));
        }
    }

    @Test
    void testSlidingWindowRejectsOverLimit() {
        Day147RateLimiter.SlidingWindowRateLimiter limiter =
                new Day147RateLimiter.SlidingWindowRateLimiter(3, 1_000_000_000L);
        long now = System.nanoTime();
        for (int i = 0; i < 3; i++) limiter.tryAcquire(now + i);
        assertFalse(limiter.tryAcquire(now + 3));
        assertEquals(1, limiter.rejectedCount());
    }

    @Test
    void testSlidingWindowEvictsOldEntries() {
        // Window = 100ms = 100_000_000 ns
        Day147RateLimiter.SlidingWindowRateLimiter limiter =
                new Day147RateLimiter.SlidingWindowRateLimiter(2, 100_000_000L);
        long t0 = 0L;
        limiter.tryAcquire(t0);
        limiter.tryAcquire(t0 + 1);
        // Both slots used; advance time past window
        long t1 = 200_000_000L;
        assertTrue(limiter.tryAcquire(t1)); // old entries evicted
    }

    @Test
    void testTokenBucketAllowsUpToBurst() {
        Day147RateLimiter.TokenBucketRateLimiter limiter =
                new Day147RateLimiter.TokenBucketRateLimiter(10, 5);
        long now = System.nanoTime();
        int allowed = 0;
        for (int i = 0; i < 10; i++) {
            if (limiter.tryAcquire(now + i)) allowed++;
        }
        assertEquals(5, allowed);
    }

    @Test
    void testTokenBucketRefills() {
        // 1 token/second, burst=1; consume 1 token, advance ~2 seconds
        Day147RateLimiter.TokenBucketRateLimiter limiter =
                new Day147RateLimiter.TokenBucketRateLimiter(1, 1);
        long now = 0L;
        assertTrue(limiter.tryAcquire(now));
        assertFalse(limiter.tryAcquire(now + 1)); // bucket empty
        // Advance 2 seconds (2_000_000_000 ns) — should refill
        assertTrue(limiter.tryAcquire(now + 2_000_000_001L));
    }

    @Test
    void testRateLimiterVsCircuitBreakerComparison() {
        List<Day147RateLimiter.PatternComparison> comparisons =
                Day147RateLimiter.rateLimiterVsCircuitBreaker();
        assertEquals(5, comparisons.size());
    }

    @Test
    void testDefaultConfig() {
        Day147RateLimiter.RateLimiterConfig cfg =
                Day147RateLimiter.RateLimiterConfig.defaultConfig();
        assertEquals(10, cfg.limitForPeriod());
        assertEquals(1_000_000_000L, cfg.refreshPeriodNanos());
    }
}
