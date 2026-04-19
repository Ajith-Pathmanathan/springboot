package com.techleadguru.phase4.day77;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * DAY 77 — CompletableFuture Error Pipeline
 *
 * COMPLETABLEFUTURE CHAINING OPERATORS:
 *
 *  TRANSFORM:
 *   thenApply(fn)       → sync transform of result (like Stream.map())
 *   thenApplyAsync(fn)  → transform on different thread
 *   thenCompose(fn)     → async flatMap — fn returns CompletableFuture (avoids nested CF<CF<T>>)
 *
 *  COMBINE:
 *   thenCombine(cf, fn) → combine two independent CFs when both complete
 *   allOf(cf1,cf2,...)  → complete when ALL complete (no result — must join each)
 *   anyOf(cf1,cf2,...)  → complete when FIRST one completes (race pattern)
 *
 *  SIDE EFFECT:
 *   thenAccept(consumer) → consume result without returning value
 *   thenRun(runnable)    → run action when done (no access to result)
 *   whenComplete(fn)     → runs on success AND failure (has result + exception)
 *   exceptionally(fn)    → handle error, return fallback value (chain continues)
 *   handle(fn)           → handle BOTH success and failure, transform result
 *
 *  TIMEOUT (Java 9+):
 *   orTimeout(timeout, unit)               → complete exceptionally if too slow
 *   completeOnTimeout(value, timeout, unit) → complete with fallback value if too slow
 *
 * EXCEPTION HANDLING RULES:
 *   1. exceptionally() catches the exception and provides a fallback. Chain continues normally.
 *   2. handle() receives (result, exception) — always runs regardless of success/failure.
 *      Return the transformed value. If you re-throw in handle(), the exception propagates.
 *   3. whenComplete() is for side effects (logging). The exception still propagates after.
 *   4. If an exception is not caught, calling .get() throws ExecutionException wrapping it.
 *
 * COMMON PITFALLS:
 *   - Calling .get() in a thenApply() callback (blocks the ForkJoinPool thread!)
 *   - Not catching exceptions → silent failures in fire-and-forget pipelines
 *   - thenCompose(fn) when fn returns a plain value (use thenApply instead)
 *   - allOf() doesn't have a result — you must .get() each individual CF after
 */
@Slf4j
@Service
public class Day77CompletableFuturePipeline {

    // =========================================================================
    // Order processing pipeline — realistic end-to-end example
    // =========================================================================

    public record OrderRequest(String orderId, String customerId, double amount) {}

    public record ValidatedOrder(String orderId, String customerId, double amount, boolean valid) {}

    public record ChargeResult(String orderId, String chargeId, boolean success) {}

    public record FulfillmentResult(String orderId, String warehouseId, String trackingId) {}

    public record OrderResult(String orderId, String chargeId, String trackingId,
                              String status, String errorMessage) {}

    /**
     * Full pipeline:
     *   validate → charge → fulfill → build result
     *   with error handling at each step and a 5-second overall timeout.
     */
    public CompletableFuture<OrderResult> processOrder(OrderRequest request) {
        return CompletableFuture
                .supplyAsync(() -> validate(request))           // Step 1: validate
                .thenCompose(validated -> chargeCustomer(validated)) // Step 2: thenCompose (returns CF)
                .thenCompose(charge -> fulfillOrder(charge))    // Step 3: fulfill
                .thenApply(fulfillment ->                       // Step 4: build success result
                        new OrderResult(fulfillment.orderId(), null, fulfillment.trackingId(),
                                "COMPLETED", null))
                .exceptionally(ex -> {                          // catch any upstream exception
                    log.error("Order {} failed: {}", request.orderId(), ex.getMessage());
                    return new OrderResult(request.orderId(), null, null,
                            "FAILED", ex.getMessage());
                })
                .whenComplete((result, ex) ->                   // always log, even on failure
                        log.info("Order {} final status: {}", request.orderId(),
                                ex != null ? "EXCEPTION" : result.status()))
                .orTimeout(5, TimeUnit.SECONDS);                // fail fast if too slow
    }

