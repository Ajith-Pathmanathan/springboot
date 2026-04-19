package com.techleadguru.phase8.day176;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class Day176CacheStampedeTest {

    @Test
    void typicalScenarioHasHighConcurrency() {
        var scenario = Day176CacheStampede.typicalScenario();
        assertTrue(scenario.concurrentRequestsOnExpiry() > 100);
        assertNotNull(scenario.impact());
    }

    @Test
    void xFetchEventuallyTriggersEarlyRefresh() {
        // With a very short TTL and a high delta, shouldRefreshEarly should
        // return true at least once in many trials (beta=1, delta=large).
        var xfetch = new Day176CacheStampede.XFetchAlgorithm(1.0);
        Instant expiresAt = Instant.now().plusMillis(100); // expires in 100ms
        long deltaMs = 5000; // recompute takes 5s → always should refresh

        boolean refreshTriggered = false;
        for (int i = 0; i < 100; i++) {
            if (xfetch.shouldRefreshEarly(deltaMs, expiresAt)) {
                refreshTriggered = true;
                break;
            }
        }
        assertTrue(refreshTriggered,
                "XFetch should trigger early refresh with large delta vs small remaining TTL");
    }

    @Test
    void mutexCacheSimulatorOnlyCallsLoaderOnce() {
        var cache = new Day176CacheStampede.MutexCacheSimulator();
        AtomicInteger loaderCalls = new AtomicInteger();

        // Call twice; loader should only be invoked once (second call hits cache)
        String v1 = (String) cache.get("k", () -> {
            loaderCalls.incrementAndGet(); return "value";
        }, Duration.ofMinutes(5));

        String v2 = (String) cache.get("k", () -> {
            loaderCalls.incrementAndGet(); return "value";
        }, Duration.ofMinutes(5));

        assertEquals("value", v1);
        assertEquals("value", v2);
        assertEquals(1, loaderCalls.get());
    }

    @Test
    void thunderingHerdExamplesAreNonEmpty() {
        var examples = Day176CacheStampede.thunderingHerdExamples();
        assertFalse(examples.isEmpty());
        examples.forEach(e -> assertNotNull(e.trigger()));
    }

    @Test
    void mitigationStrategiesAreNonEmpty() {
        var strategies = Day176CacheStampede.mitigationStrategies();
        assertFalse(strategies.isEmpty());
        boolean hasMutex = strategies.stream()
                .anyMatch(s -> s.name().toLowerCase().contains("mutex"));
        assertTrue(hasMutex);
    }
}
