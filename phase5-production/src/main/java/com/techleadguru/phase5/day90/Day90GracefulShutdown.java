package com.techleadguru.phase5.day90;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 90 — Graceful Shutdown: Not Losing In-Flight Requests
 *
 * WITHOUT GRACEFUL SHUTDOWN (default in older Spring Boot):
 *   SIGTERM arrives → Tomcat stops accepting connections → in-flight requests get TCP RST → client sees 502
 *
 * WITH GRACEFUL SHUTDOWN (Spring Boot 2.3+):
 *   application.properties: server.shutdown=graceful
 *   application.properties: spring.lifecycle.timeout-per-shutdown-phase=30s
 *
 *   SIGTERM arrives → Tomcat marks server as "shutting down" (no new connections)
 *                   → waits for active requests to complete (up to timeout)
 *                   → then shuts down
 *
 * SHUTDOWN SEQUENCE (SmartLifecycle):
 *   1. ApplicationContext publishes ContextClosedEvent
 *   2. SmartLifecycle beans: stop() called in phase order (highest phase first)
 *   3. @PreDestroy methods called
 *   4. DestroyBean callbacks
 *
 * K8S CHECKLIST:
 *   1. terminationGracePeriodSeconds >= spring.lifecycle.timeout + buffer
 *   2. preStop hook: sleep 5s (gives time for Kubernetes to remove pod from endpoints)
 *   3. readinessProbe marks pod NotReady before SIGTERM → no new traffic
 *
 * ASYNC TASK SHUTDOWN:
 *   ThreadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true)
 *   ThreadPoolTaskExecutor.setAwaitTerminationSeconds(30)
 *   These ensure @Async tasks complete before ApplicationContext closes.
 */
@Slf4j
public class Day90GracefulShutdown {

    // =========================================================================
    // Tracks in-flight work to demonstrate draining
    // =========================================================================

    @Service
    @Slf4j
    public static class OrderProcessingService {

        private final AtomicInteger inFlight    = new AtomicInteger();
        private final AtomicInteger completed   = new AtomicInteger();
        private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
        private final CountDownLatch shutdownLatch = new CountDownLatch(1);

        /**
         * Simulates processing an order (I/O-bound work).
         * Uses a guard so no new orders are accepted during shutdown.
         */
        @Async
        public CompletableFuture<String> processOrder(String orderId, int processingMs) {
            if (shuttingDown.get()) {
                log.warn("[Day90] Refusing order {} — service is shutting down", orderId);
                return CompletableFuture.completedFuture("REJECTED_SHUTDOWN");
            }
            inFlight.incrementAndGet();
            try {
                Thread.sleep(processingMs);
                int c = completed.incrementAndGet();
                log.info("[Day90] Completed order {} (total={})", orderId, c);
                return CompletableFuture.completedFuture("PROCESSED-" + orderId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return CompletableFuture.completedFuture("INTERRUPTED");
            } finally {
                inFlight.decrementAndGet();
            }
        }

        @PreDestroy
        public void onShutdown() throws InterruptedException {
            log.info("[Day90] @PreDestroy fired — draining in-flight orders...");
            shuttingDown.set(true);

            // Wait up to 10 seconds for in-flight tasks to complete
            long deadline = System.currentTimeMillis() + 10_000;
            while (inFlight.get() > 0 && System.currentTimeMillis() < deadline) {
                log.info("[Day90] Still {} in-flight orders — waiting...", inFlight.get());
                Thread.sleep(500);
            }

            if (inFlight.get() > 0) {
                log.warn("[Day90] Shutdown timed out — {} orders still in-flight!", inFlight.get());
            } else {
                log.info("[Day90] All orders drained cleanly. Shutting down.");
            }
        }

        public int getInFlight()   { return inFlight.get(); }
        public int getCompleted()  { return completed.get(); }
        public boolean isShuttingDown() { return shuttingDown.get(); }
    }

    // =========================================================================
    // Manual drain helper (for tests — not Spring-managed)
    // =========================================================================

    public static class WorkDrainer {

        private final ExecutorService executor;
        private final AtomicInteger activeCount = new AtomicInteger();
        private final AtomicBoolean accepting   = new AtomicBoolean(true);

        public WorkDrainer(int threads) {
            this.executor = Executors.newFixedThreadPool(threads, r -> {
                Thread t = new Thread(r, "drain-worker-" + r.hashCode());
                t.setDaemon(true);
                return t;
            });
        }

        public Future<Void> submit(Runnable task) {
            if (!accepting.get()) {
                throw new IllegalStateException("WorkDrainer is shutting down");
            }
            activeCount.incrementAndGet();
            return executor.submit(() -> {
                try {
                    task.run();
                    return null;
                } finally {
                    activeCount.decrementAndGet();
                }
            });
        }

        /**
         * Signal no new work, wait for in-flight to complete.
         */
        public boolean drain(long timeoutMs) throws InterruptedException {
            accepting.set(false);
            executor.shutdown();
            return executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        }

        public int getActiveCount() { return activeCount.get(); }
        public boolean isAccepting() { return accepting.get(); }
    }

    // =========================================================================
    // Configuration notes (shown at startup via log)
    // =========================================================================

    @Service
    @Slf4j
    public static class ShutdownConfigAuditor {

        public record ShutdownConfig(boolean gracefulEnabled, String timeoutPerPhase,
                                     boolean asyncWaitsForTasks, int asyncTerminationSecs) {}

        public ShutdownConfig getRecommendedConfig() {
            return new ShutdownConfig(true, "30s", true, 30);
        }

        /**
         * Returns the recommended application.properties snippet for graceful shutdown.
         */
        public String configSnippet() {
            return """
                    # application.properties — Graceful Shutdown
                    server.shutdown=graceful
                    spring.lifecycle.timeout-per-shutdown-phase=30s
                    
                    # AsyncConfig bean (ThreadPoolTaskExecutor):
                    #   executor.setWaitForTasksToCompleteOnShutdown(true)
                    #   executor.setAwaitTerminationSeconds(30)
                    
                    # K8s Deployment pod spec:
                    #   terminationGracePeriodSeconds: 60   # > Spring timeout + buffer
                    #   lifecycle:
                    #     preStop:
                    #       exec:
                    #         command: ["/bin/sh", "-c", "sleep 5"]
                    """;
        }
    }
}
