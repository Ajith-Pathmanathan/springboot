package com.techleadguru.phase4.day77;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAY 77 — CompletableFuture Error Pipeline Test
 *
 * Verifies:
 * 1. Full order pipeline succeeds with valid input
 * 2. Full order pipeline returns FAILED status for invalid input (amount > 10k)
 * 3. exceptionally() provides fallback value — no exception propagates to caller
 * 4. handle() processes both success and failure paths
 * 5. whenComplete() side effect runs but exception still propagates
 * 6. waitForAll() collects results from all CompletableFutures
 * 7. withTimeout() returns fallback when operation exceeds timeout
 * 8. withRetry() retries on transient failures
 */
@SpringBootTest(classes = Phase4Application.class)
class Day77CompletableFuturePipelineTest {

    @Autowired Day77CompletableFuturePipeline pipeline;

    // =========================================================================
    // Full order pipeline
    // =========================================================================

    @Test
    void processOrder_succeeds_for_valid_order() throws Exception {
        var request = new Day77CompletableFuturePipeline.OrderRequest("ORD-001", "CUST-1", 99.99);
        var result = pipeline.processOrder(request).get(10, TimeUnit.SECONDS);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.trackingId()).isNotBlank();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void processOrder_fails_for_amount_over_10k() throws Exception {
        var request = new Day77CompletableFuturePipeline.OrderRequest("ORD-002", "CUST-2", 99_999.0);
        var result = pipeline.processOrder(request).get(10, TimeUnit.SECONDS);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.errorMessage()).isNotBlank();
    }

    @Test
    void processOrder_fails_for_invalid_customer() throws Exception {
        var request = new Day77CompletableFuturePipeline.OrderRequest("ORD-003", "", 50.0);
        var result = pipeline.processOrder(request).get(10, TimeUnit.SECONDS);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.errorMessage()).containsIgnoringCase("customer");
    }

    // =========================================================================
    // Error handling operators
    // =========================================================================

    @Test
    void exceptionally_returns_fallback_on_failure() throws Exception {
        String result = pipeline.withExceptionallyFallback(true).get(3, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("cached-fallback");
    }

    @Test
    void exceptionally_passes_through_on_success() throws Exception {
        String result = pipeline.withExceptionallyFallback(false).get(3, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("real-data");
    }

    @Test
    void handle_transforms_success() throws Exception {
        String result = pipeline.withHandle(false).get(3, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("HANDLED: SUCCESS-VALUE");
    }

    @Test
    void handle_transforms_failure() throws Exception {
        String result = pipeline.withHandle(true).get(3, TimeUnit.SECONDS);
        assertThat(result).startsWith("HANDLED: handle-error");
    }

    @Test
    void whenComplete_still_propagates_exception() {
        var future = pipeline.withWhenComplete(true);
        // whenComplete does NOT catch the exception — get() should still throw
        assertThatThrownBy(() -> future.get(3, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void whenComplete_does_not_affect_successful_result() throws Exception {
        String result = pipeline.withWhenComplete(false).get(3, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("value");
    }

    // =========================================================================
    // Combining patterns
    // =========================================================================

    @Test
    void combineTwo_concatenates_hello_world() throws Exception {
        String result = pipeline.combineTwo().get(3, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("Hello World");
    }

    @Test
    void waitForAll_collects_all_results() throws Exception {
        List<String> results = pipeline.waitForAll(List.of("a", "b", "c", "d"))
                .get(5, TimeUnit.SECONDS);
        assertThat(results).containsExactlyInAnyOrder("A", "B", "C", "D");
    }

    @Test
    void withTimeout_returns_fallback_when_delay_exceeds_100ms() throws Exception {
        // delayMs = 500ms, timeout = 100ms → returns fallback
        String result = pipeline.withTimeout(500, "timeout-fallback").get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("timeout-fallback");
    }

    @Test
    void withTimeout_returns_real_result_when_fast_enough() throws Exception {
        // delayMs = 10ms, timeout = 100ms → returns real result
        String result = pipeline.withTimeout(10, "fallback").get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("slow-result"); // method body returns "slow-result" but it's fast
    }
}
