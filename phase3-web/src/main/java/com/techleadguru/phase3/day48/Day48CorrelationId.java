package com.techleadguru.phase3.day48;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * DAY 48 — OncePerRequestFilter + MDC Correlation ID
 *
 * THE PROBLEM:
 *   A user reports "my order failed at 14:23". You search logs... 10,000 lines.
 *   How do you find the specific request's log lines among all concurrent requests?
 *
 * THE SOLUTION: Correlation ID + MDC (Mapped Diagnostic Context)
 *   1. Client sends (or server generates): X-Request-ID: 550e8400-e29b-41d4-a716-446655440000
 *   2. A filter injects this into SLF4J's MDC (thread-local map)
 *   3. Logback/Log4j pattern includes %X{requestId} in every log line
 *   4. ALL log lines for this request share the same requestId
 *   5. Search "requestId=550e8400..." → immediately find all lines for that request
 *
 * MDC (Mapped Diagnostic Context):
 *   - Thread-local key-value store provided by SLF4J
 *   - Values injected with MDC.put("key", "value")
 *   - Referenced in log pattern as %X{key}
 *   - MUST be cleared with MDC.clear() after request (prevents leaks with thread pools!)
 *
 * LOGBACK PATTERN (log4j/logback):
 *   %d{yyyy-MM-dd HH:mm:ss} [%thread] [requestId=%X{requestId}] %-5level — %msg%n
 *
 *   Result: 2024-01-15 14:23:01 [http-nio-8080-exec-5] [requestId=550e8400...] DEBUG — Order ORD-42 created
 *
 * OncePerRequestFilter:
 *   Guarantees doFilterInternal() is called exactly once per request.
 *   Spring's base class that prevents double-execution in forward/include scenarios.
 *   Always extend this instead of implementing Filter directly.
 *
 * PRODUCTION NOTE:
 *   For async (@Async) or reactive (WebFlux), MDC must be manually propagated
 *   since threads change. Spring Sleuth / Micrometer Tracing handles this automatically.
 */
@Slf4j
public class Day48CorrelationId {

    public static final String HEADER_REQUEST_ID = "X-Request-ID";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";

    // =========================================================================
    // The filter — injects correlation ID into MDC for every request
    // =========================================================================

    @Slf4j
    public static class CorrelationIdFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            // Use provided ID or generate a new one
            String requestId = request.getHeader(HEADER_REQUEST_ID);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }

            String userId = request.getHeader("X-User-Id");

            // Inject into MDC — visible in ALL log statements on this thread
            MDC.put(MDC_REQUEST_ID, requestId);
            if (userId != null) {
                MDC.put(MDC_USER_ID, userId);
            }

            // Also echo back the requestId in response — clients can correlate errors
            response.setHeader(HEADER_REQUEST_ID, requestId);

            log.debug("[Day48] Request started: {} {} | requestId={}",
                    request.getMethod(), request.getRequestURI(), requestId);

            try {
                filterChain.doFilter(request, response);
            } finally {
                // CRITICAL: clear MDC after request to prevent thread pool leaks
                MDC.remove(MDC_REQUEST_ID);
                MDC.remove(MDC_USER_ID);
                log.debug("[Day48] Request complete — MDC cleared");
            }
        }
    }

    // =========================================================================
    // Configuration — registers the filter as a Spring-managed bean
    // =========================================================================

    @Configuration
    @Slf4j
    public static class CorrelationIdConfig {

        @Bean
        public CorrelationIdFilter correlationIdFilter() {
            log.debug("[Day48] CorrelationIdFilter registered");
            return new CorrelationIdFilter();
        }
    }

    // =========================================================================
    // Demo controller
    // =========================================================================

    @RestController
    @RequestMapping("/api/day48")
    @Slf4j
    public static class MdcDemoController {

        @GetMapping("/request-info")
        public RequestInfo getRequestInfo(HttpServletRequest request) {
            // MDC values are available throughout the request thread
            String requestId = MDC.get(MDC_REQUEST_ID);
            String userId = MDC.get(MDC_USER_ID);

            log.info("[Day48] Processing request — all log lines on this thread have requestId={}", requestId);
            return new RequestInfo(requestId, userId, request.getMethod(), request.getRequestURI());
        }
    }

    public record RequestInfo(String requestId, String userId, String method, String uri) {}
}
