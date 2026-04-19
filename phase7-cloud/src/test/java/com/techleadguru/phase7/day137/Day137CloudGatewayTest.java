package com.techleadguru.phase7.day137;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class Day137CloudGatewayTest {

    @Test
    void testRewritePathWithNamedGroup() {
        // /api/orders/123 → /orders/123
        String result = Day137CloudGateway.rewritePath(
                "/api/orders/123",
                "/api/(?<segment>.*)",
                "/${segment}");
        assertEquals("/orders/123", result);
    }

    @Test
    void testRewritePathNoMatch() {
        String result = Day137CloudGateway.rewritePath(
                "/health",
                "/api/(?<segment>.*)",
                "/${segment}");
        assertEquals("/health", result); // unchanged when pattern doesn't match
    }

    @Test
    void testStripPrefix() {
        assertEquals("/orders/123", Day137CloudGateway.stripPrefix("/api/v1/orders/123", 2));
    }

    @Test
    void testStripPrefixZero() {
        assertEquals("/orders/123", Day137CloudGateway.stripPrefix("/orders/123", 0));
    }

    @Test
    void testMatchGlob() {
        assertTrue(Day137CloudGateway.matchesGlob("/api/**", "/api/orders/123"));
        assertTrue(Day137CloudGateway.matchesGlob("/api/**", "/api/users"));
        assertFalse(Day137CloudGateway.matchesGlob("/api/**", "/health"));
    }

    @Test
    void testMatchGlobSingleSegment() {
        assertTrue(Day137CloudGateway.matchesGlob("/users/*", "/users/alice"));
        assertFalse(Day137CloudGateway.matchesGlob("/users/*", "/users/alice/profile"));
    }

    @Test
    void testMatchRoute() {
        List<Day137CloudGateway.RouteDefinition> routes = Day137CloudGateway.sampleRoutes();
        Optional<Day137CloudGateway.RouteDefinition> match =
                Day137CloudGateway.matchRoute(routes, "/api/orders/123");
        assertTrue(match.isPresent());
        assertEquals("order-service", match.get().id());
    }

    @Test
    void testMatchRouteNoMatch() {
        List<Day137CloudGateway.RouteDefinition> routes = Day137CloudGateway.sampleRoutes();
        Optional<Day137CloudGateway.RouteDefinition> match =
                Day137CloudGateway.matchRoute(routes, "/unknown/path");
        assertTrue(match.isEmpty());
    }

    @Test
    void testCommonFilterFactories() {
        List<Day137CloudGateway.FilterFactory> factories = Day137CloudGateway.commonFilterFactories();
        assertEquals(6, factories.size());
    }

    @Test
    void testSampleRoutes() {
        List<Day137CloudGateway.RouteDefinition> routes = Day137CloudGateway.sampleRoutes();
        assertEquals(3, routes.size());
    }
}
