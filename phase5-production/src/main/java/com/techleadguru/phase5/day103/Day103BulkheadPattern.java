package com.techleadguru.phase5.day103;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * DAY 103 — Bulkhead Pattern: Thread Pool Isolation
 *
 * THE PROBLEM:
 *   A single shared thread pool means slow/broken downstream services
 *   can starve ALL request handling threads.
 *
 *   Example without bulkheads:
 *     20 threads total
 *     Payment service is slow (10s response time)
 *     20 concurrent users call /checkout → all 20 threads waiting for payment
 *     → Catalog, user, order endpoints all return 503 (no free threads)
 *
 * BULKHEAD SOLUTION (from ship bulkheads — separate compartments):
 *   Dedicate separate thread pools per resource/concern:
 *   - fastPool:    4 threads (catalog, health, search — fast I/O)
 *   - slowPool:    2 threads (PDF generation, batch export — slow CPU)
 *   - paymentPool: 3 threads (payment service — external, can be slow)
 *
 *   Failure in paymentPool doesn't affect fastPool or slowPool.
 *
 * SIZING FORMULA (revisit from Day58):
 *   Threads = N * (1 + WT/ST)
 *   N = CPU cores
 *   WT = wait time (I/O wait)
 *   ST = service time (CPU busy)
 *   For I/O-bound (WT/ST = 9): threads = N * 10
 *
 * RESILIENCE4J BULKHEAD:
 *   ThreadPoolBulkhead — rejects when pool + queue is full (throws BulkheadFullException)
 *   SemaphoreBulkhead  — limits concurrent calls (no separate thread)
 *
 * OBSERVATION:
 *   Monitor each bulkhead pool:
 *   executor.queued.tasks{pool="payment"} — alert if > 10
 *   executor.active{pool="payment"}       — alert if == maxSize for > 30s
 */
@Slf4j
public class Day103BulkheadPattern {

    // =========================================================================
    // Bulkhead service with two named thread pools
    // =========================================================================

    @Service
    @Slf4j
    public static class BulkheadService {

        private final ExecutorService fastPool;
        private final ExecutorService slowPool;

        private final AtomicInteger fastSucceeded  = new AtomicInteger();
        private final AtomicInteger slowSucceeded  = new AtomicInteger();
        private final AtomicInteger fastRejected   = new AtomicInteger();
        private final AtomicInteger slowRejected   = new AtomicInteger();

        public BulkheadService(int fastPoolSize, int slowPoolSize, int queueCapacity) {
            this.fastPool = createBoundedPool("fast", fastPoolSize, queueCapacity);
            this.slowPool = createBoundedPool("slow", slowPoolSize, queueCapacity);
        }

        public BulkheadService() {
            this(4, 2, 20);
        }

        private ExecutorService createBoundedPool(String name, int size, int queueCapacity) {
            return new ThreadPoolExecutor(
                    size, size,
                    60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    r -> {
                        Thread t = new Thread(r, name + "-bulkhead-" + r.hashCode());
                        t.setDaemon(true);
                        return t;
                    },
                    (task, executor) -> {
                        if (name.equals("fast")) fastRejected.incrementAndGet();
                        else slowRejected.incrementAndGet();
                        log.warn("[Day103] Bulkhead [{}] FULL — rejecting task", name);
                        throw new RejectedExecutionException("Bulkhead [" + name + "] full");
                    }
            );
        }

        /**
         * Execute a fast task (catalog lookup, auth check, health ping).
         * Fails fast if fast pool is saturated — never blocks slow pool.
         */
        public <T> CompletableFuture<T> executeInFastLane(Supplier<T> task) {
            CompletableFuture<T> future = new CompletableFuture<>();
            try {
                fastPool.execute(() -> {
                    try {
                        T result = task.get();
                        fastSucceeded.incrementAndGet();
                        future.complete(result);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            } catch (RejectedExecutionException e) {
                future.completeExceptionally(e);
            }
            return future;
        }

        /**
         * Execute a slow task (PDF generation, report export, batch).
         * Uses separate pool — cannot starve fast-lane threads.
         */
        public <T> CompletableFuture<T> executeInSlowLane(Supplier<T> task) {
            CompletableFuture<T> future = new CompletableFuture<>();
            try {
                slowPool.execute(() -> {
                    try {
                        T result = task.get();
                        slowSucceeded.incrementAndGet();
                        future.complete(result);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });
            } catch (RejectedExecutionException e) {
                future.completeExceptionally(e);
            }
            return future;
        }

        public void shutdown() {
            fastPool.shutdown();
            slowPool.shutdown();
        }

        public BulkheadStats getStats() {
            return new BulkheadStats(
                    fastSucceeded.get(), fastRejected.get(),
                    slowSucceeded.get(), slowRejected.get()
            );
        }

        public record BulkheadStats(int fastSucceeded, int fastRejected,
                                    int slowSucceeded, int slowRejected) {
            public boolean hadFastRejections() { return fastRejected > 0; }
            public boolean hadSlowRejections() { return slowRejected > 0; }
        }
    }

    // =========================================================================
    // Simulation: slow task floods slow pool without affecting fast pool
    // =========================================================================

    public static class BulkheadSimulation {

        /**
         * Submit many slow tasks (to fill slowPool), then verify fast tasks still run.
         */
        public static SimulationResult run(BulkheadService service,
                                           int slowTaskCount, int fastTaskCount,
                                           int taskDurationMs) throws Exception {
            CountDownLatch slowLatch = new CountDownLatch(slowTaskCount);
            CountDownLatch fastLatch = new CountDownLatch(fastTaskCount);

            AtomicInteger fastCompleted = new AtomicInteger();
            AtomicInteger slowCompleted = new AtomicInteger();
            AtomicInteger fastFailed    = new AtomicInteger();
            AtomicInteger slowFailed    = new AtomicInteger();

            // Flood slow lane
            for (int i = 0; i < slowTaskCount; i++) {
                service.executeInSlowLane(() -> {
                            try { Thread.sleep(taskDurationMs); return "slow-done"; }
                            catch (InterruptedException e) { Thread.currentThread().interrupt(); return "interrupted"; }
                        })
                        .whenComplete((r, ex) -> {
                            if (ex != null) slowFailed.incrementAndGet();
                            else slowCompleted.incrementAndGet();
                            slowLatch.countDown();
                        });
            }

            // Fast lane should still work even though slow pool is saturated
            for (int i = 0; i < fastTaskCount; i++) {
                service.executeInFastLane(() -> {
                            try { Thread.sleep(1); return "fast-done"; }
                            catch (InterruptedException e) { Thread.currentThread().interrupt(); return "interrupted"; }
                        })
                        .whenComplete((r, ex) -> {
                            if (ex != null) fastFailed.incrementAndGet();
                            else fastCompleted.incrementAndGet();
                            fastLatch.countDown();
                        });
            }

            fastLatch.await(10, TimeUnit.SECONDS);
            slowLatch.await(30, TimeUnit.SECONDS);

            return new SimulationResult(fastCompleted.get(), fastFailed.get(),
                    slowCompleted.get(), slowFailed.get());
        }

        public record SimulationResult(int fastCompleted, int fastFailed,
                                       int slowCompleted, int slowFailed) {
            /**
             * Fast lane isolation: fast tasks still completed even when slow pool was saturated.
             */
            public boolean fastLaneIsolated() {
                return fastCompleted > 0;
            }
        }
    }
}
