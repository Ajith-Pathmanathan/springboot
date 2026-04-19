package com.techleadguru.phase8.day176;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Day 176 — Cache Stampede: Thundering Herd Problem
 *
 * Cache stampede: when a popular key expires, many concurrent requests
 * all simultaneously hit the DB — causing a traffic spike that can
 * overwhelm the database.
 *
 * Mitigation strategies:
 *  1. Probabilistic early expiry (XFetch / early recompute)
 *  2. Mutex / lock-based single population
 *  3. Background refresh (async recompute before expiry)
 *  4. Staggered TTL (add jitter to expiry)
 */
public class Day176CacheStampede {

    // ─────────────────────────────────────────────────────────────────────────
    // Stampede scenario descriptor
    // ─────────────────────────────────────────────────────────────────────────

    public record StampedeScenario(
            String  cacheName,
            int     concurrentRequestsOnExpiry,
            long    dbQueryTimeMs,
            String  impact) {}

    public static StampedeScenario typicalScenario() {
        return new StampedeScenario(
            "product-catalogue",
            500,
            1500,
            "500 simultaneous DB queries; probable DB overload and cascading timeouts"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XFetch probabilistic early expiry algorithm
    //
    // Formula (from XFetch paper):
    //   now + beta * delta * ln(rand) > expiresAt - ttl
    //   => if true, refresh the cache entry early (before it expires)
    //   beta: tuning parameter (typically 1.0)
    //   delta: time in ms to recompute the value
    //   rand:  uniform random U(0,1)
    // ─────────────────────────────────────────────────────────────────────────

    public static class XFetchAlgorithm {

        private final double beta;
        private final Random random;

        public XFetchAlgorithm(double beta) {
            this.beta   = beta;
            this.random = new Random();
        }

        /**
         * Returns true if the caller should refresh the cache entry now
         * (before it expires) to avoid a stampede.
         *
         * @param deltaMs      time in ms to recompute the cached value
         * @param expiresAt    when the cache entry expires
         */
        public boolean shouldRefreshEarly(long deltaMs, Instant expiresAt) {
            double rand  = random.nextDouble();
            if (rand <= 0.0) rand = 1e-10; // avoid ln(0)
            long nowMs   = Instant.now().toEpochMilli();
            long expMs   = expiresAt.toEpochMilli();
            double score = nowMs + beta * deltaMs * (-Math.log(rand));
            return score >= expMs;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mutex-based cache (only one thread refreshes; others wait)
    // ─────────────────────────────────────────────────────────────────────────

    public static class MutexCacheSimulator {

        private record Entry(Object value, Instant expiresAt) {
            boolean isExpired() { return Instant.now().isAfter(expiresAt); }
        }

        private final Map<String, Entry>          store   = new ConcurrentHashMap<>();
        private final Map<String, ReentrantLock>  locks   = new ConcurrentHashMap<>();

        /**
         * Get or load. Only one thread runs the loader; others block and
         * receive the freshly computed value.
         */
        public Object get(String key, java.util.function.Supplier<Object> loader,
                          java.time.Duration ttl) {
            Entry e = store.get(key);
            if (e != null && !e.isExpired()) return e.value();

            ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
            lock.lock();
            try {
                // double-check after acquiring lock
                e = store.get(key);
                if (e != null && !e.isExpired()) return e.value();

                Object value = loader.get();
                store.put(key, new Entry(value, Instant.now().plus(ttl)));
                return value;
            } finally {
                lock.unlock();
            }
        }

        public int size() { return store.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Thundering herd examples
    // ─────────────────────────────────────────────────────────────────────────

    public record ThunderingHerdExample(String scenario, String trigger, String consequence) {}

    public static List<ThunderingHerdExample> thunderingHerdExamples() {
        return List.of(
            new ThunderingHerdExample(
                "Product catalogue expiry",
                "Popular /products cache key expires at peak traffic",
                "Hundreds of threads hit DB concurrently; DB CPU spikes to 100%"),
            new ThunderingHerdExample(
                "Session cache flush",
                "Redis restart clears all session caches",
                "All users trigger simultaneous DB session reads on next request"),
            new ThunderingHerdExample(
                "Rate limiter counter reset",
                "Counter TTL expires for all users at the same second",
                "Burst of DB writes overloads the counter store"),
            new ThunderingHerdExample(
                "CDN cache invalidation",
                "Deploy triggers cache purge for high-traffic pages",
                "Origin server receives unbounded traffic spike")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mitigation strategies
    // ─────────────────────────────────────────────────────────────────────────

    public record MitigationStrategy(String name, String description, String tradeoff) {}

    public static List<MitigationStrategy> mitigationStrategies() {
        return List.of(
            new MitigationStrategy(
                "Probabilistic Early Expiry (XFetch)",
                "Gradually increase chance of refresh as TTL approaches expiry",
                "Random; hard to tune beta parameter exactly"),
            new MitigationStrategy(
                "Mutex / distributed lock",
                "Only first thread recomputes; others wait for result",
                "Increased latency for waiters; lock contention risk"),
            new MitigationStrategy(
                "Background refresh",
                "Dedicated thread refreshes cache before TTL expires",
                "Extra complexity; stale data served briefly"),
            new MitigationStrategy(
                "TTL jitter",
                "Add random offset to TTL: ttl + random(0, jitter)",
                "Distributes expiry across time; simplest mitigation"),
            new MitigationStrategy(
                "Cache-aside with stale-while-revalidate",
                "Return stale value on miss while async refresh runs",
                "Requires stale-value storage; eventual consistency"),
            new MitigationStrategy(
                "Request coalescing",
                "Deduplicate identical in-flight requests; share single DB call",
                "Complex implementation in application layer")
        );
    }
}
