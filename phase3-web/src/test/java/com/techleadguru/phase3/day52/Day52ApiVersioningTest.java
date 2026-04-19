package com.techleadguru.phase3.day52;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 52 — Test: URI versioning and header versioning return different response shapes.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day52ApiVersioningTest {

    @Autowired
    MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Test 1: URI V1 returns only basic fields
    // -----------------------------------------------------------------------
    @Test
    void uri_v1_returns_basic_fields_only() throws Exception {
        mockMvc.perform(get("/api/day52/v1/orders/ORD-A")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ORD-A"))
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.total").value(149.99))
                // V1 does NOT include enriched fields
                .andExpect(jsonPath("$.estimatedDelivery").doesNotExist())
                .andExpect(jsonPath("$.loyaltyPointsEarned").doesNotExist());

        System.out.println("[DAY 52] URI V1: /v1/orders/ORD-A → basic fields only");
    }

    // -----------------------------------------------------------------------
    // Test 2: URI V2 returns enriched fields
    // -----------------------------------------------------------------------
    @Test
    void uri_v2_returns_enriched_fields() throws Exception {
        mockMvc.perform(get("/api/day52/v2/orders/ORD-A")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ORD-A"))
                .andExpect(jsonPath("$.estimatedDelivery").exists())
                .andExpect(jsonPath("$.loyaltyPointsEarned").value(150))
                .andExpect(jsonPath("$.regionCode").value("US-WEST"));

        System.out.println("[DAY 52] URI V2: /v2/orders/ORD-A → enriched with loyalty + delivery");
    }

    // -----------------------------------------------------------------------
    // Test 3: Header V1 (default) returns basic fields
    // -----------------------------------------------------------------------
    @Test
    void header_v1_returns_basic_fields() throws Exception {
        mockMvc.perform(get("/api/day52/orders/ORD-A")
                        .header("X-API-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ORD-A"))
                .andExpect(jsonPath("$.estimatedDelivery").doesNotExist());

        System.out.println("[DAY 52] X-API-Version: 1 → basic response shape");
    }

    // -----------------------------------------------------------------------
    // Test 4: Header V2 returns enriched fields
    // -----------------------------------------------------------------------
    @Test
    void header_v2_returns_enriched_fields() throws Exception {
        mockMvc.perform(get("/api/day52/orders/ORD-A")
                        .header("X-API-Version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyaltyPointsEarned").value(150))
                .andExpect(jsonPath("$.regionCode").value("US-WEST"));

        System.out.println("[DAY 52] X-API-Version: 2 → enriched response shape");
    }

    // -----------------------------------------------------------------------
    // Test 5: Header default (no version header) defaults to V1
    // -----------------------------------------------------------------------
    @Test
    void missing_version_header_defaults_to_v1() throws Exception {
        mockMvc.perform(get("/api/day52/orders/ORD-B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ORD-B"))
                .andExpect(jsonPath("$.estimatedDelivery").doesNotExist());

        System.out.println("[DAY 52] No X-API-Version → defaultValue='1' → V1 response");
    }

    // -----------------------------------------------------------------------
    // Test 6: Unknown version returns 400
    // -----------------------------------------------------------------------
    @Test
    void unsupported_version_returns_400() throws Exception {
        mockMvc.perform(get("/api/day52/orders/ORD-A")
                        .header("X-API-Version", "99"))
                .andExpect(status().isBadRequest());

        System.out.println("[DAY 52] X-API-Version: 99 → 400 Bad Request (unsupported version)");
    }

    // -----------------------------------------------------------------------
    // Test 7: Document versioning strategies
    // -----------------------------------------------------------------------
    @Test
    void document_versioning_strategies() {
        System.out.println("[DAY 52] API VERSIONING STRATEGIES:");
        System.out.println();
        System.out.println("  URI:    GET /api/v1/orders  vs  /api/v2/orders");
        System.out.println("    ✓ Simple, visible, easy to test in browser");
        System.out.println("    ✓ Used by Stripe, Twitter, Twilio");
        System.out.println("    ✗ 'Impure' REST — URI should identify resource, not version");
        System.out.println();
        System.out.println("  Header: GET /api/orders  +  X-API-Version: 2");
        System.out.println("    ✓ Clean URL, single canonical resource URL");
        System.out.println("    ✓ Used by GitHub API");
        System.out.println("    ✗ Invisible in browser, harder to cache");
        System.out.println();
        System.out.println("  BEST PRACTICE:");
        System.out.println("    - Support N and N-1 versions simultaneously");
        System.out.println("    - Deprecation header: Deprecation: version='v1', sunset='2025-06-01'");
        System.out.println("    - Only version on BREAKING changes (removing/renaming fields)");
        System.out.println("    - Adding new optional fields is backward-compatible (no new version)");
        assertThat(true).isTrue();
    }
}
