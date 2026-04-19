package com.techleadguru.phase8.day175;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class Day175CacheStrategiesTest {

    @Test
    void strategiesListContainsAllFour() {
        var list = Day175CacheStrategies.strategies();
        assertEquals(4, list.size());
        boolean hasCacheAside = list.stream()
                .anyMatch(s -> s.strategy() == Day175CacheStrategies.CacheStrategy.CACHE_ASIDE);
        assertTrue(hasCacheAside);
    }

    @Test
    void cacheAsideSimulatorHitsCacheOnSecondCall() {
        var sim = new Day175CacheStrategies.CacheAsideSimulator();
        AtomicInteger dbCalls = new AtomicInteger();

        // First call: cache miss → DB hit
        sim.get("key1", () -> { dbCalls.incrementAndGet(); return "value1"; });
        // Second call: cache hit → no DB hit
        sim.get("key1", () -> { dbCalls.incrementAndGet(); return "value1"; });

        assertEquals(1, dbCalls.get()); // DB called only once
        assertEquals(1, sim.dbHits());
        assertEquals(1, sim.cacheHits());
    }

    @Test
    void cacheAsideSimulatorEvictsOnDelete() {
        var sim = new Day175CacheStrategies.CacheAsideSimulator();
        sim.get("k", () -> "v");
        sim.evict("k", () -> {});

        // After evict, next get should hit DB again
        AtomicInteger dbCalls = new AtomicInteger();
        sim.get("k", () -> { dbCalls.incrementAndGet(); return "v2"; });
        assertEquals(1, dbCalls.get());
    }

    @Test
    void consistencyGuideIsNonEmpty() {
        assertFalse(Day175CacheStrategies.consistencyGuide().isEmpty());
    }

    @Test
    void writePolicyComparisonContainsAllThree() {
        var list = Day175CacheStrategies.writePolicyComparison();
        assertFalse(list.isEmpty());
        list.forEach(c -> {
            assertNotNull(c.cacheAside());
            assertNotNull(c.writeThrough());
            assertNotNull(c.writeBehind());
        });
    }
}
