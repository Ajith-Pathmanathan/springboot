package com.techleadguru.phase7.day137;

import java.util.*;
import java.util.regex.*;

/**
 * Day 137 — Spring Cloud Gateway: routing + URI rewrite.
 *
 * Gateway is built on Spring WebFlux (reactive / non-blocking).
 * Every request passes through a chain of GlobalFilters and route-specific GatewayFilters.
 *
 * Core concept: Route = Predicate + Filter chain + destination URI
 *
 * Common predicates: Path, Method, Header, Host, Query, Cookie, After/Before/Between
 * Common filter factories: RewritePath, StripPrefix, AddRequestHeader, AddResponseHeader,
 *                          CircuitBreaker, RateLimiter, RequestRateLimiter
 *
 * Request flow:
 *   Client → Gateway → RoutePredicateHandlerMapping
 *          → FilteringWebHandler → [GlobalFilters + GatewayFilters]
 *          → Proxied to origin service
 */
public class Day137CloudGateway {

    // ─────────────────────────────────────────────────────────────────────────
    // Route model
    // ─────────────────────────────────────────────────────────────────────────

    public record RouteDefinition(
            String id,
            String uri,            // destination e.g. lb://order-service
            String pathPredicate,  // e.g. /api/orders/**
            List<String> filters) {}

    // ─────────────────────────────────────────────────────────────────────────
    // RewritePath simulation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simulates the RewritePath filter:
     *   RewritePath=/api/(?<segment>.*), /${segment}
     *
     * Strips the /api prefix before forwarding to the backend.
     */
    public static String rewritePath(String requestPath, String regexPattern, String replacement) {
        Pattern p = Pattern.compile(regexPattern);
        Matcher m = p.matcher(requestPath);
        if (m.matches()) {
            String result = replacement;
            // Named groups: replace ${name} with matched group
            Matcher namedGroups = Pattern.compile("\\$\\{(\\w+)}").matcher(replacement);
            StringBuffer sb = new StringBuffer();
            while (namedGroups.find()) {
                String groupName = namedGroups.group(1);
                try {
                    String groupVal = m.group(groupName);
                    namedGroups.appendReplacement(sb, groupVal != null ? groupVal : "");
                } catch (IllegalArgumentException e) {
                    namedGroups.appendReplacement(sb, "");
                }
            }
            namedGroups.appendTail(sb);
            return sb.toString();
        }
        return requestPath;
    }

    /**
     * Simulates the StripPrefix filter (StripPrefix=1 removes first path segment).
     */
    public static String stripPrefix(String path, int segments) {
        String[] parts = path.split("/", -1);
        // parts[0] is empty (path starts with /)
        if (segments >= parts.length - 1) return "/";
        return "/" + String.join("/", Arrays.copyOfRange(parts, segments + 1, parts.length));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Route matcher
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns the first route whose pathPredicate glob matches the given path. */
    public static Optional<RouteDefinition> matchRoute(
            List<RouteDefinition> routes, String path) {
        return routes.stream()
                .filter(r -> matchesGlob(r.pathPredicate(), path))
                .findFirst();
    }

    static boolean matchesGlob(String pattern, String path) {
        // Convert glob pattern (/api/**) to regex.
        // Must handle ** before * to avoid corrupting the .* replacement.
        String regex = pattern
                .replace(".", "\\.")
                .replace("**", "\u0000STAR\u0000") // placeholder for **
                .replace("*", "[^/]*")
                .replace("\u0000STAR\u0000", ".*");
        return path.matches(regex);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reference data
    // ─────────────────────────────────────────────────────────────────────────

    public record FilterFactory(String name, String example, String effect) {}

    public static List<FilterFactory> commonFilterFactories() {
        return List.of(
            new FilterFactory("RewritePath",
                "RewritePath=/api/(?<s>.*), /${s}",
                "Rewrites the downstream path using regex capture groups"),
            new FilterFactory("StripPrefix",
                "StripPrefix=1",
                "Removes N leading path segments before forwarding"),
            new FilterFactory("AddRequestHeader",
                "AddRequestHeader=X-Source, gateway",
                "Adds a header to every forwarded request"),
            new FilterFactory("AddResponseHeader",
                "AddResponseHeader=X-Response-Time, #{T(System).currentTimeMillis()}",
                "Adds a header to every response"),
            new FilterFactory("CircuitBreaker",
                "CircuitBreaker=name=myCircuitBreaker,fallbackUri=/fallback",
                "Wraps route with Resilience4j circuit breaker"),
            new FilterFactory("RequestRateLimiter",
                "RequestRateLimiter=redis-rate-limiter.replenishRate=10",
                "Rate-limits requests per key using Redis token bucket")
        );
    }

    public static List<RouteDefinition> sampleRoutes() {
        return List.of(
            new RouteDefinition("order-service",
                "lb://order-service",
                "/api/orders/**",
                List.of("StripPrefix=1")),
            new RouteDefinition("user-service",
                "lb://user-service",
                "/api/users/**",
                List.of("RewritePath=/api/(?<segment>.*), /${segment}")),
            new RouteDefinition("product-service",
                "lb://product-service",
                "/api/products/**",
                List.of("StripPrefix=1", "AddRequestHeader=X-Source, gateway"))
        );
    }
}
