package com.techleadguru.phase8.day174;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 174 — Redis Cache: Per-Cache TTL Configuration
 *
 * Spring's RedisCacheConfiguration allows per-cache TTL, null-value caching,
 * key prefixing, and custom serialisation.
 *
 * Pattern: build a Map<String, RedisCacheConfiguration> where each cache name
 * gets its own TTL and configuration, then wire into RedisCacheManager.
 */
public class Day174RedisCacheTTL {

    // ─────────────────────────────────────────────────────────────────────────
    // Cache configuration descriptor
    // ─────────────────────────────────────────────────────────────────────────

    public record CacheConfig(
            String   cacheName,
            Duration ttl,
            int      maxSizeHint,
            boolean  cacheNullValues) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Per-cache TTL builder (simulates RedisCacheManager config)
    // ─────────────────────────────────────────────────────────────────────────

    public static class PerCacheTtlConfig {

        private final Map<String, CacheConfig> configs = new LinkedHashMap<>();

        public PerCacheTtlConfig add(String name, Duration ttl, boolean cacheNulls) {
            configs.put(name, new CacheConfig(name, ttl, 0, cacheNulls));
            return this;
        }

        public PerCacheTtlConfig add(String name, Duration ttl) {
            return add(name, ttl, false);
        }

        public Map<String, CacheConfig> buildCacheConfigs() {
            return Collections.unmodifiableMap(configs);
        }

        public Optional<CacheConfig> configFor(String cacheName) {
            return Optional.ofNullable(configs.get(cacheName));
        }
    }

    /**
     * Returns a default PerCacheTtlConfig with typical per-domain TTLs.
     * Mirrors what you'd build with RedisCacheManager in production.
     */
    public static PerCacheTtlConfig defaultCacheTtls() {
        return new PerCacheTtlConfig()
                .add("products",         Duration.ofMinutes(30))
                .add("users",            Duration.ofMinutes(10))
                .add("orders",           Duration.ofMinutes(5))
                .add("sessions",         Duration.ofHours(2))
                .add("config",           Duration.ofHours(24))
                .add("search-results",   Duration.ofSeconds(60))
                .add("access-tokens",    Duration.ofMinutes(15));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Redis cache simulator (demonstrates per-entry TTL semantics)
    // ─────────────────────────────────────────────────────────────────────────

    private record Entry(Object value, Instant expiresAt) {
        boolean isExpired(Instant now) { return now.isAfter(expiresAt); }
    }

    public static class RedisCacheSimulator {

        private final Duration           defaultTtl;
        private final Map<String, Entry> store = new ConcurrentHashMap<>();

        public RedisCacheSimulator(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public void put(String key, Object value) {
            put(key, value, defaultTtl);
        }

        public void put(String key, Object value, Duration ttl) {
            store.put(key, new Entry(value, Instant.now().plus(ttl)));
        }

        public Optional<Object> get(String key) {
            Entry e = store.get(key);
            if (e == null || e.isExpired(Instant.now())) {
                store.remove(key);
                return Optional.empty();
            }
            return Optional.of(e.value());
        }

        public int evictExpired() {
            Instant now = Instant.now();
            List<String> expired = store.entrySet().stream()
                    .filter(en -> en.getValue().isExpired(now))
                    .map(Map.Entry::getKey)
                    .toList();
            expired.forEach(store::remove);
            return expired.size();
        }

        public int size() { return store.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Null-value caching guide
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> cacheNullValuesGuide() {
        return List.of(
            "Enable null caching to prevent cache-miss storms on non-existent keys (stampede prevention)",
            "Use a short TTL for null values (e.g. 30s) compared to real values (e.g. 30m)",
            "RedisConfig: RedisCacheConfiguration.defaultConfig().entryTtl(ttl) — caches nulls by default",
            "To disable null caching: .disableCachingNullValues()",
            "Custom NullValueSerializer required when CacheValue extends Serializable",
            "Be careful: cached nulls can hide actual data inserted after the null was cached"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Redis cache property guide
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> redisConfigProperties() {
        return Map.of(
            "spring.data.redis.host",          "localhost",
            "spring.data.redis.port",          "6379",
            "spring.data.redis.password",      "<secret>",
            "spring.data.redis.timeout",       "2000ms",
            "spring.data.redis.lettuce.pool.max-active",  "8",
            "spring.data.redis.lettuce.pool.max-idle",    "8",
            "spring.cache.type",               "redis",
            "spring.cache.redis.time-to-live", "600000"  // fallback TTL in ms
        );
    }
}
