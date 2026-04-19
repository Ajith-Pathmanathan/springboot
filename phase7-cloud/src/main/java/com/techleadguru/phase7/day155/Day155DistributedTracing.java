package com.techleadguru.phase7.day155;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Day 155 — Distributed Tracing (Micrometer Tracing + Brave/OpenTelemetry)
 *
 * Key concepts:
 *   Trace  — represents an end-to-end request across services (shared traceId)
 *   Span   — a single unit of work within a trace (has own spanId + parentSpanId)
 *   Context propagation — trace headers forwarded between services via HTTP/messaging
 *
 * Spring Boot 3 uses Micrometer Tracing (facade) backed by:
 *   - Brave (Zipkin format, B3 propagation)
 *   - OpenTelemetry (OTLP format, W3C TraceContext)
 */
public class Day155DistributedTracing {

    // ─────────────────────────────────────────────────────────────────────────
    // Trace context
    // ─────────────────────────────────────────────────────────────────────────

    public record TraceContext(
            String traceId,       // 64 or 128-bit hex
            String spanId,        // 64-bit hex
            String parentSpanId,  // null for root span
            boolean sampled) {}   // whether to export this trace

    // ─────────────────────────────────────────────────────────────────────────
    // Span simulator
    // ─────────────────────────────────────────────────────────────────────────

    public static class SpanSimulator {

        private final String              name;
        private final TraceContext        context;
        private final Map<String, String> tags       = new LinkedHashMap<>();
        private final long                startNanos = System.nanoTime();
        private long                      endNanos   = -1L;

        public SpanSimulator(String name, TraceContext context) {
            this.name    = name;
            this.context = context;
        }

        public SpanSimulator addTag(String key, String value) {
            tags.put(key, value);
            return this;
        }

        public void finish() {
            endNanos = System.nanoTime();
        }

        public long durationNanos() {
            if (endNanos < 0) return System.nanoTime() - startNanos;
            return endNanos - startNanos;
        }

        public String name()           { return name; }
        public TraceContext context()   { return context; }
        public Map<String, String> tags() { return Collections.unmodifiableMap(tags); }
        public boolean isFinished()    { return endNanos >= 0; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // B3 propagation (inject / extract HTTP headers)
    // ─────────────────────────────────────────────────────────────────────────

    public static final String HEADER_TRACE_ID  = "X-B3-TraceId";
    public static final String HEADER_SPAN_ID   = "X-B3-SpanId";
    public static final String HEADER_PARENT_ID = "X-B3-ParentSpanId";
    public static final String HEADER_SAMPLED   = "X-B3-Sampled";

    public static class TracePropagator {

        /** Inject trace context into carrier (HTTP headers / Kafka headers). */
        public static Map<String, String> inject(TraceContext ctx) {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put(HEADER_TRACE_ID, ctx.traceId());
            headers.put(HEADER_SPAN_ID,  ctx.spanId());
            if (ctx.parentSpanId() != null) {
                headers.put(HEADER_PARENT_ID, ctx.parentSpanId());
            }
            headers.put(HEADER_SAMPLED, ctx.sampled() ? "1" : "0");
            return headers;
        }

        /** Extract trace context from incoming headers. Returns null if no traceId. */
        public static TraceContext extract(Map<String, String> headers) {
            String traceId = headers.get(HEADER_TRACE_ID);
            if (traceId == null) return null;
            return new TraceContext(
                    traceId,
                    headers.getOrDefault(HEADER_SPAN_ID, ""),
                    headers.get(HEADER_PARENT_ID),
                    "1".equals(headers.get(HEADER_SAMPLED)));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Standard tracing headers
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> tracingHeaders() {
        return Map.of(
            "X-B3-TraceId",      "128-bit trace identifier (hex); shared across all spans in a trace",
            "X-B3-SpanId",       "64-bit span identifier (hex); unique per operation",
            "X-B3-ParentSpanId", "SpanId of the calling span; absent for root span",
            "X-B3-Sampled",      "1=export this trace to Zipkin/Jaeger, 0=skip",
            "traceparent",       "W3C TraceContext header: 00-{traceId}-{parentId}-{flags}"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Brave vs OpenTelemetry
    // ─────────────────────────────────────────────────────────────────────────

    public record TracingComparison(String aspect, String brave, String otel) {}

    public static List<TracingComparison> otelVsBrave() {
        return List.of(
            new TracingComparison("Propagation format",
                "B3 (single or multi-header)",
                "W3C TraceContext (traceparent header)"),
            new TracingComparison("Export format",
                "Zipkin JSON or Thrift",
                "OTLP (gRPC or HTTP/Protobuf)"),
            new TracingComparison("Backend",
                "Zipkin",
                "Jaeger, Tempo, Honeycomb, OTLP-compatible"),
            new TracingComparison("Micrometer bridge",
                "micrometer-tracing-bridge-brave",
                "micrometer-tracing-bridge-otel"),
            new TracingComparison("Spring Boot 3 default",
                "Brave (included in Spring Boot 3 AC)",
                "Manual: replace brave with otel bridge and exporter")
        );
    }
}
