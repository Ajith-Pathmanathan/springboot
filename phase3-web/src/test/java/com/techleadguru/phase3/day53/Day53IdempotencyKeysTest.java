package com.techleadguru.phase3.day53;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 53 — Test: Idempotency filter returns cached response for duplicate key.
 * Uses phase3.day53.idempotency.enabled=true to activate the filter.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "phase3.day53.idempotency.enabled=true")
@AutoConfigureMockMvc
class Day53IdempotencyKeysTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    Day53IdempotencyKeys.IdempotencyStore idempotencyStore;

    private static final String PAYMENT_REQUEST =
            "{\"customerId\":\"CUST-1\",\"amount\":99.99}";

    // -----------------------------------------------------------------------
    // Test 1: POST without idempotency key → 400
    // -----------------------------------------------------------------------
    @Test
    void post_without_idempotency_key_returns_400() throws Exception {
        mockMvc.perform(post("/api/day53/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYMENT_REQUEST))
                .andExpect(status().isBadRequest());

        System.out.println("[DAY 53] Missing X-Idempotency-Key → 400 Bad Request");
    }

    // -----------------------------------------------------------------------
    // Test 2: POST with new key → 201 Created
    // -----------------------------------------------------------------------
    @Test
    void post_with_new_key_creates_payment() throws Exception {
        String key = "test-key-" + System.nanoTime();

        String body = mockMvc.perform(post("/api/day53/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", key)
                        .content(PAYMENT_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();

        System.out.println("[DAY 53] First request with key=" + key + " → 201 Created");
        System.out.println("[DAY 53] Response: " + body);
    }

    // -----------------------------------------------------------------------
    // Test 3: Second request with same key returns SAME response (cached)
    // -----------------------------------------------------------------------
    @Test
    void second_request_with_same_key_returns_identical_response() throws Exception {
        String key = "idempotent-key-" + System.nanoTime();

        // First request
        String firstBody = mockMvc.perform(post("/api/day53/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", key)
                        .content(PAYMENT_REQUEST))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Second request with SAME key
        String secondBody = mockMvc.perform(post("/api/day53/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", key)
                        .content(PAYMENT_REQUEST))
                .andExpect(header().string("X-Idempotent-Replayed", "true"))
                .andReturn().getResponse().getContentAsString();

        // The paymentId must be IDENTICAL — no new payment created
        assertThat(firstBody).isEqualTo(secondBody);

        System.out.println("[DAY 53] Same key → same response. Payment NOT charged twice!");
        System.out.println("[DAY 53] X-Idempotent-Replayed: true header signals replay");
    }

    // -----------------------------------------------------------------------
    // Test 4: Different keys → different paymentIds (no caching)
    // -----------------------------------------------------------------------
    @Test
    void different_keys_create_independent_payments() throws Exception {
        String key1 = "key-a-" + System.nanoTime();
        String key2 = "key-b-" + System.nanoTime();

        String body1 = mockMvc.perform(post("/api/day53/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", key1)
                        .content(PAYMENT_REQUEST))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String body2 = mockMvc.perform(post("/api/day53/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", key2)
                        .content(PAYMENT_REQUEST))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        String id1 = mapper.readTree(body1).get("paymentId").asText();
        String id2 = mapper.readTree(body2).get("paymentId").asText();

        assertThat(id1).isNotEqualTo(id2);
        System.out.println("[DAY 53] Different keys → different paymentIds: " + id1 + " vs " + id2);
    }

    // -----------------------------------------------------------------------
    // Test 5: Document idempotency pattern
    // -----------------------------------------------------------------------
    @Test
    void document_idempotency_pattern() {
        System.out.println("[DAY 53] IDEMPOTENCY KEY PATTERN:");
        System.out.println();
        System.out.println("  PROBLEM: Network retry causes duplicate charge");
        System.out.println("    POST /payments (timeout) → retry → charged TWICE");
        System.out.println();
        System.out.println("  SOLUTION:");
        System.out.println("    1. Client generates UUID: X-Idempotency-Key: 550e-...");
        System.out.println("    2. Server checks key in Redis: seen before?");
        System.out.println("       → YES: return cached response (no DB write)");
        System.out.println("       → NO: process, store result under key (TTL=24h)");
        System.out.println("    3. Client retries with same key → gets same paymentId");
        System.out.println();
        System.out.println("  PRODUCTION: Use Redis with SETNX for atomic check-and-store");
        System.out.println("    → Prevents race condition on concurrent identical requests");
        assertThat(true).isTrue();
    }
}
