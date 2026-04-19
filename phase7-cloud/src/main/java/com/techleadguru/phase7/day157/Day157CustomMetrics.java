package com.techleadguru.phase7.day157;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Day 157 — Custom Metrics with Micrometer
 *
 * Spring Boot auto-configures a MeterRegistry (Prometheus, Datadog, etc.).
 * Inject MeterRegistry into your beans and register meters.
 *
 * RED metrics:
 *   Rate      — requests per second
 *   Errors    — error rate
 *   Duration  — latency distribution
 */
public class Day157CustomMetrics {

    // ─────────────────────────────────────────────────────────────────────────
    // Meter types
    // ─────────────────────────────────────────────────────────────────────────

    public enum MetricType { COUNTER, TIMER, GAUGE, DISTRIBUTION_SUMMARY }

    public record MetricDefinition(
            String     name,
            MetricType type,
            String     description,
            List<String> tags) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Simple metric registry (in-memory — for testing/demo purposes)
    // ─────────────────────────────────────────────────────────────────────────

    public static class SimpleMetricRegistry {

        private final Map<String, AtomicLong>  counters     = new ConcurrentHashMap<>();
        private final Map<String, Double>       gauges       = new ConcurrentHashMap<>();
        private final Map<String, List<Long>>   timerSamples = new ConcurrentHashMap<>();

        // Counter
        public void incrementCounter(String name) { incrementCounter(name, 1); }
        public void incrementCounter(String name, long amount) {
            counters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(amount);
        }
        public long getCounter(String name) {
            AtomicLong c = counters.get(name);
            return c == null ? 0L : c.get();
        }

        // Gauge
        public void setGauge(String name, double value) { gauges.put(name, value); }
        public double getGauge(String name) { return gauges.getOrDefault(name, 0.0); }

        // Timer (record durations in nanoseconds)
        public void recordTimer(String name, long nanos) {
            timerSamples.computeIfAbsent(name, k -> new ArrayList<>()).add(nanos);
        }
        public long timerCount(String name) {
            List<Long> s = timerSamples.get(name);
            return s == null ? 0L : s.size();
        }
        public double timerMeanMs(String name) {
            List<Long> s = timerSamples.get(name);
            if (s == null || s.isEmpty()) return 0.0;
            return s.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
        }

        public Set<String> registeredCounters() { return counters.keySet(); }
        public Set<String> registeredGauges()   { return gauges.keySet(); }
        public Set<String> registeredTimers()   { return timerSamples.keySet(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recommended metrics (RED method)
    // ─────────────────────────────────────────────────────────────────────────

    public record RecommendedMetric(
            String component,
            String redDimension,
            MetricType type,
            String meterName,
            String description) {}

    public static List<RecommendedMetric> recommendedMetrics() {
        return List.of(
            new RecommendedMetric("HTTP Controller", "Rate",
                MetricType.COUNTER,
                "http.requests.total",
                "Total HTTP requests; tag by method, path, status"),
            new RecommendedMetric("HTTP Controller", "Errors",
                MetricType.COUNTER,
                "http.requests.errors",
                "HTTP 4xx/5xx responses; tag by status code"),
            new RecommendedMetric("HTTP Controller", "Duration",
                MetricType.TIMER,
                "http.requests.duration",
                "Latency distribution in ms; auto-registered by Spring Web"),
            new RecommendedMetric("DB Repository", "Rate",
                MetricType.COUNTER,
                "db.repository.calls",
                "Repository method invocation count"),
            new RecommendedMetric("DB Repository", "Duration",
                MetricType.TIMER,
                "db.repository.duration",
                "Time spent in repository calls"),
            new RecommendedMetric("Kafka Consumer", "Rate",
                MetricType.COUNTER,
                "kafka.consumer.messages.consumed",
                "Messages consumed per topic"),
            new RecommendedMetric("Kafka Consumer", "Errors",
                MetricType.COUNTER,
                "kafka.consumer.messages.errors",
                "Consumer processing failures"),
            new RecommendedMetric("Circuit Breaker", "Errors",
                MetricType.GAUGE,
                "resilience4j.circuitbreaker.state",
                "Current state (0=CLOSED, 1=OPEN, 2=HALF_OPEN)"),
            new RecommendedMetric("JVM", "Gauge",
                MetricType.GAUGE,
                "jvm.memory.used",
                "Heap memory used (auto-registered by Spring Boot)")
        );
    }
}
