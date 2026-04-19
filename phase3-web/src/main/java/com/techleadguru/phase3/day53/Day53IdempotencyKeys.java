package com.techleadguru.phase3.day53;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAY 53 — Idempotency Keys for POST Requests
 *
 * THE PROBLEM: Network unreliability
 *   Client sends POST /api/payments with amount=$100.
 *   Network times out. Did the payment go through?
 *   Client retries. Payment is charged TWICE. 💥
 *
 * THE SOLUTION: Idempotency Keys
 *   Client generates a unique key (UUID) for each intended operation.
 *   Key is sent as: X-Idempotency-Key: 550e-8400-...
 *   Server stores: key → response
 *   On retry with the same key → server returns the CACHED response. No double-charge.
 *   Key expires after a TTL (24 hours typical).
 *
 * WHICH METHODS NEED IDEMPOTENCY KEYS?
 *   GET, HEAD, OPTIONS  → naturally idempotent (read-only)
 *   PUT, DELETE         → idempotent by definition (same result on repeat)
 *   POST                → NOT idempotent → needs idempotency key
 *   PATCH               → depends on operation (relative vs absolute)
 *
 * IMPLEMENTATION OPTIONS:
 *   a) Filter intercepting at HTTP layer (this demo)
 *   b) AOP @Around advice on service methods
 *   c) Database-level idempotency (transactions with unique key constraint)
 *
 * STORAGE:
 *   Development: ConcurrentHashMap (in-memory, single node)
 *   Production: Redis with TTL expiration (survives restarts, works across instances)
 *     RedisTemplate.opsForValue().setIfAbsent(key, processing_placeholder, 24h)
 *     → If returns false → key already claimed → wait and return cached result
 *
 * BEST PRACTICE: The "processing" sentinel
 *   On first request: store key → "PROCESSING" immediately
 *   After completion: store key → actual response
 *   On concurrent retry: see "PROCESSING" → wait with backoff
 *   This prevents the "two concurrent identical requests" race condition.
 */
@Slf4j
public class Day53IdempotencyKeys {

    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    // =========================================================================
    // In-memory idempotency store (use Redis in production)
    // =========================================================================

    public static class IdempotencyStore {

        public record CachedResponse(int status, String body, String contentType) {}

        private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

        public boolean hasKey(String key) {
            return cache.containsKey(key);
        }

        public CachedResponse get(String key) {
            return cache.get(key);
        }

        public void store(String key, CachedResponse response) {
            cache.put(key, response);
            log.debug("[Day53] Stored idempotency key: {} → status={}", key, response.status());
        }

        public int size() {
            return cache.size();
        }
    }

    // =========================================================================
    // Filter — intercepts POST requests, checks/stores idempotency key
    // =========================================================================

    @Slf4j
    public static class IdempotencyFilter extends OncePerRequestFilter {

        private final IdempotencyStore store;

        public IdempotencyFilter(IdempotencyStore store) {
            this.store = store;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            // Only apply to POST requests
            if (!"POST".equals(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);

            if (key == null || key.isBlank()) {
                response.sendError(HttpStatus.BAD_REQUEST.value(),
                        "X-Idempotency-Key header is required for POST requests");
                return;
            }

            // Check cache — if key seen before, return cached response
            if (store.hasKey(key)) {
                IdempotencyStore.CachedResponse cached = store.get(key);
                log.info("[Day53] Idempotent replay for key={} → returning cached status={}",
                        key, cached.status());

                response.setStatus(cached.status());
                response.setContentType(cached.contentType());
                response.setHeader("X-Idempotent-Replayed", "true");
                response.setHeader(IDEMPOTENCY_KEY_HEADER, key);
                response.getOutputStream().write(cached.body().getBytes(StandardCharsets.UTF_8));
                return;
            }

            // New request — wrap response to capture the response body
            CachedResponseWrapper wrapper = new CachedResponseWrapper(response);
            filterChain.doFilter(request, wrapper);

            // Cache the response for future retries
            byte[] capturedBytes = wrapper.getCapturedBytes();
            String body = wrapper.getCapturedBody();
            store.store(key, new IdempotencyStore.CachedResponse(
                    wrapper.getStatus(),
                    body,
                    wrapper.getContentType() != null ? wrapper.getContentType() : "application/json"
            ));

            // Write the actual response — use getOutputStream() since Jackson writes bytes, not chars
            if (wrapper.getContentType() != null) {
                response.setContentType(wrapper.getContentType());
            }
            response.setHeader(IDEMPOTENCY_KEY_HEADER, key);
            response.getOutputStream().write(capturedBytes);
        }
    }

