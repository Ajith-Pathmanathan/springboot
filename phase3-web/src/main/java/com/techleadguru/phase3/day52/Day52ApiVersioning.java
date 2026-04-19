package com.techleadguru.phase3.day52;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * DAY 52 — API Versioning: URI, Header, and Content-Type Strategies
 *
 * WHY API VERSIONING MATTERS:
 *   Once an API is live, clients depend on it. Breaking changes (removing fields,
 *   changing types, renaming fields) break clients. Versioning lets you evolve
 *   the API while supporting old clients during their migration window.
 *
 * THREE STRATEGIES:
 *
 *   1. URI VERSIONING: /api/v1/orders vs /api/v2/orders
 *      ✓ Simple — visible in URL, easy to test in browser
 *      ✓ Can route to different service versions (Nginx, API gateway)
 *      ✗ "Impure" REST — resource location shouldn't change with version
 *      Used by: Twitter, Stripe, Twilio
 *
 *   2. HEADER VERSIONING: X-API-Version: 2
 *      ✓ URL stays clean: /api/orders/{id}
 *      ✓ Can version at the field level inside the same resource
 *      ✗ Harder to test in browser (need curl/Postman)
 *      ✗ Can't bookmark/cache versioned URL directly
 *      Used by: GitHub API
 *
 *   3. ACCEPT HEADER (Content-Type versioning):
 *      Accept: application/vnd.myapp.v2+json
 *      ✓ Truly RESTful — content negotiation built into HTTP spec
 *      ✗ Most complex to implement and test
 *      ✗ Proxy/CDN caching complications
 *      Used by: GitHub (also supports this)
 *
 * TECH LEAD RECOMMENDATION:
 *   - Public APIs: URI versioning (simplest for clients)
 *   - Internal microservices: Header versioning (cleaner URLs, easy in service mesh)
 *   - Evolving incrementally: add new fields (backward-compatible), only bump version
 *     when making breaking changes
 *   - Support at least 1 old version after deprecating (6-month sunset window)
 */
@Slf4j
public class Day52ApiVersioning {

    // =========================================================================
    // V1 response — basic fields
    // =========================================================================
    public record OrderSummaryV1(String id, String status, BigDecimal total) {}

    // =========================================================================
    // V2 response — V1 fields + analytics enrichment
    // =========================================================================
    public record OrderSummaryV2(
            String id,
            String status,
            BigDecimal total,
            LocalDate estimatedDelivery,
            int loyaltyPointsEarned,
            String regionCode
    ) {}

    private static final Map<String, OrderSummaryV2> ORDERS = Map.of(
            "ORD-A", new OrderSummaryV2("ORD-A", "SHIPPED", BigDecimal.valueOf(149.99),
                    LocalDate.now().plusDays(3), 150, "US-WEST"),
            "ORD-B", new OrderSummaryV2("ORD-B", "DELIVERED", BigDecimal.valueOf(79.50),
                    LocalDate.now().minusDays(1), 80, "EU-CENTRAL")
    );

    // =========================================================================
    // Strategy 1: URI Versioning
    // /api/day52/v1/orders/{id} — limited response
    // /api/day52/v2/orders/{id} — enriched response
    // =========================================================================

    @RestController
    @RequestMapping("/api/day52/v1/orders")
    @Slf4j
    public static class OrderControllerV1 {

        @GetMapping("/{id}")
        public OrderSummaryV1 getOrder(@PathVariable String id) {
            OrderSummaryV2 full = ORDERS.get(id);
            if (full == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            log.debug("[Day52] V1 GET /v1/orders/{} — returning basic summary", id);
            return new OrderSummaryV1(full.id(), full.status(), full.total());
        }
    }

    @RestController
    @RequestMapping("/api/day52/v2/orders")
    @Slf4j
    public static class OrderControllerV2 {

        @GetMapping("/{id}")
        public OrderSummaryV2 getOrder(@PathVariable String id) {
            OrderSummaryV2 order = ORDERS.get(id);
            if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            log.debug("[Day52] V2 GET /v2/orders/{} — returning enriched summary", id);
            return order;
        }
    }

    // =========================================================================
    // Strategy 2: Header Versioning (X-API-Version)
    // Single URL, version selected from header
    // =========================================================================

    @RestController
    @RequestMapping("/api/day52/orders")
    @Slf4j
    public static class OrderControllerHeaderVersioned {

        @GetMapping("/{id}")
        public Object getOrder(
                @PathVariable String id,
                @RequestHeader(value = "X-API-Version", defaultValue = "1") int version) {

            OrderSummaryV2 full = ORDERS.get(id);
            if (full == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

            log.debug("[Day52] Header-versioned GET /orders/{} version={}", id, version);

            return switch (version) {
                case 1  -> new OrderSummaryV1(full.id(), full.status(), full.total());
                case 2  -> full;
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported API version: " + version + ". Supported: 1, 2");
            };
        }
    }
}
