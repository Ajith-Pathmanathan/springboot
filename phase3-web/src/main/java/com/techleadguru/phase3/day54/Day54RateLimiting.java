package com.techleadguru.phase3.day54;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAY 54 — Rate Limiting with Bucket4j
 *
 * WHY RATE LIMITING?
 *   Without rate limiting, a single client can:
 *     - Send 10,000 requests/second → DoS your service
 *     - Scrape all your data → intellectual property theft
 *     - Brute force login attempts → credential stuffing attack
 *     - Exhaust your database connections → cascading failure
 *
 * TOKEN BUCKET ALGORITHM (Bucket4j's approach):
 *   - Bucket starts with N tokens (capacity)
 *   - Each request consumes 1 token
 *   - Tokens refill at rate R per time window
 *   - If bucket is empty → 429 Too Many Requests
 *
 *   Example: capacity=10, refill=10/minute
 *     → First 10 requests: instant (drain bucket)
 *     → After that: 1 request every 6 seconds
 *     → Bucket refills to 10 after 1 minute
 *
 * RATE LIMIT GRANULARITY — what key to bucket by:
 *   "IP address"    → simple, but shared IPs (NAT, VPN) affect many users
 *   "API key"       → fair per-client limiting
 *   "userId"        → requires authentication
 *   "endpoint"      → different limits per API tier
 *   Production: combine userId + endpoint for fine-grained control
 *
 * RESPONSE HEADERS (RFC 6585):
 *   X-Rate-Limit-Remaining: 7  → tokens left in current window
 *   X-Rate-Limit-Limit: 10     → max tokens per window
 *   Retry-After: 45            → seconds until refill (on 429)
 *
 * DISTRIBUTED RATE LIMITING:
 *   In-memory buckets work for single-instance only.
 *   For multiple instances: use Redis with Bucket4j-Redis:
 *     ProxyManager<String> proxyManager = Bucket4jRedis.casBasedBuilder(redissonClient).build();
 *     Bucket bucket = proxyManager.builder().addLimit(bandwidth).build(clientKey);
 */
@Slf4j
public class Day54RateLimiting {

    // =========================================================================
    // Registry — per-client bucket storage
    // =========================================================================

    @Slf4j
    public static class BucketRegistry {

        private final long capacity;
        private final long refillPerMinute;
        private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

        public BucketRegistry(long capacity, long refillPerMinute) {
            this.capacity = capacity;
            this.refillPerMinute = refillPerMinute;
            log.debug("[Day54] BucketRegistry: capacity={}, refill={}/min", capacity, refillPerMinute);
        }

        public Bucket getBucket(String clientKey) {
            return buckets.computeIfAbsent(clientKey, k -> createBucket());
        }

        private Bucket createBucket() {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(capacity)
                    .refillGreedy(refillPerMinute, Duration.ofMinutes(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        }

        public long getCapacity() { return capacity; }
    }

    // =========================================================================
    // Rate limiting filter
    // =========================================================================

    @Slf4j
    public static class RateLimitingFilter extends OncePerRequestFilter {

        private final BucketRegistry registry;

        public RateLimitingFilter(BucketRegistry registry) {
            this.registry = registry;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            String clientKey = resolveClientKey(request);
            Bucket bucket = registry.getBucket(clientKey);

            long remainingTokens = bucket.getAvailableTokens();

            // Add rate limit headers to all responses
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(registry.getCapacity()));
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(Math.max(0, remainingTokens - 1)));

            if (bucket.tryConsume(1)) {
                log.debug("[Day54] Request allowed for {}: {} tokens remaining", clientKey, remainingTokens - 1);
                filterChain.doFilter(request, response);
            } else {
                log.warn("[Day54] Rate limit exceeded for client: {}", clientKey);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", "60");
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Rate limit exceeded\",\"retryAfter\":60}"
                );
            }
        }

        private String resolveClientKey(HttpServletRequest request) {
            // Use X-Forwarded-For (behind proxy/load balancer) or remote addr
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim(); // first IP in chain
            }
            return request.getRemoteAddr();
        }
    }

    // =========================================================================
    // Configuration — conditional on property, capacity configurable
    // =========================================================================

    @Configuration
    @Slf4j
    public static class RateLimitConfig {

        @Bean
        public BucketRegistry bucketRegistry(
                @Value("${phase3.day54.rate-limit.capacity:10}") long capacity,
                @Value("${phase3.day54.rate-limit.refill-per-minute:10}") long refillPerMinute) {
            return new BucketRegistry(capacity, refillPerMinute);
        }

        @Bean
        @ConditionalOnProperty("phase3.day54.rate-limit.enabled")
        public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilter(BucketRegistry registry) {
            var filter = new RateLimitingFilter(registry);
            var reg = new FilterRegistrationBean<>(filter);
            reg.addUrlPatterns("/api/day54/*");
            reg.setOrder(2); // run early, before business logic
            log.debug("[Day54] RateLimitingFilter registered for /api/day54/*");
            return reg;
        }
    }

    // =========================================================================
    // Demo controller
    // =========================================================================

    @RestController
    @RequestMapping("/api/day54")
    public static class PublicApiController {

        @GetMapping("/data")
        public Map<String, String> getData() {
            return Map.of("message", "data retrieved", "timestamp", java.time.Instant.now().toString());
        }
    }
}
