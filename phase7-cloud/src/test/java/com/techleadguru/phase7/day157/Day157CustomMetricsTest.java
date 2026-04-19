package com.techleadguru.phase7.day157;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day157CustomMetricsTest {

    @Test
    void testCounterIncrements() {
        Day157CustomMetrics.SimpleMetricRegistry registry =
                new Day157CustomMetrics.SimpleMetricRegistry();
        registry.incrementCounter("http.requests");
        registry.incrementCounter("http.requests");
        assertEquals(2L, registry.getCounter("http.requests"));
    }

    @Test
    void testCounterIncrementByAmount() {
        Day157CustomMetrics.SimpleMetricRegistry registry =
                new Day157CustomMetrics.SimpleMetricRegistry();
        registry.incrementCounter("events", 5);
        assertEquals(5L, registry.getCounter("events"));
    }

    @Test
    void testCounterDefaultsToZero() {
        Day157CustomMetrics.SimpleMetricRegistry registry =
                new Day157CustomMetrics.SimpleMetricRegistry();
        assertEquals(0L, registry.getCounter("nonexistent"));
    }

    @Test
    void testGaugeSetAndGet() {
        Day157CustomMetrics.SimpleMetricRegistry registry =
                new Day157CustomMetrics.SimpleMetricRegistry();
        registry.setGauge("jvm.memory", 512.0);
        assertEquals(512.0, registry.getGauge("jvm.memory"), 1e-9);
    }

    @Test
    void testGaugeDefaultsToZero() {
        Day157CustomMetrics.SimpleMetricRegistry registry =
                new Day157CustomMetrics.SimpleMetricRegistry();
        assertEquals(0.0, registry.getGauge("none"), 1e-9);
    }

    @Test
    void testTimerRecordAndMean() {
        Day157CustomMetrics.SimpleMetricRegistry registry =
                new Day157CustomMetrics.SimpleMetricRegistry();
        registry.recordTimer("db.call", 10_000_000L); // 10ms
        registry.recordTimer("db.call", 20_000_000L); // 20ms
        assertEquals(2, registry.timerCount("db.call"));
        assertEquals(15.0, registry.timerMeanMs("db.call"), 1e-3);
    }

    @Test
    void testRegisteredKeys() {
        Day157CustomMetrics.SimpleMetricRegistry registry =
                new Day157CustomMetrics.SimpleMetricRegistry();
        registry.incrementCounter("c1");
        registry.setGauge("g1", 1.0);
        registry.recordTimer("t1", 1L);
        assertTrue(registry.registeredCounters().contains("c1"));
        assertTrue(registry.registeredGauges().contains("g1"));
        assertTrue(registry.registeredTimers().contains("t1"));
    }

    @Test
    void testRecommendedMetrics() {
        List<Day157CustomMetrics.RecommendedMetric> metrics =
                Day157CustomMetrics.recommendedMetrics();
        assertFalse(metrics.isEmpty());
        assertTrue(metrics.stream().anyMatch(
                m -> m.redDimension().equals("Rate") && m.component().equals("HTTP Controller")));
    }
}