    private ValidatedOrder validate(OrderRequest req) {
        if (req.amount() <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (req.customerId() == null || req.customerId().isBlank())
            throw new IllegalArgumentException("Customer ID required");
        log.info("Order {} validated", req.orderId());
        return new ValidatedOrder(req.orderId(), req.customerId(), req.amount(), true);
    }

    private CompletableFuture<ChargeResult> chargeCustomer(ValidatedOrder order) {
        return CompletableFuture.supplyAsync(() -> {
            if (order.amount() > 10_000) throw new RuntimeException("Charge declined: amount too large");
            log.info("Order {} charged", order.orderId());
            return new ChargeResult(order.orderId(), "CHG-" + System.currentTimeMillis(), true);
        });
    }

    private CompletableFuture<FulfillmentResult> fulfillOrder(ChargeResult charge) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Order {} fulfilled", charge.orderId());
            return new FulfillmentResult(charge.orderId(), "WH-EAST", "TRK-" + charge.orderId());
        });
    }

    // =========================================================================
    // Error handling patterns
    // =========================================================================

    /**
     * exceptionally() — provide fallback value when exception occurs.
     * The RETURNED CompletableFuture resolves normally with the fallback.
     * Chain continues as if no exception happened.
     */
    public CompletableFuture<String> withExceptionallyFallback(boolean shouldFail) {
        return CompletableFuture
                .supplyAsync(() -> {
                    if (shouldFail) throw new RuntimeException("Service unavailable");
                    return "real-data";
                })
                .exceptionally(ex -> {
                    log.warn("Falling back because: {}", ex.getMessage());
                    return "cached-fallback";
                });
    }

    /**
     * handle() — always runs, for both success and failure.
     * Use when you need to transform the result AND handle the error in one place.
     */
    public CompletableFuture<String> withHandle(boolean shouldFail) {
        return CompletableFuture
                .supplyAsync(() -> {
                    if (shouldFail) throw new RuntimeException("handle-error");
                    return "success-value";
                })
                .handle((result, ex) -> {
                    if (ex != null) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        return "HANDLED: " + cause.getMessage();
                    }
                    return "HANDLED: " + result.toUpperCase();
                });
    }

    /**
     * whenComplete() — for side effects, does NOT change the result or catch exceptions.
     * If the stage failed, the exception STILL propagates after whenComplete.
     */
    public CompletableFuture<String> withWhenComplete(boolean shouldFail) {
        return CompletableFuture
                .supplyAsync(() -> {
                    if (shouldFail) throw new RuntimeException("whenComplete-error");
                    return "value";
                })
                .whenComplete((result, ex) -> {
                    if (ex != null) log.error("WhenComplete saw error: {}", ex.getMessage());
                    else log.info("WhenComplete saw result: {}", result);
                    // NOTE: exception still propagates even though we "saw" it here
                });
    }

    // =========================================================================
    // Combining patterns
    // =========================================================================

    /**
     * thenCombine(): wait for TWO independent futures, combine their results.
     * Runs both concurrently; combines when both complete.
     */
    public CompletableFuture<String> combineTwo() {
        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "Hello");
        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> "World");
        return cf1.thenCombine(cf2, (a, b) -> a + " " + b);
    }

    /**
     * allOf(): wait for all futures to complete.
     * NOTE: allOf() itself returns CompletableFuture<Void> — you must get each result separately.
     */
    public CompletableFuture<List<String>> waitForAll(List<String> inputs) {
        var futures = inputs.stream()
                .map(input -> CompletableFuture.supplyAsync(() -> input.toUpperCase()))
                .toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().map(CompletableFuture::join).toList());
    }

    /**
     * anyOf(): complete as soon as the FIRST future completes (race pattern).
     * Use when multiple services can answer the same query — take the fastest.
     */
    public CompletableFuture<String> raceForFirstResult(List<CompletableFuture<String>> candidates) {
        return CompletableFuture.anyOf(candidates.toArray(new CompletableFuture[0]))
                .thenApply(o -> (String) o);
    }

    /**
     * orTimeout() vs completeOnTimeout():
     *   orTimeout()           → throws TimeoutException if too slow
     *   completeOnTimeout(v)  → returns fallback value v if too slow (no exception)
     */
    public CompletableFuture<String> withTimeout(long delayMs, String fallback) {
        return CompletableFuture
                .supplyAsync(() -> {
                    try { Thread.sleep(delayMs); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "slow-result";
                })
                .completeOnTimeout(fallback, 100, TimeUnit.MILLISECONDS);
    }

    // =========================================================================
    // Retry pattern with thenCompose
    // =========================================================================

    /**
     * Retry a CompletableFuture up to maxAttempts times.
     * Uses recursion with thenCompose for clean async retry without blocking.
     */
    public CompletableFuture<String> withRetry(Callable<String> task, int maxAttempts) {
        return attemptWithRetry(task, maxAttempts, 1);
    }

    private CompletableFuture<String> attemptWithRetry(Callable<String> task,
                                                        int maxAttempts, int attempt) {
        return CompletableFuture.supplyAsync(() -> {
            try { return task.call(); }
            catch (Exception e) { throw new CompletionException(e); }
        }).exceptionally(ex -> {
            if (attempt < maxAttempts) {
                log.warn("Attempt {} failed, retrying... ({})", attempt, ex.getMessage());
                // Note: This is a simplified sync retry for clarity.
                // For true async retry, use a ScheduledExecutorService with backoff.
                return attemptWithRetry(task, maxAttempts, attempt + 1).join();
            }
            throw new CompletionException("Max retries exceeded after " + attempt + " attempts", ex);
        });
    }
}
