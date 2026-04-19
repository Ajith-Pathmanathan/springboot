package com.techleadguru.phase3.day48;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 48 — Test: CorrelationIdFilter injects X-Request-ID via MDC.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day48CorrelationIdTest {

    @Autowired
    MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Test 1: Client-provided X-Request-ID is echoed back in response
    // -----------------------------------------------------------------------
    @Test
    void provided_request_id_is_echoed_in_response() throws Exception {
        String myRequestId = "my-corr-id-12345";

        mockMvc.perform(get("/api/day48/request-info")
                        .header("X-Request-ID", myRequestId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", myRequestId))
                .andExpect(jsonPath("$.requestId").value(myRequestId));

        System.out.println("[DAY 48] X-Request-ID echoed back: " + myRequestId);
        System.out.println("[DAY 48] MDC.put('requestId', id) → Log pattern picks it up");
    }

    // -----------------------------------------------------------------------
    // Test 2: Missing X-Request-ID → auto-generated UUID in response
    // -----------------------------------------------------------------------
    @Test
    void missing_request_id_generates_uuid() throws Exception {
        String responseId = mockMvc.perform(get("/api/day48/request-info"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andReturn()
                .getResponse()
                .getHeader("X-Request-ID");

        assertThat(responseId)
                .isNotBlank()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        System.out.println("[DAY 48] Auto-generated X-Request-ID: " + responseId);
    }

    // -----------------------------------------------------------------------
    // Test 3: X-User-Id header is also captured in response body via MDC
    // -----------------------------------------------------------------------
    @Test
    void user_id_header_is_captured_in_mdc() throws Exception {
        mockMvc.perform(get("/api/day48/request-info")
                        .header("X-Request-ID", "test-req-001")
                        .header("X-User-Id", "USR-99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("USR-99"));

        System.out.println("[DAY 48] X-User-Id → MDC.put('userId') → included in log pattern");
    }

    // -----------------------------------------------------------------------
    // Test 4: Each request gets a unique ID (no ID reuse)
    // -----------------------------------------------------------------------
    @Test
    void each_request_gets_unique_correlation_id() throws Exception {
        String id1 = mockMvc.perform(get("/api/day48/request-info"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Request-ID");

        String id2 = mockMvc.perform(get("/api/day48/request-info"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Request-ID");

        assertThat(id1).isNotEqualTo(id2);
        System.out.println("[DAY 48] Two requests → two unique IDs: " + id1 + " | " + id2);
    }

    // -----------------------------------------------------------------------
    // Test 5: Document MDC pattern
    // -----------------------------------------------------------------------
    @Test
    void document_mdc_structured_logging_pattern() {
        System.out.println("[DAY 48] MDC CORRELATION ID PATTERN:");
        System.out.println();
        System.out.println("  1. Client sends: X-Request-ID: 550e-...");
        System.out.println("     OR filter generates: UUID.randomUUID()");
        System.out.println("  2. Filter: MDC.put(\"requestId\", id)");
        System.out.println("  3. Logback pattern: [requestId=%X{requestId}]");
        System.out.println("     2024-01-15 14:23:01 [requestId=550e8400] DEBUG — Order created");
        System.out.println("  4. Filter finally block: MDC.clear()");
        System.out.println("     ← CRITICAL: prevents ThreadPool MDC leaks");
        System.out.println();
        System.out.println("  ASYNC WARNING:");
        System.out.println("    @Async spawns a new thread → MDC is NOT inherited!");
        System.out.println("    Fix: use TaskDecorator to copy MDC to the new thread.");
        System.out.println("    Spring Sleuth / Micrometer Tracing handle this automatically.");
        assertThat(true).isTrue();
    }
}
