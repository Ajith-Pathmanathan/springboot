package com.techleadguru.phase5.day89;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 89 — Thread Pool Saturation: Queue Buildup and Rejection Policy
 *
 * THREAD POOL ANATOMY (ThreadPoolExecutor):
 *   corePoolSize    — threads always kept alive, even idle
 *   maximumPoolSize — max threads when queue is full
 *   workQueue       — queue between core threads and max threads
 *   keepAliveTime   — idle above-core threads removed after this delay
 *   rejectedExecutionHandler — what happens when both pool AND queue are full
 *
 * QUEUEING BEHAVIOR:
 *   1. Submit task → if running < core → create new thread → execute
 *   2. Submit task → if running == core → put in queue
 *   3. Submit task → if queue full AND running < max → create new thread → execute
 *   4. Submit task → if queue full AND running == max → RejectionPolicy fires !
 *
 * REJECTION POLICIES:
 *   AbortPolicy         → throw RejectedExecutionException (DEFAULT)
 *   CallerRunsPolicy    → run in the calling thread (backpressure on submitter)
 *   DiscardPolicy       → silently drop the task (DANGEROUS — data loss)
 *   DiscardOldestPolicy → drop oldest queued task, retry submit (DANGEROUS)
 *
 * SATURATION SYMPTOMS:
 *   - RejectedExecutionException in logs
 *   - Increasing queue size in metrics: executor.queued.tasks
 *   - Response latency climbing (tasks waiting in queue)
 *   - CPU mostly idle (threads waiting for I/O, not actually working)
 *
 * DIAGNOSIS:
 *   Actuator: GET /actuator/metrics/executor.active (should be < poolSize)
 *             GET /actuator/metrics/executor.queued (should be low)
 *             GET /actuator/metrics/executor.completed.tasks (should grow)
 *
 * FIX:
 *   1. Tune pool size (Day 58 formula)
 *   2. Use CallerRunsPolicy for backpressure instead of dropping
 *   3. Add circuit breaker (Day 144) to fail fast
 *   4. Move slow I/O work to dedicated pool, keep request thread pool for fast work
 */
@Slf4j
public class Day89ThreadPoolSaturation {

    // =========================================================================
    // Custom thread pool with monitoring
    // =========================================================================

    public static class MonitoredThreadPool {

        private final ThreadPoolExecutor executor;
        private final AtomicInteger rejectedCount = new AtomicInteger();

        public MonitoredThreadPool(int coreSize, int maxSize, int queueCapacity,
                                   boolean useCallerRunsOnReject) {
            RejectedExecutionHandler handler;
            if (useCallerRunsOnReject) {
                handler = (task, pool) -> {
                    rejectedCount.incrementAndGet();
                    log.warn("[Day89] Queue full — running in caller thread (CallerRuns)");
                    task.run();
                };
            } else {
                handler = (task, pool) -> {
                    rejectedCount.incrementAndGet();
                    log.error("[Day89] Queue full — REJECTING task (AbortPolicy)");
                    throw new RejectedExecutionException("Thread pool saturated");
                };
            }

            this.executor = new ThreadPoolExecutor(
                    coreSize, maxSize,
                    60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    r -> {
                        Thread t = new Thread(r);
                        t.setName("monitored-pool-" + t.getId());
                        t.setDaemon(true);
                        return t;
                    },
                    handler
            );
        }

        public Future<String> submit(Callable<String> task) {
            return executor.submit(task);
        }

        public void execute(Runnable task) {
            executor.execute(task);
        }

        public PoolStats stats() {
            return new PoolStats(
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getCompletedTaskCount(),
                    executor.getPoolSize(),
                    executor.getMaximumPoolSize(),
                    rejectedCount.get()
            );
        }

        public void shutdown() { executor.shutdown(); }

        public record PoolStats(int active, int queued, long completed,
                                int poolSize, int maxPoolSize, int rejected) {}
    }

    // =========================================================================
    // Service that can saturate a pool
    // =========================================================================

    @Service
    @Slf4j
    public static class ReportGenerationService {

        private final AtomicInteger processedCount = new AtomicInteger();
        private final AtomicInteger rejectedCount  = new AtomicInteger();

        /**
         * Simulates a workload where tasks take variable time.
         * When submitted to an undersized pool, rejection will occur.
         */
        public SaturationResult bombardPool(MonitoredThreadPool pool,
                                             int taskCount, int taskDurationMs)
                throws InterruptedException {
            CountDownLatch done = new CountDownLatch(taskCount);
            AtomicInteger successes = new AtomicInteger();
            AtomicInteger rejects   = new AtomicInteger();

            for (int i = 0; i < taskCount; i++) {
                final int idx = i;
                try {
                    pool.execute(() -> {
                        try {
                            Thread.sleep(taskDurationMs);
                            processedCount.incrementAndGet();
                            successes.incrementAndGet();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    rejectedCount.incrementAndGet();
                    rejects.incrementAndGet();
                    done.countDown();
                }
            }

            done.await(30, TimeUnit.SECONDS);
            return new SaturationResult(taskCount, successes.get(), rejects.get(),
                    pool.stats());
        }

        public int getProcessedCount() { return processedCount.get(); }
        public int getRejectedCount()  { return rejectedCount.get(); }

        public record SaturationResult(int submitted, int succeeded, int rejected,
                                       MonitoredThreadPool.PoolStats poolStats) {
            public boolean hadRejections()  { return rejected > 0; }
            public double rejectionRate()   { return (double)rejected / submitted; }
        }
    }

    // =========================================================================
    // @Async version — shows rejection with Spring TaskExecutor
    // =========================================================================

    @Service
    @Slf4j
    public static class AsyncEmailService {

        private final AtomicInteger sentCount     = new AtomicInteger();
        private final AtomicInteger rejectedCount = new AtomicInteger();

        @Async
        public CompletableFuture<String> sendEmail(String to) {
            try {
                Thread.sleep(50); // simulate email SMTP call
                int count = sentCount.incrementAndGet();
                log.debug("[Day89] Sent email #{} to {} on thread {}", count, to, Thread.currentThread().getName());
                return CompletableFuture.completedFuture("SENT-" + to);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return CompletableFuture.completedFuture("INTERRUPTED");
            }
        }

        public int getSentCount()     { return sentCount.get(); }
        public int getRejectedCount() { return rejectedCount.get(); }
    }
}
