package com.techleadguru.phase4.day59;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 59 — @Async + CompletableFuture: Parallel vs Sequential Test
 *
 * Verifies:
 * 1. getParallel() completes in roughly 1/3 the time of getSequential()
 * 2. All three service results (stock, price, rating) are populated
 * 3. REST endpoints aggregate correctly
 */
@SpringBootTest(classes = Phase4Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day59AsyncCompletableFutureTest {

    @Autowired MockMvc mockMvc;
    @Autowired Day59AsyncCompletableFuture.ProductAggregatorService aggregatorService;

    @Test
    void getSequential_returns_all_three_values() throws Exception {
        var result = aggregatorService.getSequential("PROD-1");
        assertThat(result.productId()).isEqualTo("PROD-1");
        assertThat(result.stock()).isNotNull();
        assertThat(result.price()).isNotNull();
        assertThat(result.rating()).isNotNull();
    }

    @Test
    void getParallel_returns_all_three_values() throws Exception {
        var result = aggregatorService.getParallel("PROD-2");
        assertThat(result.productId()).isEqualTo("PROD-2");
        assertThat(result.stock()).isNotNull();
        assertThat(result.price()).isNotNull();
        assertThat(result.rating()).isNotNull();
    }

    @Test
    void parallel_is_faster_than_sequential() throws Exception {
        // Warm up
        aggregatorService.getParallel("PROD-WARM");

        long seqStart = System.currentTimeMillis();
        aggregatorService.getSequential("PROD-SEQ");
        long seqElapsed = System.currentTimeMillis() - seqStart;

        long parStart = System.currentTimeMillis();
        aggregatorService.getParallel("PROD-PAR");
        long parElapsed = System.currentTimeMillis() - parStart;

        // Sequential = 3 × 100ms = ~300ms; Parallel = ~100ms
        assertThat(seqElapsed).as("Sequential should take ≥200ms").isGreaterThanOrEqualTo(200);
        assertThat(parElapsed).as("Parallel should take much less").isLessThan(seqElapsed);
    }

    @Test
    void sequential_endpoint_returns_product_details() throws Exception {
        mockMvc.perform(get("/api/day59/products/PROD-1/sequential"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("PROD-1"))
                .andExpect(jsonPath("$.stock").isNumber())
                .andExpect(jsonPath("$.price").isNumber())
                .andExpect(jsonPath("$.rating").isNumber())
                .andExpect(jsonPath("$.elapsedMs").isNumber());
    }

    @Test
    void parallel_endpoint_returns_product_details() throws Exception {
        mockMvc.perform(get("/api/day59/products/PROD-2/parallel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("PROD-2"))
                .andExpect(jsonPath("$.elapsedMs").isNumber());
    }

    @Test
    void getPriceWithDiscount_chains_thenApply_correctly() throws Exception {
        String result = aggregatorService.getPriceWithDiscount("PROD-3")
                .get(5, TimeUnit.SECONDS);
        // Returns "Discounted: $XX.XX"
        assertThat(result).startsWith("Discounted: $");
    }
}
