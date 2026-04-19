package com.techleadguru.phase7.day149;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day149CloudStreamTest {

    @Test
    void testMessageEnvelopeOf() {
        Day149CloudStream.MessageEnvelope env =
                Day149CloudStream.MessageEnvelope.of("hello");
        assertEquals("hello", env.payload());
        assertNotNull(env.headers());
    }

    @Test
    void testMessageEnvelopeWithHeaders() {
        Map<String, Object> headers = Map.of("contentType", "application/json");
        Day149CloudStream.MessageEnvelope env =
                Day149CloudStream.MessageEnvelope.of("payload", headers);
        assertEquals("application/json", env.headers().get("contentType"));
    }

    @Test
    void testBindingProperties() {
        Map<String, String> props =
                Day149CloudStream.bindingProperties("orderProcessor", "orders", "order-svc");
        assertTrue(props.containsKey("spring.cloud.function.definition"));
        assertEquals("orderProcessor",
                props.get("spring.cloud.function.definition"));
        assertTrue(props.containsKey(
                "spring.cloud.stream.bindings.orderProcessor-in-0.destination"));
        assertEquals("orders",
                props.get("spring.cloud.stream.bindings.orderProcessor-in-0.destination"));
    }

    @Test
    void testBindingPropertiesContainGroupAndOutput() {
        Map<String, String> props =
                Day149CloudStream.bindingProperties("fn", "input-topic", "my-group");
        assertEquals("my-group",
                props.get("spring.cloud.stream.bindings.fn-in-0.group"));
        assertTrue(props.containsKey(
                "spring.cloud.stream.bindings.fn-out-0.destination"));
    }

    @Test
    void testMigrationSteps() {
        List<Day149CloudStream.MigrationStep> steps =
                Day149CloudStream.migrationFromAnnotationStyle();
        assertEquals(5, steps.size());
        assertEquals(1, steps.get(0).order());
    }
}
