package com.techleadguru.phase7.day140;

import java.time.Instant;
import java.util.*;

/**
 * Day 140 — Gateway global exception handling
 *
 * Spring Cloud Gateway replaces the default Whitelabel error page with a
 * reactive ErrorWebExceptionHandler (WebFlux). All exceptions are caught and
 * converted to a consistent JSON error response.
 *
 * Bean: @Primary @Order(-1) GlobalErrorHandler implements ErrorWebExceptionHandler
 */
public class Day140GatewayExceptionHandling {

    // ─────────────────────────────────────────────────────────────────────────
    // Standard error envelope
    // ─────────────────────────────────────────────────────────────────────────

    public record ErrorResponse(
            String  timestamp,   // ISO-8601
            int     status,
            String  error,       // HTTP reason phrase
            String  path,
            String  traceId) {

        public static ErrorResponse of(int status, String error, String path, String traceId) {
            return new ErrorResponse(Instant.now().toString(), status, error, path, traceId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build response from exception type
    // ─────────────────────────────────────────────────────────────────────────

    public static ErrorResponse buildErrorResponse(Exception ex, String path, String traceId) {
        int status = exceptionToStatus(ex);
        String error = httpReasonPhrase(status) + ": " + ex.getMessage();
        return ErrorResponse.of(status, error, path, traceId);
    }

    public static int exceptionToStatus(Exception ex) {
        return HTTP_STATUS_MAP.entrySet().stream()
                .filter(e -> e.getKey().isInstance(ex))
                .mapToInt(Map.Entry::getValue)
                .findFirst()
                .orElse(500);
    }

    // Map exception class to HTTP status
    private static final Map<Class<? extends Exception>, Integer> HTTP_STATUS_MAP =
            buildStatusMap();

    private static Map<Class<? extends Exception>, Integer> buildStatusMap() {
        Map<Class<? extends Exception>, Integer> m = new LinkedHashMap<>();
        m.put(IllegalArgumentException.class,         400);
        m.put(SecurityException.class,                401);
        m.put(AccessDeniedException.class,            403);
        m.put(NoSuchElementException.class,           404);
        m.put(UnsupportedOperationException.class,   405);
        m.put(RuntimeException.class,                 500);
        m.put(Exception.class,                        500);
        return m;
    }

    /** Minimal marker exception for 403. */
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String msg) { super(msg); }
    }

    public static String httpReasonPhrase(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 429 -> "Too Many Requests";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default  -> "Internal Server Error";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Handler documentation
    // ─────────────────────────────────────────────────────────────────────────

    public record ExceptionHandlerGuide(
            String exceptionClass,
            int    httpStatus,
            String resolution) {}

    public static List<ExceptionHandlerGuide> handlerGuide() {
        return List.of(
            new ExceptionHandlerGuide(
                "ResponseStatusException",      400,
                "Thrown by WebFlux controllers — use its HTTP status and reason"),
            new ExceptionHandlerGuide(
                "ConnectException",             503,
                "Downstream service unreachable — return 503 Service Unavailable"),
            new ExceptionHandlerGuide(
                "TimeoutException",             504,
                "Upstream timeout — return 504 Gateway Timeout"),
            new ExceptionHandlerGuide(
                "CallNotPermittedException",    503,
                "Resilience4j circuit OPEN — return 503 with Retry-After header"),
            new ExceptionHandlerGuide(
                "RequestRateLimiterException",  429,
                "Token bucket exhausted — return 429 Too Many Requests with limits in headers")
        );
    }
}
