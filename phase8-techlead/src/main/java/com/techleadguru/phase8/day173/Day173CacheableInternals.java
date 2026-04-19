package com.techleadguru.phase8.day173;

import java.util.*;

/**
 * Day 173 — @Cacheable / @CacheEvict / @CachePut Internals
 *
 * Spring Cache abstraction intercepts annotated methods via a Spring AOP proxy.
 * The proxy delegates to a CacheManager which retrieves named Cache instances.
 *
 * Proxy intercept chain:
 *  CacheInterceptor → checks key in cache → [hit] return cached / [miss] invoke real method → put in cache
 *
 * Key:hashCode-based by default; customise with SpEL expressions.
 */
public class Day173CacheableInternals {

    // ─────────────────────────────────────────────────────────────────────────
    // Cache operation types
    // ─────────────────────────────────────────────────────────────────────────

    public enum CacheOperation {
        CACHEABLE,   // read-through: check cache first; invoke method only on miss
        CACHE_PUT,   // write-through: always invoke method, always update cache
        CACHE_EVICT  // delete: remove entry (or all entries) on method call
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Interceptor steps
    // ─────────────────────────────────────────────────────────────────────────

    public record InterceptorStep(int order, String action, String notes) {}

    public static List<InterceptorStep> cacheInterceptorSteps() {
        return List.of(
            new InterceptorStep(1, "Proxy intercepts method call",
                "AOP around advice; method not yet invoked"),
            new InterceptorStep(2, "Evaluate SpEL key expression",
                "Default: SimpleKey(params); custom: #id, #order.id, etc."),
            new InterceptorStep(3, "Look up CacheManager.getCache(name)",
                "Returns the named cache instance"),
            new InterceptorStep(4, "@Cacheable — check cache.get(key)",
                "Hit: return cached value immediately; Miss: continue to step 5"),
            new InterceptorStep(5, "Invoke real method",
                "Only reached on cache miss or for @CachePut"),
            new InterceptorStep(6, "Store result in cache",
                "@Cacheable and @CachePut: cache.put(key, result)"),
            new InterceptorStep(7, "@CacheEvict — call cache.evict(key) or cache.clear()",
                "beforeInvocation=true: evict before method; false (default): after")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SpEL key expressions
    // ─────────────────────────────────────────────────────────────────────────

    public record SpELKeyExample(String expression, String description) {}

    public static List<SpELKeyExample> spelKeyExamples() {
        return List.of(
            new SpELKeyExample("#id", "Use method parameter named 'id' as key"),
            new SpELKeyExample("#order.id", "Use field of parameter object"),
            new SpELKeyExample("#root.method.name", "Use method name as key"),
            new SpELKeyExample("T(java.util.Objects).hash(#a,#b)", "Composite key from two params"),
            new SpELKeyExample("'orders:' + #customerId", "Prefixed string key")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Conditional cache guide
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> conditionalCacheGuide() {
        return List.of(
            "condition='#id > 0' — only cache if condition is true (evaluated before method)",
            "unless='#result == null' — do NOT cache if result meets condition (after method)",
            "unless='#result.empty' — skip caching if list is empty",
            "condition='#query.length > 2' — skip cache for very short queries",
            "unless='#result.size() > 1000' — skip caching huge results"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Annotation comparison
    // ─────────────────────────────────────────────────────────────────────────

    public record AnnotationComparison(
            String annotation,
            String invokesMethod,
            String updatesCache,
            String bestUseCase) {}

    public static List<AnnotationComparison> cacheAnnotationComparison() {
        return List.of(
            new AnnotationComparison("@Cacheable",
                "Only on cache miss",
                "Yes — on miss",
                "Read-heavy: fetch-by-id, catalogue queries"),
            new AnnotationComparison("@CachePut",
                "Always",
                "Always",
                "Write endpoints that must keep cache in sync"),
            new AnnotationComparison("@CacheEvict",
                "Always",
                "No — it removes",
                "Delete or update endpoints where stale data is unacceptable")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cache manager types
    // ─────────────────────────────────────────────────────────────────────────

    public record CacheManagerType(
            String name,
            String springBootStarter,
            String persistence,
            String notes) {}

    public static List<CacheManagerType> cacheManagerTypes() {
        return List.of(
            new CacheManagerType("ConcurrentMapCacheManager",
                "None (default)",
                "In-process, JVM heap",
                "Zero config; no TTL; not suitable for multi-instance deployments"),
            new CacheManagerType("CaffeineCacheManager",
                "spring-boot-starter-cache + caffeine",
                "In-process JVM heap with eviction",
                "High-performance; supports TTL, max size, weak refs"),
            new CacheManagerType("RedisCacheManager",
                "spring-boot-starter-data-redis",
                "Distributed Redis",
                "Shared across pods; supports TTL per cache; serialisation required"),
            new CacheManagerType("EhCacheCacheManager",
                "ehcache",
                "Local or distributed (Terracotta)",
                "Mature; rich config; less common in new Spring Boot projects"),
            new CacheManagerType("HazelcastCacheManager",
                "hazelcast-spring",
                "Distributed in-memory grid",
                "Near-cache option; good for heavy compute caching across cluster")
        );
    }
}
