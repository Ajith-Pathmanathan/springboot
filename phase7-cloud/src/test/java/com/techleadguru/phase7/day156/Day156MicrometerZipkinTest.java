package com.techleadguru.phase7.day156;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day156MicrometerZipkinTest {

    private Day156MicrometerZipkin.Span span(String traceId, String spanId, int durationMs) {
        Instant start = Instant.ofEpochMilli(1_000_000L);
        Instant end   = Instant.ofEpochMilli(1_000_000L + durationMs);
        return new Day156MicrometerZipkin.Span(
                traceId, spanId, null, "test-op", "my-service",
                start, end, Map.of());
    }

    @Test
    void testSpanDurationMicros() {
        Day156MicrometerZipkin.Span s = span("t1", "s1", 50);
        assertEquals(50_000L, s.durationMicros());
    }

    @Test
    void testTraceExporterForceBuffer() {
        Day156MicrometerZipkin.TraceExportConfig cfg =
                new Day156MicrometerZipkin.TraceExportConfig(
                        1.0, "http://zipkin:9411/api/v2/spans", "my-service");
        Day156MicrometerZipkin.TraceExporter exporter =
                new Day156MicrometerZipkin.TraceExporter(cfg);

        exporter.forceBuffer(span("t1", "s1", 10));
        exporter.forceBuffer(span("t1", "s2", 20));
        assertEquals(2, exporter.bufferSize());
    }

    @Test
    void testFlushExportsSpans() {
        Day156MicrometerZipkin.TraceExportConfig cfg =
                new Day156MicrometerZipkin.TraceExportConfig(
                        1.0, "http://zipkin:9411/api/v2/spans", "svc");
        Day156MicrometerZipkin.TraceExporter exporter =
                new Day156MicrometerZipkin.TraceExporter(cfg);
        exporter.forceBuffer(span("t1", "s1", 5));
        int flushed = exporter.flush();
        assertEquals(1, flushed);
        assertEquals(1, exporter.exportedCount());
        assertEquals(0, exporter.bufferSize());
        assertEquals(1, exporter.flushCount());
    }

    @Test
    void testSamplingStrategies() {
        List<Day156MicrometerZipkin.SamplingStrategy> strategies =
                Day156MicrometerZipkin.samplingStrategies();
        assertEquals(4, strategies.size());
    }

    @Test
    void testZipkinProperties() {
        Map<String, String> props =
                Day156MicrometerZipkin.zipkinProperties(
                        "http://zipkin:9411/api/v2/spans");
        assertEquals("http://zipkin:9411/api/v2/spans",
                props.get("management.zipkin.tracing.endpoint"));
        assertEquals("1.0",
                props.get("management.tracing.sampling.probability"));
    }
}
