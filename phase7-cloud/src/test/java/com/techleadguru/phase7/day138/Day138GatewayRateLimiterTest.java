package com.techleadguru.phase7.day138;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day138GatewayRateLimiterTest {

    @Test
    void testTokenBucketAllowsRequestsUpToBurst() {
        Day138GatewayRateLimiter.TokenBucket bucket =
                new Day138GatewayRateLimiter.TokenBucket(10, 5, 1);
        // Bucket starts full at burstCapacity=5
        int allowed = 0;
        for (int i = 0; i < 10; i++) {
            if (bucket.tryConsume()) allowed++;
        }
        assertEquals(5, allowed);
    }

    @Test
    void testTokenBucketConfig() {
        Day138GatewayRateLimiter.TokenBucket bucket =
                new Day138GatewayRateLimiter.TokenBucket(10, 20, 1);
        assertEquals(10, bucket.replenishRate());
        assertEquals(20, bucket.burstCapacity());
        assertEquals(1,  bucket.requestedTokens());
    }

    @Test
    void testPerKeyRateLimiterAllowsWithinLimit() {
        Day138GatewayRateLimiter.PerKeyRateLimiter limiter =
                new Day138GatewayRateLimiter.PerKeyRateLimiter(10, 3, 1);
        assertTrue(limiter.isAllowed("userA"));
        assertTrue(limiter.isAllowed("userA"));
        assertTrue(limiter.isAllowed("userA"));
        // 4th burst-exceeds
        assertFalse(limiter.isAllowed("userA"));
    }

    @Test
    void testPerKeyRateLimiterIsolatesKeys() {
        Day138GatewayRateLimiter.PerKeyRateLimiter limiter =
                new Day138GatewayRateLimiter.PerKeyRateLimiter(2, 2, 1);
        limiter.isAllowed("userA");
        limiter.isAllowed("userA");
        assertFalse(limiter.isAllowed("userA"));
        // userB should still have tokens
        assertTrue(limiter.isAllowed("userB"));
    }

    @Test
    void testTotalDeniedCount() {
        Day138GatewayRateLimiter.PerKeyRateLimiter limiter =
                new Day138GatewayRateLimiter.PerKeyRateLimiter(1, 1, 1);
        limiter.isAllowed("k"); // allowed
        limiter.isAllowed("k"); // denied
        limiter.isAllowed("k"); // denied
        assertEquals(2, limiter.totalDenied());
    }

    @Test
    void testKeyResolverStrategies() {
        List<Day138GatewayRateLimiter.KeyResolverInfo> strategies =
                Day138GatewayRateLimiter.keyResolverStrategies();
        assertEquals(4, strategies.size());
    }
}
