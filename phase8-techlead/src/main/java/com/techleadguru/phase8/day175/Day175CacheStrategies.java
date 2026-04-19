package com.techleadguru.phase8.day175;

import java.util.*;
import java.util.function.*;

/**
 * Day 175 — Cache Strategies: Cache-Aside, Write-Through, Write-Behind, Read-Through
 *
 * Each strategy defines when data is read from and written to the cache
 * relative to the backing store (database).
 */
public class Day175CacheStrategies {

    // ─────────────────────────────────────────────────────────────────────────
    // Strategy enum
    // ─────────────────────────────────────────────────────────────────────────

    public enum CacheStrategy {
        CACHE_ASIDE,   // application manages cache manually
        WRITE_THROUGH, // synchronous write to cache + store
        WRITE_BEHIND,  // async write to store after cache update
        READ_THROUGH   // cache loads from store on miss (cache sits in front)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strategy info
    // ─────────────────────────────────────────────────────────────────────────

    public record CacheStrategyInfo(
            CacheStrategy strategy,
            String        description,
            List<String>  pros,
            List<String>  cons,
            String        bestFor) {}

    public static List<CacheStrategyInfo> strategies() {
        return List.of(
            new CacheStrategyInfo(
                CacheStrategy.CACHE_ASIDE,
                "Application reads cache; on miss, loads from DB and populates cache",
                List.of("Simple to implement", "Resilient — DB still accessible if cache fails",
                        "Lazy population — only cache what is needed"),
                List.of("Cache stampede on cold start", "Application manages consistency",
                        "Extra code in every service"),
                "General-purpose read-heavy workloads"),

            new CacheStrategyInfo(
                CacheStrategy.WRITE_THROUGH,
                "Every write goes to cache first, then synchronously to DB",
                List.of("Cache always consistent with DB", "No stale reads after write",
                        "Simple read path — always read from cache"),
                List.of("Write latency = cache write + DB write", "Cache may hold rarely-read data",
                        "Cold cache on restart means all data in DB"),
                "Write-then-read patterns, user profile updates"),

            new CacheStrategyInfo(
                CacheStrategy.WRITE_BEHIND,
                "Write to cache; DB write happens asynchronously (buffered)",
                List.of("Low write latency — return after cache write",
                        "DB write batching reduces I/O"),
                List.of("Data loss risk if cache fails before async persistence",
                        "Complex implementation", "Ordering guarantees needed"),
                "High-throughput writes where small data loss is acceptable (analytics counters)"),

            new CacheStrategyInfo(
                CacheStrategy.READ_THROUGH,
                "Cache automatically loads data from DB on miss (plugin model)",
                List.of("Application only reads from cache; cache handles DB",
                        "Consistent read path"),
                List.of("First request always slow (cold miss)", "Cache provider must support loaders",
                        "Less control over loading logic"),
                "Caffeine/Ehcache with CacheLoader; transparent to application")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cache-aside simulator
    // ─────────────────────────────────────────────────────────────────────────

    public static class CacheAsideSimulator {

        private final Map<String, Object> cache = new LinkedHashMap<>();
        private int dbHits  = 0;
        private int cacheHits = 0;

        /** Get with cache-aside pattern. supplier = DB lookup. */
        public Object get(String key, Supplier<Object> dbLookup) {
            Object cached = cache.get(key);
            if (cached != null) {
                cacheHits++;
                return cached;
            }
            // cache miss — load from DB
            dbHits++;
            Object value = dbLookup.get();
            if (value != null) cache.put(key, value);
            return value;
        }

        /** Write-aside: update cache and DB (application's responsibility). */
        public void put(String key, Object value, Runnable dbWriter) {
            dbWriter.run();
            cache.put(key, value);
        }

        /** Evict from cache on delete. */
        public void evict(String key, Runnable dbDeleter) {
            dbDeleter.run();
            cache.remove(key);
        }

        public int dbHits()    { return dbHits; }
        public int cacheHits() { return cacheHits; }
        public int cacheSize() { return cache.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consistency guide
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> consistencyGuide() {
        return List.of(
            "Cache-aside: use short TTL or evict on write to prevent stale reads",
            "Write-through: cache is always in sync but write throughput limited by DB latency",
            "Write-behind: accept eventual consistency; ensure idempotent DB writes",
            "Read-through: pair with write-through or write-behind to keep cache current",
            "Distributed caches: use optimistic locking (CAS) to avoid lost updates across nodes",
            "Near-cache: local JVM copy + distributed cache; local eviction on remote update",
            "Always test cache invalidation path; it is the hardest part of caching"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Write policy comparison
    // ─────────────────────────────────────────────────────────────────────────

    public record WritePolicyComparison(
            String aspect,
            String cacheAside,
            String writeThrough,
            String writeBehind) {}

    public static List<WritePolicyComparison> writePolicyComparison() {
        return List.of(
            new WritePolicyComparison("Write latency",
                "DB latency (cache secondary)",
                "Cache + DB latency (in series)",
                "Cache-only (DB async)"),
            new WritePolicyComparison("Data safety",
                "DB is source of truth; cache loss is safe",
                "DB is source of truth; very safe",
                "Cache loss = data loss before async flush"),
            new WritePolicyComparison("Consistency",
                "Possible stale window after write",
                "Immediately consistent",
                "Eventually consistent"),
            new WritePolicyComparison("Spring Boot support",
                "@Cacheable + @CacheEvict manually",
                "@CachePut on every write method",
                "Not native; custom async writer needed")
        );
    }
}
