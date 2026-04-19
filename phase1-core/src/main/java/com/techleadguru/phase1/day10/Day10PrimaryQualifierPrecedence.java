package com.techleadguru.phase1.day10;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;

/**
 * DAY 10 — @Primary vs @Qualifier vs @Resource vs @Inject
 *
 * THE FOUR WAYS TO RESOLVE AMBIGUITY:
 *
 *   @Primary   — Mark one bean as the default. Spring picks it when no qualifier is specified.
 *                 Problem: Only one @Primary bean per type. Forces a default on all injection points.
 *                 Use when: you have a clear "main" implementation and rarely need the other.
 *
 *   @Qualifier — At the injection site, name the exact bean you want.
 *                 Use when: different injection points need different implementations.
 *                 Best practice: define custom annotation qualifiers to avoid string typos.
 *
 *   @Resource  — JSR-250. Resolves by NAME first, then type. From javax/jakarta.annotation.
 *                 @Resource(name="fooService") is equivalent to @Autowired + @Qualifier("fooService").
 *                 Useful when migrating legacy Java EE code.
 *
 *   @Inject    — JSR-330. @Inject = @Autowired (required=true). No required=false option.
 *                 Use @Inject + @Named("beanName") for name-based resolution.
 *                 Use when: writing framework-agnostic code (drops Spring API dependency).
 *
 * PRODUCTION SCENARIO — @Primary breaks a plugin system:
 *   AuthService has two impls: JwtAuthService, ApiKeyAuthService.
 *   @Primary on JwtAuthService — all injection points get JWT.
 *   New external auth endpoint added: must use ApiKeyAuthService.
 *   Team adds @Qualifier at that one site — works.
 *   Next sprint: third impl added. @Primary changed. All injection points silently switch.
 *   API key endpoint now also switches. Weeks of debugging.
 *   FIX: Custom @Qualifier annotations. No @Primary. Each site explicit.
 */
@Slf4j
public class Day10PrimaryQualifierPrecedence {

    public interface CacheService {
        String get(String key);
        void put(String key, String value);
    }

    // ===================================================================================
    // Multiple cache implementations
    // ===================================================================================

    @Slf4j
    public static class RedisCacheService implements CacheService {
        private final java.util.Map<String, String> store = new java.util.HashMap<>();

        @Override
        public String get(String key) { return store.get(key); }

        @Override
        public void put(String key, String value) {
            store.put(key, value);
            log.debug("[Day10] Redis put: {}={}", key, value);
        }

        @Override
        public String toString() { return "RedisCacheService"; }
    }

    @Slf4j
    public static class InMemoryCacheService implements CacheService {
        private final java.util.Map<String, String> store = new java.util.HashMap<>();

        @Override
        public String get(String key) { return store.get(key); }

        @Override
        public void put(String key, String value) {
            store.put(key, value);
            log.debug("[Day10] InMemory put: {}={}", key, value);
        }

        @Override
        public String toString() { return "InMemoryCacheService"; }
    }

    // ===================================================================================
    // Configuration: @Primary marks Redis as the default
    // ===================================================================================

    @Configuration
    public static class CacheConfig {

        @Bean
        @Primary // Default: all @Autowired CacheService injection points get Redis
        public CacheService redisCacheService() {
            return new RedisCacheService();
        }

        @Bean("inMemoryCacheService")
        public CacheService inMemoryCacheService() {
            return new InMemoryCacheService();
        }
    }

    // ===================================================================================
    // Service using @Primary — gets Redis without any qualifier
    // ===================================================================================

    @Slf4j
    public static class ProductService {
        private final CacheService cacheService; // gets @Primary = Redis

        public ProductService(CacheService cacheService) {
            this.cacheService = cacheService;
            log.info("[Day10] ProductService wired with: {}", cacheService);
        }

        public String getProduct(String id) {
            String cached = cacheService.get(id);
            if (cached == null) {
                cacheService.put(id, "Product:" + id);
                return "Product:" + id;
            }
            return cached;
        }

        public CacheService getCacheService() { return cacheService; }
    }

    // ===================================================================================
    // Service using @Qualifier("inMemoryCacheService") — overrides @Primary
    // ===================================================================================

    @Slf4j
    public static class SessionService {
        private final CacheService cacheService; // overrides @Primary with explicit qualifier

        public SessionService(@Qualifier("inMemoryCacheService") CacheService cacheService) {
            this.cacheService = cacheService;
            log.info("[Day10] SessionService wired with: {}", cacheService);
        }

        public void storeSession(String sessionId, String userId) {
            cacheService.put(sessionId, userId);
        }

        public String getSession(String sessionId) {
            return cacheService.get(sessionId);
        }

        public CacheService getCacheService() { return cacheService; }
    }
}
