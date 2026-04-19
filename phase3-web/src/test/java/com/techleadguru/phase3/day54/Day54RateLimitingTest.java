package com.techleadguru.phase3.day54;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 54 — Test: Token-bucket rate limiting blocks excess requests.
 *
 * capacity=3 means only 3 tokens. The 4th request (from the same client IP)
 * must be rejected with 429 Too Many Requests.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "phase3.day54.rate-limit.enabled=true",
                "phase3.day54.rate-limit.capacity=3",
                "phase3.day54.rate-limit.refill-per-minute=100"
        })
@AutoConfigureMockMvc
class Day54RateLimitingTest {

    @Autowired
    MockMvc mockMvc;

    // Unique client key per test class to avoid cross-test bucket sharing
    private static final String CLIENT_IP = "10.0.0.99";

    // -----------------------------------------------------------------------
    // Test 1: First N requests (within limit) succeed with 200
    // -----------------------------------------------------------------------
    @Test
    void requests_within_limit_return_200() throws Exception {
        String clientId = "client-within-" + System.nanoTime();

        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(get("/api/day54/data")
                            .header("X-Forwarded-For", clientId))
                    .andExpect(status().isOk());
        }
        System.out.println("[DAY 54] 3 requests within capacity=3 limit → all 200 OK");
    }

    // -----------------------------------------------------------------------
    // Test 2: Request exceeding limit returns 429
    // -----------------------------------------------------------------------
    @Test
    void request_exceeding_limit_returns_429() throws Exception {
        String clientId = "client-exceed-" + System.nanoTime();

        // Exhaust the 3-token bucket
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/day54/data")
                            .header("X-Forwarded-For", clientId))
                    .andExpect(status().isOk());
        }

        // 4th request → bucket empty → 429
        mockMvc.perform(get("/api/day54/data")
                        .header("X-Forwarded-For", clientId))
                .andExpect(status().isTooManyRequests());

        System.out.println("[DAY 54] 4th request after exhausting capacity=3 → 429 Too Many Requests");
    }

    // -----------------------------------------------------------------------
    // Test 3: 429 response includes Retry-After header
    // -----------------------------------------------------------------------
    @Test
    void rate_limited_response_has_retry_after_header() throws Exception {
        String clientId = "client-retry-" + System.nanoTime();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/day54/data").header("X-Forwarded-For", clientId))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/day54/data").header("X-Forwarded-For", clientId))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        System.out.println("[DAY 54] 429 response has Retry-After header (client knows when to retry)");
    }

    // -----------------------------------------------------------------------
    // Test 4: Rate limit headers present on allowed requests
    // -----------------------------------------------------------------------
    @Test
    void allowed_requests_have_rate_limit_headers() throws Exception {
        String clientId = "client-headers-" + System.nanoTime();

        mockMvc.perform(get("/api/day54/data").header("X-Forwarded-For", clientId))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Rate-Limit-Limit"))
                .andExpect(header().exists("X-Rate-Limit-Remaining"));

        System.out.println("[DAY 54] X-Rate-Limit-Limit and X-Rate-Limit-Remaining headers present");
    }

    // -----------------------------------------------------------------------
    // Test 5: Each unique client gets its own independent bucket
    // -----------------------------------------------------------------------
    @Test
    void each_client_has_independent_rate_limit_bucket() throws Exception {
        String clientA = "client-A-" + System.nanoTime();
        String clientB = "client-B-" + System.nanoTime();

        // Exhaust client A's bucket
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/day54/data").header("X-Forwarded-For", clientA))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/day54/data").header("X-Forwarded-For", clientA))
                .andExpect(status().isTooManyRequests());

        // Client B has its own fresh bucket → still allowed
        mockMvc.perform(get("/api/day54/data").header("X-Forwarded-For", clientB))
                .andExpect(status().isOk());

        System.out.println("[DAY 54] Client B unaffected by Client A exhausting its bucket");
    }

    // -----------------------------------------------------------------------
    // Test 6: Document token bucket algorithm
    // -----------------------------------------------------------------------
    @Test
    void document_token_bucket_algorithm() {
        System.out.println("[DAY 54] TOKEN BUCKET RATE LIMITING:");
        System.out.println();
        System.out.println("  Bucket4j Greedy Refill:");
        System.out.println("    capacity = 3 tokens");
        System.out.println("    refill   = 100 tokens/minute = ~1.67 tokens/second");
        System.out.println();
        System.out.println("  Each request consumes 1 token.");
        System.out.println("  Tokens refill continuously (greedy) up to capacity.");
        System.out.println();
        System.out.println("  Response headers:");
        System.out.println("    X-Rate-Limit-Limit:     max capacity");
        System.out.println("    X-Rate-Limit-Remaining: tokens left after this request");
        System.out.println("    Retry-After: 60          (seconds until bucket refills)");
        System.out.println();
        System.out.println("  Client key = X-Forwarded-For header (or remote IP)");
        System.out.println("  → Per-client isolation via ConcurrentHashMap<clientKey, Bucket>");
        assertThat(true).isTrue();
    }
}
