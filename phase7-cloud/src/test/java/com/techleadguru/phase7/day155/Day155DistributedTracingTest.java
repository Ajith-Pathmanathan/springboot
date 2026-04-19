package com.techleadguru.phase7.day155;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day155DistributedTracingTest {

    @Test
    void testSpanSimulatorDuration() throws InterruptedException {
        Day155DistributedTracing.TraceContext ctx =
                new Day155DistributedTracing.TraceContext(
                        "trace1", "span1", null, true);
        Day155DistributedTracing.SpanSimulator span =
                new Day155DistributedTracing.SpanSimulator("db-call", ctx);
        Thread.sleep(5);
        span.finish();
        assertTrue(span.isFinished());
        assertTrue(span.durationNanos() > 0);
    }

    @Test
    void testSpanSimulatorTags() {
        Day155DistributedTracing.TraceContext ctx =
                new Day155DistributedTracing.TraceContext("t1", "s1", null, true);
        Day155DistributedTracing.SpanSimulator span =
                new Day155DistributedTracing.SpanSimulator("http-call", ctx);
        span.addTag("http.method", "GET").addTag("http.status", "200");
        assertEquals("GET",  span.tags().get("http.method"));
        assertEquals("200",  span.tags().get("http.status"));
    }

    @Test
    void testTracePropagatorInjectExtract() {
        Day155DistributedTracing.TraceContext original =
                new Day155DistributedTracing.TraceContext(
                        "abc123", "def456", "parent789", true);
        Map<String, String> headers =
                Day155DistributedTracing.TracePropagator.inject(original);
        Day155DistributedTracing.TraceContext extracted =
                Day155DistributedTracing.TracePropagator.extract(headers);
        assertNotNull(extracted);
        assertEquals("abc123",   extracted.traceId());
        assertEquals("def456",   extracted.spanId());
        assertEquals("parent789", extracted.parentSpanId());
        assertTrue(extracted.sampled());
    }

    @Test
    void testExtractReturnsNullWithoutTraceId() {
        assertNull(Day155DistributedTracing.TracePropagator.extract(Map.of()));
    }

    @Test
    void testTracingHeaders() {
        Map<String, String> headers = Day155DistributedTracing.tracingHeaders();
        assertTrue(headers.containsKey("X-B3-TraceId"));
        assertTrue(headers.containsKey("traceparent"));
    }

    @Test
    void testOtelVsBrave() {
        List<Day155DistributedTracing.TracingComparison> comparisons =
                Day155DistributedTracing.otelVsBrave();
        assertFalse(comparisons.isEmpty());
        assertTrue(comparisons.stream().anyMatch(c -> c.aspect().equals("Propagation format")));
    }

    @Test
    void testValidationSteps() {
        List<Day155DistributedTracing.TracingComparison> comparisons =
                Day155DistributedTracing.otelVsBrave();
        assertEquals(5, comparisons.size());
    }
}
