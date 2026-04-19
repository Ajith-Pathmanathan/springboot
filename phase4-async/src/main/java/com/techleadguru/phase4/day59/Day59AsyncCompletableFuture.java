package com.techleadguru.phase4.day59;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * DAY 59 — @Async + CompletableFuture: Parallel Calls
 *
 * PROBLEM: Fetching data from 3 microservices sequentially:
 *   inventory service: 100ms
 *   pricing service:   100ms
 *   rating service:    100ms
 *   Total:             300ms  ← unacceptable for a product page
 *
 * SOLUTION: Run all 3 in parallel with @Async + CompletableFuture.allOf()
 *   Total: max(100ms, 100ms, 100ms) = 100ms  ← 3× faster
 *
 * CompletableFuture COMPOSITION OPERATORS:
 *   thenApply(fn)           → transform result synchronously on the same thread
 *   thenApplyAsync(fn)      → transform on a new thread
 *   thenCompose(fn)         → flat-map: fn returns another CompletableFuture
 *   thenCombine(other, fn)  → merge two futures' results
 *   allOf(f1, f2, f3)       → waits for ALL to complete; returns CompletableFuture<Void>
 *   anyOf(f1, f2, f3)       → first to complete wins
 *   exceptionally(fn)       → handle failure with fallback
 *   handle(fn)              → handles both success and failure (BiFunction<T, Throwable, R>)
 *   whenComplete(action)    → side-effect on completion (doesn't transform result)
 *
 * GOTCHA: CompletableFuture.allOf() returns Void.
 *   You must still call each future.join() to get individual results after allOf.
 *   Pattern: CompletableFuture.allOf(f1, f2, f3).thenApply(v -> List.of(f1.join(), f2.join(), f3.join()))
 *
 * TIMEOUT: Use .orTimeout(2, SECONDS) on Java 9+
 *   If service hangs, this prevents indefinite blocking.
 */
@Slf4j
public class Day59AsyncCompletableFuture {

    // =========================================================================
    // Downstream services — each takes 100ms (I/O simulation)
    // =========================================================================

    @Service
    @Slf4j
    public static class InventoryService {
        @Async
        public CompletableFuture<Integer> getStockAsync(String productId) {
            log.debug("[Day59] Fetching inventory for {} on {}", productId, Thread.currentThread().getName());
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return CompletableFuture.completedFuture(42); // always 42 in stock
        }
    }

    @Service
    @Slf4j
    public static class PricingService {
        @Async
        public CompletableFuture<BigDecimal> getPriceAsync(String productId) {
            log.debug("[Day59] Fetching price for {} on {}", productId, Thread.currentThread().getName());
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return CompletableFuture.completedFuture(BigDecimal.valueOf(29.99));
        }
    }

    @Service
    @Slf4j
    public static class RatingService {
        @Async
        public CompletableFuture<Double> getRatingAsync(String productId) {
            log.debug("[Day59] Fetching rating for {} on {}", productId, Thread.currentThread().getName());
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return CompletableFuture.completedFuture(4.7);
        }
    }

    // =========================================================================
    // Aggregation service — fetches all three in parallel
    // =========================================================================

    @Service
    @Slf4j
    public static class ProductAggregatorService {

        private final InventoryService inventory;
        private final PricingService pricing;
        private final RatingService rating;

        public ProductAggregatorService(InventoryService inventory,
                                        PricingService pricing,
                                        RatingService rating) {
            this.inventory = inventory;
            this.pricing = pricing;
            this.rating = rating;
        }

        /**
         * SEQUENTIAL — total latency: 100 + 100 + 100 = 300ms
         */
        public ProductDetails getSequential(String productId) throws Exception {
            long start = System.currentTimeMillis();
            Integer stock = inventory.getStockAsync(productId).get();
            BigDecimal price = pricing.getPriceAsync(productId).get();
            Double ratingVal = rating.getRatingAsync(productId).get();
            log.info("[Day59] Sequential completed in {}ms", System.currentTimeMillis() - start);
            return new ProductDetails(productId, stock, price, ratingVal, System.currentTimeMillis() - start);
        }

        /**
         * PARALLEL — total latency: max(100, 100, 100) = ~100ms
         * ALL 3 @Async calls are submitted to the thread pool before any .get() is called.
         */
        public ProductDetails getParallel(String productId) throws Exception {
            long start = System.currentTimeMillis();
            // Submit all 3 to thread pool FIRST (don't call .get() yet)
            CompletableFuture<Integer> stockFuture   = inventory.getStockAsync(productId);
            CompletableFuture<BigDecimal> priceFuture = pricing.getPriceAsync(productId);
            CompletableFuture<Double> ratingFuture    = rating.getRatingAsync(productId);

            // Wait for all 3 to complete
            CompletableFuture.allOf(stockFuture, priceFuture, ratingFuture).join();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[Day59] Parallel completed in {}ms", elapsed);
            return new ProductDetails(productId, stockFuture.join(), priceFuture.join(), ratingFuture.join(), elapsed);
        }

        /**
         * Demonstrates pipeline: fetch price → apply discount → format output.
         * thenApply transforms on the async thread.
         */
        public CompletableFuture<String> getPriceWithDiscount(String productId) {
            return pricing.getPriceAsync(productId)
                    .thenApply(price -> price.multiply(BigDecimal.valueOf(0.9))) // 10% off
                    .thenApply(discounted -> "Discounted: $" + discounted.setScale(2, java.math.RoundingMode.HALF_UP));
        }
    }

    // =========================================================================
    // Controller
    // =========================================================================

    @RestController
    @RequestMapping("/api/day59/products")
    @Slf4j
    public static class ProductController {

        private final ProductAggregatorService aggregator;

        public ProductController(ProductAggregatorService aggregator) {
            this.aggregator = aggregator;
        }

        @GetMapping("/{id}/sequential")
        public ProductDetails sequential(@PathVariable String id) throws Exception {
            return aggregator.getSequential(id);
        }

        @GetMapping("/{id}/parallel")
        public ProductDetails parallel(@PathVariable String id) throws Exception {
            return aggregator.getParallel(id);
        }
    }

    public record ProductDetails(String productId, Integer stock,
                                  BigDecimal price, Double rating, long elapsedMs) {}
}
