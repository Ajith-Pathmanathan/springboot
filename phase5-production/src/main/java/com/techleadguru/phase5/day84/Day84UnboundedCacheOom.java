package com.techleadguru.phase5.day84;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DAY 84 — Unbounded In-Memory Cache OOM → Caffeine Fix
 *
 * PROBLEM: in-memory caches without size/TTL limits grow forever → OOM.
 *
 * COMMON ANTI-PATTERNS:
 *   - HashMap/ConcurrentHashMap used as a cache with computeIfAbsent()
 *   - @Cacheable with Spring's default ConcurrentMapCacheManager (unbounded)
 *   - Custom registry map that adds entries but never evicts
 *
 * CAFFEINE LIMITS:
 *   maximumSize(N)          → evict least-recently-used when entry count > N
 *   maximumWeight(W)        → evict when total weight > W (custom Weigher)
 *   expireAfterWrite(D)     → expire D after entry was written (regardless of reads)
 *   expireAfterAccess(D)    → expire D after last read/write (keeps hot items alive)
 *   refreshAfterWrite(D)    → async refresh on next read after D (no blocking caller)
 *   softValues()            → GC can evict when memory pressured (last resort)
 *   weakValues()            → evicted when no other strong references
 *
 * MONITORING:
 *   cache.stats().hitRate()    → hit rate (target > 0.8 in production)
 *   cache.stats().evictionCount() → how many entries were evicted
 *   cache.estimatedSize()      → current entry count
 *   Via Actuator: GET /actuator/caches
 *
 * SPRING @Cacheable + Caffeine:
 *   spring.cache.type=caffeine
 *   spring.cache.caffeine.spec=maximumSize=1000,expireAfterWrite=10m
 *   Or configure per-cache via CacheManagerCustomizer bean.
 */
@Slf4j
public class Day84UnboundedCacheOom {

    // =========================================================================
    // BROKEN: unbounded HashMap cache
    // =========================================================================

    public static class UnboundedReportCache {

        // BUG: entries are added but never removed → OOM after ~100K reports
        private final Map<String, byte[]> cache = new HashMap<>();
        private final AtomicLong hits   = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();

        public byte[] get(String key) {
            byte[] val = cache.get(key);
            if (val != null) { hits.incrementAndGet(); return val; }
            misses.incrementAndGet();
            return null;
        }

        public void put(String key, byte[] value) {
            cache.put(key, value);
        }

        public int size()         { return cache.size(); }
        public long hits()        { return hits.get(); }
        public long misses()      { return misses.get(); }
        public void clear()       { cache.clear(); }
    }

    // =========================================================================
    // FIXED: Caffeine bounded cache
    // =========================================================================

    public static class BoundedReportCache {

        private final Cache<String, byte[]> cache;

        public BoundedReportCache(long maxSize, Duration expireAfterWrite) {
            this.cache = Caffeine.newBuilder()
                    .maximumSize(maxSize)
                    .expireAfterWrite(expireAfterWrite)
                    .recordStats()  // enables hit/miss rate tracking
                    .build();
        }

        public byte[] get(String key) {
            return cache.getIfPresent(key);
        }

        /** get-or-load pattern: fetch from DB only on cache miss */
        public byte[] getOrLoad(String key, java.util.function.Function<String, byte[]> loader) {
            return cache.get(key, loader);
        }

        public void put(String key, byte[] value) {
            cache.put(key, value);
        }

        public void invalidate(String key)  { cache.invalidate(key); }
        public long estimatedSize()         { return cache.estimatedSize(); }
        public double hitRate()             { return cache.stats().hitRate(); }
        public long evictionCount()         { return cache.stats().evictionCount(); }
        public long requestCount()          { return cache.stats().requestCount(); }
    }

    // =========================================================================
    // Spring @Cacheable with Caffeine spec — application.properties approach
    // =========================================================================

    @Service
    @Slf4j
    public static class ProductCatalogService {

        private final AtomicLong dbHits = new AtomicLong();

        /**
         * With spring.cache.type=caffeine and cache spec configured,
         * this method is only called on cache miss.
         * Cache key = productId; cache name = "products"
         */
        @Cacheable(value = "products", key = "#productId")
        public Product findById(String productId) {
            dbHits.incrementAndGet();
            log.debug("[Day84] DB hit for product {}", productId);
            // Simulate DB fetch
            return new Product(productId, "Product-" + productId, 100 + productId.length());
        }

        public long getDbHitCount() { return dbHits.get(); }
        public void resetStats()    { dbHits.set(0); }

        public record Product(String id, String name, double price) {}
    }

    // =========================================================================
    // Cache warming vs lazy loading comparison
    // =========================================================================

    public static class CacheStrategiesDemo {

        /**
         * LAZY LOADING (default):
         * First call to get(key) hits DB, fills cache.
         * Pros: only popular items cached; no startup cost.
         * Cons: first user after restart sees slow response (cold start).
         */
        public static String strategy_lazy(BoundedReportCache cache, String key) {
            byte[] value = cache.getOrLoad(key, k -> {
                // simulate DB load
                return ("data-for-" + k).getBytes();
            });
            return new String(value);
        }

        /**
         * CACHE WARMING:
         * Pre-populate cache at startup with most-accessed keys.
         * Pros: first user sees fast response.
         * Cons: startup time, wastes memory if pre-loaded items not accessed.
         */
        public static void warmCache(BoundedReportCache cache, java.util.List<String> keys) {
            for (String key : keys) {
                cache.put(key, ("warm-data-" + key).getBytes());
            }
            log.info("[Day84] Warmed cache with {} entries", keys.size());
        }
    }
}
