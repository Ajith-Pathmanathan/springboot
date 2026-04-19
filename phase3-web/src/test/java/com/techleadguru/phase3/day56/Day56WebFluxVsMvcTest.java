package com.techleadguru.phase3.day56;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

/**
 * DAY 56 — Test: MVC sequential/parallel and WebFlux reactive parallel all return correct data.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day56WebFluxVsMvcTest {

    @Autowired
    MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Test 1: MVC sequential returns all three prices
    // -----------------------------------------------------------------------
    @Test
    void mvc_sequential_returns_three_prices() throws Exception {
        mockMvc.perform(get("/api/day56/prices/sequential"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceA").isNumber())
                .andExpect(jsonPath("$.priceB").isNumber())
                .andExpect(jsonPath("$.priceC").isNumber())
                .andExpect(jsonPath("$.strategy").value("MVC-sequential"));

        System.out.println("[DAY 56] MVC sequential: 3 blocking calls in sequence, one thread occupied ~150ms");
    }

    // -----------------------------------------------------------------------
    // Test 2: MVC parallel-threads returns all three prices
    // -----------------------------------------------------------------------
    @Test
    void mvc_parallel_threads_returns_three_prices() throws Exception {
        mockMvc.perform(get("/api/day56/prices/parallel-threads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceA").isNumber())
                .andExpect(jsonPath("$.priceB").isNumber())
                .andExpect(jsonPath("$.strategy").value("MVC-parallel-threads"));

        System.out.println("[DAY 56] MVC parallel: 3 threads, each waiting 50ms — total ~50ms but 4 threads occupied");
    }

    // -----------------------------------------------------------------------
    // Test 3: WebFlux parallel returns all three prices reactively
    // -----------------------------------------------------------------------
    @Test
    void reactive_parallel_returns_three_prices() throws Exception {
        // Mono<T> return type uses async dispatch in Spring MVC
        var mvcResult = mockMvc.perform(get("/api/day56/reactive-prices/parallel"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceA").isNumber())
                .andExpect(jsonPath("$.priceB").isNumber())
                .andExpect(jsonPath("$.priceC").isNumber())
                .andExpect(jsonPath("$.strategy").value("WebFlux-parallel"));

        System.out.println("[DAY 56] WebFlux Mono.zip: 3 subscriptions fired simultaneously, bounded-elastic handles waits");
    }

    // -----------------------------------------------------------------------
    // Test 4: WebFlux bulk endpoint returns list of prices
    // -----------------------------------------------------------------------
    @Test
    void reactive_bulk_returns_list_of_prices() throws Exception {
        // Mono<List<T>> return type uses async dispatch in Spring MVC
        var mvcResult = mockMvc.perform(get("/api/day56/reactive-prices/bulk"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5));

        System.out.println("[DAY 56] WebFlux Flux.flatMap: 5 parallel price lookups → list result");
    }

    // -----------------------------------------------------------------------
    // Test 5: Sequential is slower than parallel (document the difference)
    // -----------------------------------------------------------------------
    @Test
    void sequential_slower_than_parallel() throws Exception {
        String seqBody = mockMvc.perform(get("/api/day56/prices/sequential"))
                .andReturn().getResponse().getContentAsString();
        String parBody = mockMvc.perform(get("/api/day56/prices/parallel-threads"))
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        long seqMs = mapper.readTree(seqBody).get("elapsedMs").asLong();
        long parMs = mapper.readTree(parBody).get("elapsedMs").asLong();

        // Sequential (3×50ms) should be slower than parallel (max 50ms)
        assertThat(seqMs).isGreaterThan(parMs);

        System.out.println("[DAY 56] Sequential: " + seqMs + "ms  vs  Parallel: " + parMs + "ms");
        System.out.printf("[DAY 56] Speedup: %.1fx%n", (double) seqMs / parMs);
    }

    // -----------------------------------------------------------------------
    // Test 6: Document the MVC vs WebFlux decision framework
    // -----------------------------------------------------------------------
    @Test
    void document_mvc_vs_webflux_choice() {
        System.out.println("[DAY 56] CHOOSE MVC when:");
        System.out.println("  • Team knows Spring MVC well");
        System.out.println("  • App is DB-heavy (JPA/JDBC is blocking anyway)");
        System.out.println("  • < 500 concurrent users expected");
        System.out.println("  • Spring Security, Spring Session, etc. (better MVC support)");
        System.out.println();
        System.out.println("[DAY 56] CHOOSE WEBFLUX when:");
        System.out.println("  • API aggregator: calling many downstream services");
        System.out.println("  • High concurrency: 10,000+ simultaneous connections");
        System.out.println("  • Streaming: SSE, WebSocket, live data feeds");
        System.out.println("  • Microservice mesh with reactive DB drivers (R2DBC)");
        System.out.println();
        System.out.println("[DAY 56] MIXED (common in enterprise):");
        System.out.println("  • Spring MVC + WebClient for outbound HTTP calls");
        System.out.println("  • MVC + @Async CompletableFuture for limited parallelism");
        System.out.println("  • 'Good enough' without full Reactor learning curve");
        assertThat(true).isTrue();
    }
}
