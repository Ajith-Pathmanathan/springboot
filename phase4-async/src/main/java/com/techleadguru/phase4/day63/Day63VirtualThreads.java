package com.techleadguru.phase4.day63;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * DAY 63 — Virtual Threads (Java 21 + Spring Boot 3.2+)
 *
 * BACKGROUND: Project Loom (JEP 444 — Java 21)
 *   Platform threads (OS threads): expensive to create/context-switch (~2MB stack)
 *   Virtual threads: cheap, managed by JVM (~few KB, creates millions)
 *
 * HOW VIRTUAL THREADS WORK:
 *   A virtual thread is "mounted" on a platform thread (carrier thread) while running.
 *   When it blocks (I/O, sleep, synchronized), it "unmounts" — carrier is freed.
 *   JVM mounts a different virtual thread on the same carrier — no OS context switch.
 *
 * SPRING BOOT VIRTUAL THREAD SUPPORT:
 *   spring.threads.virtual.enabled=true  → enables for:
 *     • Tomcat/Jetty/Undertow request threads → each HTTP request on a virtual thread
 *     • @Async default TaskExecutor          → virtual threads for @Async methods
 *     • (@Scheduled gets a separate config)
 *
 * WHEN VIRTUAL THREADS HELP:
 *   ✅ I/O-bound work: DB queries, HTTP calls, file reads (blocking releases carrier)
 *   ✅ High-concurrency REST APIs with many concurrent requests
 *   ✅ Replace thread-pool sizing headaches (just create as many as needed)
 *
 * WHEN VIRTUAL THREADS DON'T HELP:
 *   ❌ CPU-bound work: the carrier is held the whole time — same as platform threads
 *   ❌ Code with ThreadLocal pinning (still works, but no benefit)
 *   ❌ synchronized methods: older JDK pins carrier (JEP 491 fixes in JDK 24)
 *
 * PINNING (JDK 21 limitation):
 *   When a virtual thread is inside a synchronized block and calls blocking I/O,
 *   it PINS the carrier thread — no other virtual thread can use it.
 *   → Use ReentrantLock instead of synchronized for code used in virtual threads.
 *
 * THREAD NAME FORMAT:
 *   Platform thread: "test-async-1"  (pool name + sequential number)
 *   Virtual thread:  "VirtualThread[#42]/runnable@ForkJoinPool..."
 *   Or with thread factory: "" (empty name by default for virtual threads)
 */
@Slf4j
public class Day63VirtualThreads {

    // =========================================================================
    // Thread executor factory
    // =========================================================================

    @Configuration
    public static class VirtualThreadConfig {

        /**
         * Executor backed by virtual threads — one virtual thread per task.
         * No pooling needed: virtual threads are cheap enough to create on demand.
         */
        @Bean("virtualThreadExecutor")
        public Executor virtualThreadExecutor() {
            return Executors.newVirtualThreadPerTaskExecutor();
        }

        /**
         * For comparison: traditional platform thread pool.
         */
        @Bean("platformThreadExecutor")
        public Executor platformThreadExecutor() {
            return Executors.newFixedThreadPool(10, Thread.ofPlatform().name("platform-", 1).factory());
        }
    }

    // =========================================================================
    // I/O-bound service to demonstrate virtual thread benefit
    // =========================================================================

    @Service
    @Slf4j
    public static class ConcurrentFetchService {

        private final Executor virtualThreadExecutor;
        private final Executor platformThreadExecutor;

        public ConcurrentFetchService(Executor virtualThreadExecutor, Executor platformThreadExecutor) {
            this.virtualThreadExecutor = virtualThreadExecutor;
            this.platformThreadExecutor = platformThreadExecutor;
        }

        /**
         * Runs N tasks each sleeping 100ms, using virtual threads.
         * With virtual threads: all N mount/unmount cheaply → total ~100ms for any N.
         * With 10 platform threads: tasks/(10 threads) batches → total ~N/10*100ms.
         */
        public long runConcurrent(int taskCount, Executor executor) throws InterruptedException {
            long start = System.currentTimeMillis();
            CountDownLatch latch = new CountDownLatch(taskCount);
            for (int i = 0; i < taskCount; i++) {
                executor.execute(() -> {
                    try {
                        Thread.sleep(100); // simulate blocking I/O
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            return System.currentTimeMillis() - start;
        }

        public boolean nextTaskIsVirtual() throws ExecutionException, InterruptedException {
            return CompletableFuture.supplyAsync(
                    () -> Thread.currentThread().isVirtual(),
                    virtualThreadExecutor
            ).get();
        }
    }

    // =========================================================================
    // Service demonstrating @Async on virtual threads (when enabled via property)
    // =========================================================================

    @Service
    @Slf4j
    public static class AsyncVirtualService {

        @Async("virtualThreadExecutor")
        public CompletableFuture<Boolean> runOnVirtualThread() {
            boolean isVirtual = Thread.currentThread().isVirtual();
            log.info("[Day63] runOnVirtualThread: isVirtual={}, thread={}", isVirtual, Thread.currentThread());
            return CompletableFuture.completedFuture(isVirtual);
        }
    }

    // =========================================================================
    // Demo controller
    // =========================================================================

    @RestController
    @RequestMapping("/api/day63/virtual-threads")
    @Slf4j
    public static class VirtualThreadController {

        private final ConcurrentFetchService fetchService;

        public VirtualThreadController(ConcurrentFetchService fetchService) {
            this.fetchService = fetchService;
        }

        @GetMapping("/current-thread")
        public java.util.Map<String, Object> currentThread() {
            Thread t = Thread.currentThread();
            return java.util.Map.of(
                    "name", t.getName(),
                    "isVirtual", t.isVirtual(),
                    "threadId", t.threadId()
            );
        }

        @GetMapping("/concurrency-test")
        public java.util.Map<String, Long> concurrencyTest(@RequestParam(defaultValue = "20") int tasks)
                throws InterruptedException {
            long virtualMs = fetchService.runConcurrent(tasks, Executors.newVirtualThreadPerTaskExecutor());
            long platformMs = fetchService.runConcurrent(tasks, new java.util.concurrent.ForkJoinPool(10));
            return java.util.Map.of("virtualThreadMs", virtualMs, "platformThread10Ms", platformMs);
        }
    }
}
