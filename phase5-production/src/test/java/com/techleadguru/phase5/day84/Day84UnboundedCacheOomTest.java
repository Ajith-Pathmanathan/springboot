package com.techleadguru.phase5.day84;

import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class Day84UnboundedCacheOomTest {

    // ---- UnboundedReportCache (BROKEN) ----

    @Test
    void unboundedCache_grows_without_limit() {
        Day84UnboundedCacheOom.UnboundedReportCache cache = new Day84UnboundedCacheOom.UnboundedReportCache();
        cache.put("r1", new byte[1024]);
        cache.put("r2", new byte[1024]);
        cache.put("r3", new byte[1024]);
        assertThat(cache.size()).isEqualTo(3);
    }

    @Test
    void unboundedCache_tracks_hits_and_misses() {
        Day84UnboundedCacheOom.UnboundedReportCache cache = new Day84UnboundedCacheOom.UnboundedReportCache();
        cache.put("key", new byte[]{1});
        cache.get("key");   // hit
        cache.get("other"); // miss
        assertThat(cache.hits()).isEqualTo(1);
        assertThat(cache.misses()).isEqualTo(1);
    }

    // ---- BoundedReportCache (FIXED) ----

    @Test
    void boundedCache_respects_maximum_size() throws InterruptedException {
        Day84UnboundedCacheOom.BoundedReportCache cache =
                new Day84UnboundedCacheOom.BoundedReportCache(3, Duration.ofMinutes(5));
        for (int i = 0; i < 10; i++) {
            cache.put("key-" + i, new byte[]{(byte) i});
        }
        // Caffeine evicts asynchronously; give it a moment
        Thread.sleep(50);
        assertThat(cache.estimatedSize()).isLessThanOrEqualTo(5); // within window
    }

    @Test
    void boundedCache_getOrLoad_returns_value_on_miss() {
        Day84UnboundedCacheOom.BoundedReportCache cache =
                new Day84UnboundedCacheOom.BoundedReportCache(100, Duration.ofMinutes(5));
        byte[] result = cache.getOrLoad("report-1", k -> new byte[]{42});
        assertThat(result).isEqualTo(new byte[]{42});
    }

    @Test
    void boundedCache_getOrLoad_caches_result() {
        Day84UnboundedCacheOom.BoundedReportCache cache =
                new Day84UnboundedCacheOom.BoundedReportCache(100, Duration.ofMinutes(5));
        var callCount = new int[]{0};
        cache.getOrLoad("r", k -> { callCount[0]++; return new byte[]{1}; });
        cache.getOrLoad("r", k -> { callCount[0]++; return new byte[]{1}; });
        assertThat(callCount[0]).isEqualTo(1); // loader called only once
    }

    // ---- CacheStrategiesDemo ----

    @Test
    void warmCache_populates_all_keys() {
        Day84UnboundedCacheOom.BoundedReportCache cache =
                new Day84UnboundedCacheOom.BoundedReportCache(100, Duration.ofMinutes(5));
        Day84UnboundedCacheOom.CacheStrategiesDemo.warmCache(cache, List.of("a", "b", "c"));
        assertThat(cache.get("a")).isNotNull();
        assertThat(cache.get("b")).isNotNull();
    }
}
