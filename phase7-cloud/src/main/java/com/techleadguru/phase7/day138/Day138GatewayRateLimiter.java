package com.techleadguru.phase7.day138;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Day 138 — Gateway filters: rate limiter with Redis token bucket.
 *
 * Spring Cloud Gateway uses a token-bucket algorithm backed by Redis
 * (via RedisRateLimiter bean).
 *
 * Token bucket algorithm:
 *   - Bucket fills at 'replenishRate' tokens/second
 *   - Bucket capacity = 'burstCapacity' tokens
 *   - Each request consumes 'requestedTokens' (default 1)
 *   - If tokens available → allow (decrement and return ALLOWED)
 *   - If tokens < requested → reject (return DENIED, HTTP 429)
 *
 * Configuration:
 *   filters:
 *     - name: RequestRateLimiter
 *       args:
 *         redis-rate-limiter.replenishRate: 10
 *         redis-rate-limiter.burstCapacity: 20
 *         redis-rate-limiter.requestedTokens: 1
 *         key-resolver: "#{@userKeyResolver}"
 *
 * Key resolver: identifies the bucket owner (by user, IP, or API key).
 */
public class Day138GatewayRateLimiter {

    // ─────────────────────────────────────────────────────────────────────────
    // Token bucket (pure Java simulation of Redis-backed bucket)
    // ─────────────────────────────────────────────────────────────────────────

    public static class TokenBucket {

        private final int    replenishRate;   // tokens added per second
        private final int    burstCapacity;   // max bucket size
        private final int    requestedTokens; // tokens consumed per request

        private double  tokens;
        private long    lastRefillNanos;

        public TokenBucket(int replenishRate, int burstCapacity, int requestedTokens) {
            this.replenishRate   = replenishRate;
            this.burstCapacity   = burstCapacity;
            this.requestedTokens = requestedTokens;
            this.tokens          = burstCapacity;
            this.lastRefillNanos = System.nanoTime();
        }

        /** Try to consume tokens. Returns true (allowed) or false (rate-limited). */
        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= requestedTokens) {
                tokens -= requestedTokens;
                return true;
            }
            return false;
        }

        private void refill() {
            long nowNanos = System.nanoTime();
            double elapsed = (nowNanos - lastRefillNanos) / 1_000_000_000.0;
            double newTokens = elapsed * replenishRate;
            tokens = Math.min(burstCapacity, tokens + newTokens);
            lastRefillNanos = nowNanos;
        }

        public int replenishRate()   { return replenishRate; }
        public int burstCapacity()   { return burstCapacity; }
        public int requestedTokens() { return requestedTokens; }
        public double currentTokens() { refill(); return tokens; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-key rate limiter (simulates Redis key = "request_rate_limiter.{key}.tokens")
    // ─────────────────────────────────────────────────────────────────────────

    public static class PerKeyRateLimiter {

        private final int replenishRate;
        private final int burstCapacity;
        private final int requestedTokens;
        private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
        private final AtomicLong totalDenied = new AtomicLong(0);

        public PerKeyRateLimiter(int replenishRate, int burstCapacity, int requestedTokens) {
            this.replenishRate   = replenishRate;
            this.burstCapacity   = burstCapacity;
            this.requestedTokens = requestedTokens;
        }

        public boolean isAllowed(String key) {
            TokenBucket bucket = buckets.computeIfAbsent(key,
                    k -> new TokenBucket(replenishRate, burstCapacity, requestedTokens));
            boolean allowed = bucket.tryConsume();
            if (!allowed) totalDenied.incrementAndGet();
            return allowed;
        }

        public long totalDenied() { return totalDenied.get(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Key resolver strategies (Spring KeyResolver functional interface)
    // ─────────────────────────────────────────────────────────────────────────

    public enum KeyResolverStrategy { USER_PRINCIPAL, REMOTE_IP, API_KEY, COMBINATION }

    public record KeyResolverInfo(
            KeyResolverStrategy strategy,
            String beanExpression,
            String description) {}

    public static List<KeyResolverInfo> keyResolverStrategies() {
        return List.of(
            new KeyResolverInfo(KeyResolverStrategy.USER_PRINCIPAL,
                "#{@principalNameKeyResolver}",
                "Rate limit by authenticated user identity (JWT sub claim)"),
            new KeyResolverInfo(KeyResolverStrategy.REMOTE_IP,
                "exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress())",
                "Rate limit by client IP (beware proxies and NAT)"),
            new KeyResolverInfo(KeyResolverStrategy.API_KEY,
                "exchange -> Mono.just(exchange.getRequest().getHeaders().getFirst(\"X-Api-Key\"))",
                "Rate limit by API key header"),
            new KeyResolverInfo(KeyResolverStrategy.COMBINATION,
                "exchange -> Mono.just(userId + \":\" + tenantId)",
                "Combine user + tenant for multi-tenant rate limiting")
        );
    }
}
