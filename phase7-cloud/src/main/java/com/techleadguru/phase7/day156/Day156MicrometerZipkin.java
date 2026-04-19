package com.techleadguru.phase7.day156;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day 156 — Micrometer Tracing with Zipkin export
 *
 * Spring Boot auto-configuration:
 *   - Add micrometer-tracing-bridge-brave + zipkin-reporter-brave
 *   - Set management.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans
 *   - Set management.tracing.sampling.probability=1.0 (100% sampling)
 *
 * Zipkin UI: search by traceId, service name, operation, duration.
 */
public class Day156MicrometerZipkin {

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────

    public record TraceExportConfig(
            double samplingRate,       // 0.0 – 1.0
            String zipkinEndpoint,
            String serviceName) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Span model for export
    // ─────────────────────────────────────────────────────────────────────────

    public record Span(
            String  traceId,
            String  spanId,
            String  parentId,
            String  name,
            String  serviceName,
            Instant startTime,
            Instant endTime,
            Map<String, String> tags) {

        public long durationMicros() {
            return (endTime.toEpochMilli() - startTime.toEpochMilli()) * 1_000L;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trace exporter (in-memory buffer, simulating batch flush to Zipkin)
    // ─────────────────────────────────────────────────────────────────────────

    public static class TraceExporter {

        private final TraceExportConfig          config;
        private final List<Span>                 buffer   = new ArrayList<>();
        private final List<Span>                 exported = new ArrayList<>();
        private final AtomicInteger              flushCount = new AtomicInteger(0);

        public TraceExporter(TraceExportConfig config) {
            this.config = config;
        }

        public void buffer(Span span) {
            // Honour sampling rate
            if (Math.random() <= config.samplingRate()) {
                buffer.add(span);
            }
        }

        /** Always-buffer version for tests (bypasses sampling). */
        public void forceBuffer(Span span) {
            buffer.add(span);
        }

        /** Flush buffered spans to the "remote" Zipkin backend. */
        public int flush() {
            int count = buffer.size();
            exported.addAll(buffer);
            buffer.clear();
            flushCount.incrementAndGet();
            return count;
        }

        public List<Span> exportedSpans()     { return Collections.unmodifiableList(exported); }
        public int        bufferSize()         { return buffer.size(); }
        public int        exportedCount()      { return exported.size(); }
        public int        flushCount()         { return flushCount.get(); }
        public TraceExportConfig config()      { return config; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sampling strategies
    // ─────────────────────────────────────────────────────────────────────────

    public record SamplingStrategy(String name, String probability, String useCase) {}

    public static List<SamplingStrategy> samplingStrategies() {
        return List.of(
            new SamplingStrategy("Always sample",
                "1.0",
                "Development / debugging; low traffic environments"),
            new SamplingStrategy("Probabilistic (10%)",
                "0.1",
                "High-traffic production; captures statistically representative traces"),
            new SamplingStrategy("Rate-limiting",
                "N traces/sec regardless of traffic",
                "Predictable overhead; prevents storage explosion"),
            new SamplingStrategy("Tail-based",
                "1.0 for error traces, 0.01 for successful",
                "Ensure all errors captured while reducing success-trace noise")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Zipkin properties
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> zipkinProperties(String zipkinEndpoint) {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("management.zipkin.tracing.endpoint",         zipkinEndpoint);
        props.put("management.tracing.sampling.probability",    "1.0");
        props.put("management.tracing.enabled",                 "true");
        props.put("logging.pattern.level",
                  "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]");
        return props;
    }
}
