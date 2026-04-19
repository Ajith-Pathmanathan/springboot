package com.techleadguru.phase7.day161;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day161HealthIndicatorTest {

    @Test
    void testDatabaseHealthCheckerUp() {
        Day161HealthIndicator.DatabaseHealthChecker checker =
                new Day161HealthIndicator.DatabaseHealthChecker(true, 5L);
        Day161HealthIndicator.HealthDetail detail = checker.check();
        assertEquals(Day161HealthIndicator.HealthStatus.UP, detail.status());
        assertTrue(detail.details().containsKey("validationQueryMs"));
    }

    @Test
    void testDatabaseHealthCheckerDown() {
        Day161HealthIndicator.DatabaseHealthChecker checker =
                new Day161HealthIndicator.DatabaseHealthChecker(false, 0L);
        Day161HealthIndicator.HealthDetail detail = checker.check();
        assertEquals(Day161HealthIndicator.HealthStatus.DOWN, detail.status());
        assertTrue(detail.details().containsKey("error"));
    }

    @Test
    void testCacheHealthCheckerUp() {
        Day161HealthIndicator.CacheHealthChecker checker =
                new Day161HealthIndicator.CacheHealthChecker(true, 0.95);
        Day161HealthIndicator.HealthDetail detail = checker.check();
        assertEquals(Day161HealthIndicator.HealthStatus.UP, detail.status());
    }

    @Test
    void testCacheHealthCheckerDown() {
        Day161HealthIndicator.CacheHealthChecker checker =
                new Day161HealthIndicator.CacheHealthChecker(false, 0.0);
        Day161HealthIndicator.HealthDetail detail = checker.check();
        assertEquals(Day161HealthIndicator.HealthStatus.DOWN, detail.status());
    }

    @Test
    void testCompositeHealthAllUp() {
        Day161HealthIndicator.CompositeHealth health = Day161HealthIndicator.aggregate(
                List.of(
                    new Day161HealthIndicator.DatabaseHealthChecker(true, 1L),
                    new Day161HealthIndicator.CacheHealthChecker(true, 0.9)
                )
        );
        assertEquals(Day161HealthIndicator.HealthStatus.UP, health.status());
        assertEquals(2, health.components().size());
    }

    @Test
    void testCompositeHealthDownWhenAnyDown() {
        Day161HealthIndicator.CompositeHealth health = Day161HealthIndicator.aggregate(
                List.of(
                    new Day161HealthIndicator.DatabaseHealthChecker(true, 1L),
                    new Day161HealthIndicator.CacheHealthChecker(false, 0.0)
                )
        );
        assertEquals(Day161HealthIndicator.HealthStatus.DOWN, health.status());
    }

    @Test
    void testHealthDetailUp() {
        Day161HealthIndicator.HealthDetail detail =
                Day161HealthIndicator.HealthDetail.up("db");
        assertEquals(Day161HealthIndicator.HealthStatus.UP, detail.status());
        assertEquals("db", detail.component());
    }

    @Test
    void testLivenessVsReadiness() {
        List<Day161HealthIndicator.ProbeComparison> comparisons =
                Day161HealthIndicator.livenessVsReadiness();
        assertEquals(5, comparisons.size());
        assertTrue(comparisons.stream()
                .anyMatch(c -> c.aspect().equals("On failure")));
    }

    @Test
    void testK8sProbeGuide() {
        List<String> guide = Day161HealthIndicator.k8sProbeGuide();
        assertEquals(7, guide.size());
    }
}
