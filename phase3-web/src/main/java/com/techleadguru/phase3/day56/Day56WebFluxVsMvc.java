package com.techleadguru.phase3.day56;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * DAY 56 — Spring MVC vs Spring WebFlux: Threading Model Comparison
 *
 * SPRING MVC (Thread-per-request / Blocking):
 *   - Tomcat allocates 1 thread per HTTP request (default pool: 200 threads)
 *   - While waiting for I/O (DB, HTTP call, sleep), the thread sits idle — blocked
 *   - Under load: ~200 concurrent requests max before threads run out
 *   - Simple, familiar programming model (imperative code)
 *   - RIGHT FOR: most CRUD/enterprise apps, simple APIs, team unfamiliar with reactive
 *
 * SPRING WEBFLUX (Event loop / Non-blocking):
 *   - Netty event loop: ~(CPU cores × 2) threads handle ALL requests
 *   - No thread blocks: I/O is handled via callbacks/continuations
 *   - 10,000+ concurrent requests handled with 8 threads
 *   - Programming model: Mono<T> (0-1 items) / Flux<T> (0-N items)
 *   - RIGHT FOR: high-concurrency APIs, streaming, websockets, event-driven systems
 *
 * THE GOLDEN RULE — Don't block in a WebFlux pipeline!
 *   WRONG: Mono.just(jdbcTemplate.query("SELECT..."))
 *          → blocks the event loop thread → kills throughput
 *   RIGHT: Mono.fromCallable(() -> jdbcTemplate.query("SELECT..."))
 *               .subscribeOn(Schedulers.boundedElastic())
 *          → offloads blocking call to an elastic thread pool
 *
 * MONO.ZIP — parallel composition:
 *   Mono.zip(callA(), callB(), callC())   ← all 3 subscriptions fired simultaneously
 *   Total latency = max(A, B, C)          ← NOT A + B + C
 *
 * WHEN TO USE WHICH:
 *   MVC: REST CRUD, database-heavy apps, team knows Java/Spring well
 *   WebFlux: payment gateways, API aggregators, chat/streaming, IoT pipelines
 *   MIXED: MVC + @Async CompletableFuture for "good enough" parallelism without full Reactor
 */
@Slf4j
public class Day56WebFluxVsMvc {

    // =========================================================================
    // Simulated external price service (adds 50ms latency per call)
    // =========================================================================

    @Service
    public static class PriceService {

        /**
         * BLOCKING — ties up a thread for 50ms.
         * In MVC under load: thread pool exhausted → 503 Service Unavailable.
         */
        public BigDecimal getPrice(String productId) {
            try {
                Thread.sleep(50); // simulates DB or external HTTP call latency
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Deterministic price based on productId for testability
            return BigDecimal.valueOf(productId.hashCode() & 0xFFFF, 2)
                    .abs().setScale(2, RoundingMode.HALF_UP);
        }

        /**
         * NON-BLOCKING — returns immediately; Reactor completes it later.
         * Event loop thread is free to handle other requests while waiting.
         * subscribeOn(boundedElastic) offloads the blocking sleep to a non-event-loop pool.
         */
        public Mono<BigDecimal> getPriceReactive(String productId) {
            return Mono.fromCallable(() -> getPrice(productId))
                       .subscribeOn(Schedulers.boundedElastic());
        }
    }

    // =========================================================================
    // WebClient config
    // =========================================================================

    @Configuration
    public static class WebClientConfig {
        @Bean
        public WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }

    // =========================================================================
    // MVC controller — blocking, sequential & parallel-with-threads
    // =========================================================================

    @RestController
    @RequestMapping("/api/day56/prices")
    @Slf4j
    public static class MvcPriceController {

        private final PriceService priceService;

        public MvcPriceController(PriceService priceService) {
            this.priceService = priceService;
        }

        /**
         * SEQUENTIAL: 3 calls × 50ms = 150ms minimum.
         * Thread is parked/blocked for the entire 150ms.
         */
        @GetMapping("/sequential")
        public PriceBundle sequential() {
            long start = System.currentTimeMillis();
            BigDecimal p1 = priceService.getPrice("PROD-A");
            BigDecimal p2 = priceService.getPrice("PROD-B");
            BigDecimal p3 = priceService.getPrice("PROD-C");
            long elapsed = System.currentTimeMillis() - start;
            log.debug("[Day56] Sequential {} ms", elapsed);
            return new PriceBundle(p1, p2, p3, elapsed, "MVC-sequential");
        }

        /**
         * PARALLEL with threads: 3 CompletableFutures × 50ms = ~50ms total.
         * Better than sequential, BUT still needs 3 extra threads from ForkJoinPool.
         * Under high load, thread pool exhaustion still possible.
         */
        @GetMapping("/parallel-threads")
        public PriceBundle parallelWithThreads() throws Exception {
            long start = System.currentTimeMillis();
            var f1 = CompletableFuture.supplyAsync(() -> priceService.getPrice("PROD-A"));
            var f2 = CompletableFuture.supplyAsync(() -> priceService.getPrice("PROD-B"));
            var f3 = CompletableFuture.supplyAsync(() -> priceService.getPrice("PROD-C"));
            CompletableFuture.allOf(f1, f2, f3).join();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("[Day56] Parallel-threads {} ms", elapsed);
            return new PriceBundle(f1.get(), f2.get(), f3.get(), elapsed, "MVC-parallel-threads");
        }
    }

    // =========================================================================
    // Reactive controller — non-blocking, event-loop
    // =========================================================================

    @RestController
    @RequestMapping("/api/day56/reactive-prices")
    @Slf4j
    public static class ReactivePriceController {

        private final PriceService priceService;

        public ReactivePriceController(PriceService priceService) {
            this.priceService = priceService;
        }

        /**
         * PARALLEL with Mono.zip: 3 subscriptions fire simultaneously.
         * Total latency ≈ max(50ms, 50ms, 50ms) = 50ms.
         * No threads blocked: boundedElastic handles actual sleep offload.
         */
        @GetMapping("/parallel")
        public Mono<PriceBundle> parallelReactive() {
            long start = System.currentTimeMillis();
            return Mono.zip(
                    priceService.getPriceReactive("PROD-A"),
                    priceService.getPriceReactive("PROD-B"),
                    priceService.getPriceReactive("PROD-C")
            ).map(t -> new PriceBundle(
                    t.getT1(), t.getT2(), t.getT3(),
                    System.currentTimeMillis() - start,
                    "WebFlux-parallel"
            ));
        }

        /**
         * BULK parallel with flatMap: processes variable-length list concurrently.
         * flatMap has default concurrency of Integer.MAX_VALUE (all at once).
         * Use flatMap(fn, maxConcurrency) to throttle (e.g., max 8 parallel).
         */
        @GetMapping("/bulk")
        public Mono<List<BigDecimal>> bulkPrices() {
            return Flux.just("PROD-A", "PROD-B", "PROD-C", "PROD-D", "PROD-E")
                       .flatMap(priceService::getPriceReactive)
                       .collectList();
        }
    }

    public record PriceBundle(
            BigDecimal priceA,
            BigDecimal priceB,
            BigDecimal priceC,
            long elapsedMs,
            String strategy
    ) {}
}
