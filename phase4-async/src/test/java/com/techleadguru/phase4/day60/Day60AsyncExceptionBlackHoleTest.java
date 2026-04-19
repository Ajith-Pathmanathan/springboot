package com.techleadguru.phase4.day60;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAY 60 — @Async Exception Black Hole Test
 *
 * Verifies:
 * 1. void @Async exception IS captured by AsyncUncaughtExceptionHandler (not silently dropped)
 * 2. CompletableFuture @Async exception surfaces via get() → ExecutionException
 * 3. .exceptionally() provides a safe fallback — no exception propagates
 */
@SpringBootTest(classes = Phase4Application.class)
class Day60AsyncExceptionBlackHoleTest {

    @Autowired Day60AsyncExceptionBlackHole.PaymentProcessorService paymentService;

    /** Handler is a @Bean exposed by AsyncExceptionConfig — inject it directly */
    @Autowired Day60AsyncExceptionBlackHole.TrackingUncaughtExceptionHandler exceptionHandler;

    @BeforeEach
    void resetHandler() {
        exceptionHandler.resetLatch(1);
    }

    @Test
    void void_async_exception_is_caught_by_uncaught_handler() throws InterruptedException {
        // Record baseline before calling
        int baseline = exceptionHandler.getExceptionCount();

        // When: call void @Async method that throws
        paymentService.processPaymentAsync("ORD-001", true);

        // Then: handler is called within 3 seconds
        boolean handlerCalled = exceptionHandler.getLatch().await(3, TimeUnit.SECONDS);
        assertThat(handlerCalled).as("AsyncUncaughtExceptionHandler should be called").isTrue();
        assertThat(exceptionHandler.getExceptionCount()).isGreaterThan(baseline);
        assertThat(exceptionHandler.getLastException()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void void_async_success_does_not_trigger_handler() throws Exception {
        int baseline = exceptionHandler.getExceptionCount();

        // When: call void @Async method that succeeds
        paymentService.processPaymentAsync("ORD-SUCCESS", false);

        // Wait briefly — handler should NOT be called
        Thread.sleep(300);
        assertThat(exceptionHandler.getExceptionCount()).isEqualTo(baseline);
    }

    @Test
    void completable_future_async_exception_surfaces_via_get() throws InterruptedException {
        // When: call CompletableFuture @Async method that throws
        var future = paymentService.chargeCardAsync("4111-1111-1111-1111", true);

        // Then: future.get() throws ExecutionException wrapping the original exception
        assertThatThrownBy(() -> future.get(3, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("declined");
    }

    @Test
    void exceptionally_provides_fallback_when_charge_fails() throws Exception {
        // When: calling method with built-in .exceptionally() fallback
        var future = paymentService.chargeWithFallbackAsync("4111-2222-3333-4444");
        String result = future.get(3, TimeUnit.SECONDS);

        // Then: no exception — fallback value returned instead
        assertThat(result).isEqualTo("FALLBACK-PAYMENT-QUEUED");
    }

    @Test
    void completable_future_success_returns_charge_id() throws Exception {
        var future = paymentService.chargeCardAsync("4111-5555-6666-7777", false);
        String result = future.get(3, TimeUnit.SECONDS);
        assertThat(result).startsWith("CHARGE-OK-");
    }
}
