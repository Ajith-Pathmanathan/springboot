package com.techleadguru.phase7.day159;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day159StructuredLoggingTest {

    @AfterEach
    void clearMdc() {
        Day159StructuredLogging.MdcContext.clear();
    }

    @Test
    void testMdcPutAndGet() {
        Day159StructuredLogging.MdcContext.put("traceId", "t-abc");
        assertEquals("t-abc", Day159StructuredLogging.MdcContext.get("traceId"));
    }

    @Test
    void testMdcRemove() {
        Day159StructuredLogging.MdcContext.put("key", "val");
        Day159StructuredLogging.MdcContext.remove("key");
        assertNull(Day159StructuredLogging.MdcContext.get("key"));
    }

    @Test
    void testMdcCopyOfContextMap() {
        Day159StructuredLogging.MdcContext.put("traceId", "t1");
        Day159StructuredLogging.MdcContext.put("spanId", "s1");
        Map<String, String> copy = Day159StructuredLogging.MdcContext.getCopyOfContextMap();
        assertEquals("t1", copy.get("traceId"));
        assertEquals("s1", copy.get("spanId"));
    }

    @Test
    void testJsonLoggerCapturesEntries() {
        Day159StructuredLogging.MdcContext.put("traceId", "abc");
        Day159StructuredLogging.JsonLogger logger =
                new Day159StructuredLogging.JsonLogger("order-svc", "OrderService");
        logger.info("Order created");
        logger.error("Failed to process");

        List<Day159StructuredLogging.LogEntry> entries = logger.capturedEntries();
        assertEquals(2,       entries.size());
        assertEquals("INFO",  entries.get(0).level());
        assertEquals("ERROR", entries.get(1).level());
        assertEquals("abc",   entries.get(0).traceId());
    }

    @Test
    void testFormatAsJson() {
        Day159StructuredLogging.MdcContext.put("traceId", "t1");
        Day159StructuredLogging.JsonLogger logger =
                new Day159StructuredLogging.JsonLogger("svc", "SvcClass");
        logger.log("WARN", "test message", Map.of("orderId", "42"));
        String json = logger.formatAsJson(logger.capturedEntries().get(0));
        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("\"message\":\"test message\""));
        assertTrue(json.contains("\"orderId\""));
    }

    @Test
    void testLogFields() {
        List<Day159StructuredLogging.MdcField> fields =
                Day159StructuredLogging.logFields();
        assertEquals(6, fields.size());
        assertTrue(fields.stream().anyMatch(f -> f.key().equals("traceId")));
    }

    @Test
    void testStructuredLoggingProperties() {
        Map<String, String> props = Day159StructuredLogging.structuredLoggingProperties();
        assertFalse(props.isEmpty());
        assertTrue(props.containsKey("logging.level.root"));
    }
}
