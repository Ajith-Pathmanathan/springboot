package com.techleadguru.phase4.day60;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DAY 60 — @Async Exception Black Hole ⚠️
 *
 * THE PROBLEM:
 *   @Async void methods run on a different thread.
 *   If they throw an exception, NOBODY is listening on that thread.
 *   The exception is silently DROPPED — no log entry, no crash, no indication anything failed.
 *
 *   Example failure scenario:
 *     orderService.processOrder(order);  // @Async void
 *     // order processing threw NullPointerException
 *     // caller sees nothing. Order is lost. Customer never notified.
 *
 * DETECTION: Implement AsyncUncaughtExceptionHandler via AsyncConfigurer
 *
 * THREE FAILURE MODES:
 *
 *   1. void @Async — exception silently vanishes (without handler):
 *      @Async
 *      public void doWork() { throw new RuntimeException("lost!"); }
 *
 *   2. CompletableFuture @Async — exception is captured in the future:
 *      @Async
 *      public CompletableFuture<String> doWork() { throw new RuntimeException(); }
 *      future.get() → throws ExecutionException (YOU must call .get()!)
 *
 *   3. CompletableFuture @Async — ALSO lost if nobody calls .get():
 *      CompletableFuture<String> f = service.doWork(); // fire and forget
 *      // exception captured in f, but f is never inspected → still lost
 *
 * SOLUTIONS:
 *   a) AsyncUncaughtExceptionHandler → for void @Async (metrics, alert, dead-letter)
 *   b) Return CompletableFuture<T> and always chain .exceptionally() or .handle()
 *   c) Wrap body in try/catch + log/publish to error channel
 *   d) Use @Async with @Retryable for transient failures
 */
@Slf4j
public class Day60AsyncExceptionBlackHole {

    // =========================================================================
    // Configurer: installs the uncaught exception handler
    // =========================================================================

    @Configuration
    public static class AsyncExceptionConfig implements AsyncConfigurer {

        private final TrackingUncaughtExceptionHandler handler = new TrackingUncaughtExceptionHandler();

        @Override
        public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return handler;
        }

        // expose handler as a bean so tests can inspect it
        @org.springframework.context.annotation.Bean("asyncExceptionHandler")
        public TrackingUncaughtExceptionHandler asyncExceptionHandler() {
            return handler;
        }
    }

    // =========================================================================
    // Custom exception handler — counts + captures async exceptions
    // =========================================================================

    public static class TrackingUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

        private final AtomicInteger count = new AtomicInteger();
        private final AtomicReference<Throwable> lastException = new AtomicReference<>();
        private volatile CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void handleUncaughtException(@NonNull Throwable ex, @NonNull Method method, @NonNull Object... params) {
            count.incrementAndGet();
            lastException.set(ex);
            log.error("[Day60] AsyncUncaughtExceptionHandler caught: {} in {}.{}",
                    ex.getMessage(), method.getDeclaringClass().getSimpleName(), method.getName());
            // In production: publish to error queue, trigger alert, increment metric
            latch.countDown();
        }

        public int getExceptionCount() { return count.get(); }
        public Throwable getLastException() { return lastException.get(); }
        public void resetLatch(int count) { this.latch = new CountDownLatch(count); }
        public CountDownLatch getLatch() { return latch; }
    }

    // =========================================================================
    // Services demonstrating the three failure modes
    // =========================================================================

    @Service
    @Slf4j
    public static class PaymentProcessorService {

        /**
         * MODE 1: void @Async — exception silently vanishes WITHOUT handler.
         * WITH AsyncUncaughtExceptionHandler, it is caught and logged.
         */
        @Async
        public void processPaymentAsync(String orderId, boolean shouldFail) {
            log.info("[Day60] Processing payment for {} on thread {}", orderId, Thread.currentThread().getName());
            if (shouldFail) {
                throw new IllegalStateException("Payment gateway timeout for order " + orderId);
            }
            log.info("[Day60] Payment processed for {}", orderId);
        }

        /**
         * MODE 2: CompletableFuture @Async — exception is captured in future.
         * Caller MUST call .get() or .exceptionally() to see it.
         */
        @Async
        public CompletableFuture<String> chargeCardAsync(String cardNumber, boolean shouldFail) {
            log.info("[Day60] Charging card {} on thread {}", cardNumber, Thread.currentThread().getName());
            if (shouldFail) {
                // Exception wraps in CompletableFuture automatically
                throw new RuntimeException("Card declined: insufficient funds");
            }
            return CompletableFuture.completedFuture("CHARGE-OK-" + cardNumber);
        }

        /**
         * MODE 3: Properly handled future with .exceptionally() fallback.
         */
        @Async
        public CompletableFuture<String> chargeWithFallbackAsync(String cardNumber) {
            // NOTE: Cannot call chargeCardAsync(this) here — self-invocation bypasses @Async proxy.
            // Instead, apply .exceptionally() inline within this @Async method.
            return CompletableFuture.<String>supplyAsync(() -> {
                        log.info("[Day60] chargeWithFallback: charging {} on thread {}", cardNumber, Thread.currentThread().getName());
                        throw new RuntimeException("Card declined: insufficient funds");
                    })
                    .exceptionally(ex -> {
                        log.warn("[Day60] Charge failed, using fallback: {}", ex.getMessage());
                        return "FALLBACK-PAYMENT-QUEUED";
                    });
        }
    }
}
