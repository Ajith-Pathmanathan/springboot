package com.techleadguru.phase4.day58;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;

/**
 * DAY 58 — ThreadPoolTaskExecutor Tuning
 *
 * THREAD POOL LIFECYCLE:
 *   Request arrives:
 *     1. If activeThreads < corePoolSize → spawn new thread immediately
 *     2. If activeThreads == corePoolSize → put task in queue
 *     3. If queue is full → spawn new thread up to maxPoolSize
 *     4. If maxPoolSize reached AND queue full → RejectedExecutionException (or policy)
 *
 * KEY CONFIGURATION PROPERTIES:
 *   corePoolSize      = always-alive threads (set to expected sustained concurrency)
 *   maxPoolSize       = burst capacity (Tomcat default = 200, async pool different)
 *   queueCapacity     = buffer between core and max (use 0 for "spawn immediately")
 *   keepAliveSeconds  = how long idle threads above coreSize stay alive (default 60s)
 *   threadNamePrefix  = makes thread dumps readable → "email-exec-1", "report-exec-1"
 *   waitForTasksToComplete = true → graceful shutdown waits for queued tasks
 *   awaitTerminationSeconds = 30 → max wait during shutdown
 *
 * FORMULA (Latency-focused):
 *   corePoolSize = N_CPU × (1 + I/O_wait_ratio)
 *   Example: 8 cores × (1 + 9) = 80 threads for I/O-bound work (90% wait time)
 *
 * FORMULA (Throughput-focused, Little's Law):
 *   poolSize = throughput_per_second × avg_latency_seconds
 *   Example: 100 req/s × 0.5s avg = 50 threads needed
 *
 * REJECTION POLICIES:
 *   AbortPolicy        → default, throws RejectedExecutionException → logs + metrics
 *   CallerRunsPolicy   → executes on the CALLER'S thread → backpressure, no data loss
 *   DiscardPolicy      → silently drops task (DANGER: silent data loss)
 *   DiscardOldestPolicy → discards oldest queued task (DANGER: unpredictable)
 *
 * MULTIPLE EXECUTORS:
 *   Use different executors for different task types (CPU-bound vs I/O-bound).
 *   @Async("ioExecutor") → runs in I/O-optimized pool
 *   @Async("cpuExecutor") → runs in CPU-optimized pool
 */
@Slf4j
public class Day58ThreadPoolTuning {

    // =========================================================================
    // Multiple named thread pools
    // =========================================================================

    @Configuration
    @Slf4j
    public static class ExecutorConfig {

        /**
         * I/O-bound executor: bigger pool because threads mostly wait.
         * Handles email, HTTP calls, file reads — high wait ratio.
         */
        @Bean("ioExecutor")
        public Executor ioExecutor() {
            ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
            exec.setCorePoolSize(10);
            exec.setMaxPoolSize(50);
            exec.setQueueCapacity(100);
            exec.setThreadNamePrefix("io-exec-");
            exec.setKeepAliveSeconds(60);
            exec.setWaitForTasksToCompleteOnShutdown(true);
            exec.setAwaitTerminationSeconds(30);
            // CallerRunsPolicy = backpressure: if pool full, caller's thread does the work
            exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            exec.initialize();
            log.debug("[Day58] ioExecutor created: core=10, max=50, queue=100");
            return exec;
        }

        /**
         * CPU-bound executor: smaller pool matching CPU count.
         * Handles data transformation, PDF generation — CPU intensive.
         */
        @Bean("cpuExecutor")
        public Executor cpuExecutor() {
            int cpuCount = Runtime.getRuntime().availableProcessors();
            ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
            exec.setCorePoolSize(cpuCount);
            exec.setMaxPoolSize(cpuCount * 2);
            exec.setQueueCapacity(50);
            exec.setThreadNamePrefix("cpu-exec-");
            exec.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
            exec.initialize();
            log.debug("[Day58] cpuExecutor created: core={}, max={}", cpuCount, cpuCount * 2);
            return exec;
        }
    }

    // =========================================================================
    // Services using named executors
    // =========================================================================

    @Service
    @Slf4j
    public static class ReportService {

        @Async("ioExecutor")
        public CompletableFuture<String> fetchRemoteDataAsync(String endpoint) {
            log.info("[Day58] Fetching {} on thread {}", endpoint, Thread.currentThread().getName());
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return CompletableFuture.completedFuture("data-from-" + endpoint);
        }

        @Async("cpuExecutor")
        public CompletableFuture<String> generatePdfAsync(String reportId) {
            log.info("[Day58] Generating PDF {} on thread {}", reportId, Thread.currentThread().getName());
            // CPU-intensive work: just simulate with a loop
            long sum = IntStream.range(0, 100_000).asLongStream().sum();
            return CompletableFuture.completedFuture("pdf-" + reportId + "-" + sum);
        }
    }

    // =========================================================================
    // Controller: pool stats endpoint
    // =========================================================================

    @RestController
    @RequestMapping("/api/day58/thread-pools")
    @Slf4j
    public static class ThreadPoolController {

        @Autowired
        @Qualifier("ioExecutor")
        private Executor ioExecutor;

        @Autowired
        @Qualifier("cpuExecutor")
        private Executor cpuExecutor;

        @Autowired
        private ReportService reportService;

        @GetMapping("/stats")
        public Map<String, Object> poolStats() {
            ThreadPoolTaskExecutor io = (ThreadPoolTaskExecutor) ioExecutor;
            ThreadPoolTaskExecutor cpu = (ThreadPoolTaskExecutor) cpuExecutor;
            return Map.of(
                    "ioExecutor", Map.of(
                            "activeCount", io.getActiveCount(),
                            "poolSize", io.getPoolSize(),
                            "corePoolSize", io.getCorePoolSize(),
                            "maxPoolSize", io.getMaxPoolSize(),
                            "queueSize", io.getThreadPoolExecutor().getQueue().size()
                    ),
                    "cpuExecutor", Map.of(
                            "activeCount", cpu.getActiveCount(),
                            "poolSize", cpu.getPoolSize(),
                            "corePoolSize", cpu.getCorePoolSize(),
                            "maxPoolSize", cpu.getMaxPoolSize()
                    )
            );
        }

        @PostMapping("/parallel-fetch")
        public CompletableFuture<List<String>> parallelFetch() {
            var f1 = reportService.fetchRemoteDataAsync("service-A");
            var f2 = reportService.fetchRemoteDataAsync("service-B");
            var f3 = reportService.fetchRemoteDataAsync("service-C");
            return CompletableFuture.allOf(f1, f2, f3)
                    .thenApply(v -> List.of(f1.join(), f2.join(), f3.join()));
        }
    }
}