    // =========================================================================
    // Response wrapper to capture body written via getOutputStream() or getWriter()
    // =========================================================================

    static class CachedResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
        private final ByteArrayOutputStream capturedContent = new ByteArrayOutputStream();
        private int status = 200;
        private String contentType;

        CachedResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        /** Jackson uses getOutputStream() to write JSON bytes — must capture it. */
        @Override
        public ServletOutputStream getOutputStream() {
            return new DelegatingServletOutputStream(capturedContent);
        }

        /** Some converters use getWriter() — route through ByteArrayOutputStream too. */
        @Override
        public java.io.PrintWriter getWriter() throws java.io.UnsupportedEncodingException {
            String enc = getCharacterEncoding();
            return new java.io.PrintWriter(
                    new java.io.OutputStreamWriter(capturedContent, enc != null ? enc : "UTF-8"));
        }

        @Override
        public void setStatus(int sc) {
            super.setStatus(sc);
            this.status = sc;
        }

        @Override
        public void setContentType(String type) {
            super.setContentType(type);
            this.contentType = type;
        }

        public int getStatus() { return status; }
        public byte[] getCapturedBytes() { return capturedContent.toByteArray(); }
        public String getCapturedBody() { return capturedContent.toString(StandardCharsets.UTF_8); }
        public String getContentType() { return contentType; }
    }

    /** Delegates OutputStream writes to a ByteArrayOutputStream for capture. */
    static class DelegatingServletOutputStream extends ServletOutputStream {
        private final OutputStream delegate;

        DelegatingServletOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override public boolean isReady() { return true; }
        @Override public void setWriteListener(WriteListener wl) {}
        @Override public void write(int b) throws IOException { delegate.write(b); }
        @Override public void write(byte[] b, int off, int len) throws IOException { delegate.write(b, off, len); }
        @Override public void flush() throws IOException { delegate.flush(); }
    }

    // =========================================================================
    // Configuration — registers filter and store bean (conditional)
    // =========================================================================

    @Configuration
    @Slf4j
    public static class IdempotencyConfig {

        @Bean
        public IdempotencyStore idempotencyStore() {
            return new IdempotencyStore();
        }

        @Bean
        @ConditionalOnProperty("phase3.day53.idempotency.enabled")
        public FilterRegistrationBean<IdempotencyFilter> idempotencyFilter(IdempotencyStore store) {
            var filter = new IdempotencyFilter(store);
            var reg = new FilterRegistrationBean<>(filter);
            reg.addUrlPatterns("/api/day53/*");
            reg.setOrder(5);
            log.debug("[Day53] IdempotencyFilter registered for /api/day53/*");
            return reg;
        }
    }

    // =========================================================================
    // Payment controller
    // =========================================================================

    @RestController
    @RequestMapping("/api/day53/payments")
    @Slf4j
    public static class PaymentsController {

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public PaymentResponse processPayment(@RequestBody CreatePaymentRequest req) {
            String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("[Day53] Processing payment: {} amount={}", paymentId, req.amount());
            return new PaymentResponse(paymentId, req.customerId(), req.amount(), "COMPLETED");
        }
    }

    public record CreatePaymentRequest(String customerId, BigDecimal amount) {}

    public record PaymentResponse(String paymentId, String customerId,
                                   BigDecimal amount, String status) {}
}
