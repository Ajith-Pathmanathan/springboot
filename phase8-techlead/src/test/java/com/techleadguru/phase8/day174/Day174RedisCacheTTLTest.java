package com.techleadguru.phase8.day174;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class Day174RedisCacheTTLTest {

    @Test
    void perCacheTtlConfigBuildsCorrectly() {
        var config = Day174RedisCacheTTL.defaultCacheTtls().buildCacheConfigs();
        assertTrue(config.containsKey("products"));
        assertTrue(config.containsKey("sessions"));
        assertEquals(Duration.ofMinutes(30), config.get("products").ttl());
        assertEquals(Duration.ofHours(2),    config.get("sessions").ttl());
    }

    @Test
    void perCacheTtlConfigFindsExistingEntry() {
        var cfg = Day174RedisCacheTTL.defaultCacheTtls();
        assertTrue(cfg.configFor("orders").isPresent());
        assertTrue(cfg.configFor("nonexistent").isEmpty());
    }

    @Test
    void redisCacheSimulatorStoresAndRetrievesValue() {
        var cache = new Day174RedisCacheTTL.RedisCacheSimulator(Duration.ofMinutes(5));
        cache.put("k1", "value1");
        assertEquals("value1", cache.get("k1").orElse(null));
    }

    @Test
    void redisCacheSimulatorReturnsEmptyForMissingKey() {
        var cache = new Day174RedisCacheTTL.RedisCacheSimulator(Duration.ofMinutes(5));
        assertTrue(cache.get("missing").isEmpty());
    }

    @Test
    void redisCacheSimulatorRespectsCustomTtl() throws InterruptedException {
        var cache = new Day174RedisCacheTTL.RedisCacheSimulator(Duration.ofMinutes(5));
        cache.put("short", "val", Duration.ofMillis(50)); // very short TTL
        Thread.sleep(100);
        assertTrue(cache.get("short").isEmpty()); // expired
    }

    @Test
    void cacheNullValuesGuideIsNonEmpty() {
        assertFalse(Day174RedisCacheTTL.cacheNullValuesGuide().isEmpty());
    }

    @Test
    void redisConfigPropertiesContainsHost() {
        var props = Day174RedisCacheTTL.redisConfigProperties();
        assertTrue(props.containsKey("spring.data.redis.host"));
    }
}
