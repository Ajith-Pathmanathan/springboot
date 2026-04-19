package com.techleadguru.phase5.day83;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 83 — ThreadLocal Leak in Thread Pools
 *
 * THREADLOCAL BASICS:
 *   ThreadLocal<T> stores one value per thread.
 *   Thread-pool threads are REUSED — they are never destroyed between requests.
 *   If you set ThreadLocal inside a request but don't call remove() when done,
 *   the value persists on the thread for the next request handled by that thread.
 *
 * THE LEAK MECHANISM:
 *   Thread pool keeps 8 threads alive → 8 ThreadLocal values alive (even if 
 *   the user's session is gone). The value (e.g. a 1MB object) stays in memory
 *   as long as the thread lives — which is forever for pool threads.
 *
 * SYMPTOMS IN PRODUCTION:
 *   - Requests see stale data: "Why does user A see user B's data?"
 *   - Memory grows linearly with thread count (not request count)
 *   - Old tenant ID / user ID appears in logs for wrong requests
 *
 * REAL EXAMPLES:
 *   - MDC (Mapped Diagnostic Context): stores correlation-id per thread
 *   - SecurityContextHolder: stores Authentication per thread
 *   - Hibernate: Session stored in ThreadLocal in older versions
 *   - RequestContextHolder: Spring's request attributes
 *
 * FIX PATTERN:
 *   Always call threadLocal.remove() in a finally block after the request.
 *   Spring's OncePerRequestFilter does this for MDC.
 *   TaskDecorator copies + clears for @Async.
 */
@Slf4j
public class Day83ThreadLocalLeak {

    // =========================================================================
    // Broken: ThreadLocal not removed after request
    // =========================================================================

    @Slf4j
    public static class RequestContextLeaky {

        private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

        public record UserContext(String userId, String tenantId, byte[] sessionData) {}

        public static void setContext(String userId, String tenantId) {
            CONTEXT.set(new UserContext(userId, tenantId, new byte[1024 * 10])); // 10KB per request
        }

        /** BROKEN: no remove → context stays on thread until next request overwrites it */
        public static String getUserId() {
            UserContext ctx = CONTEXT.get();
            return ctx != null ? ctx.userId() : "NOT_SET";
        }

        public static String getTenantId() {
            UserContext ctx = CONTEXT.get();
            return ctx != null ? ctx.tenantId() : "NOT_SET";
        }

        /** BUG: if this is NOT called, next request on same thread sees stale data */
        public static void clear() { CONTEXT.remove(); }

        public static boolean hasContext() { return CONTEXT.get() != null; }
    }

    // =========================================================================
    // Fixed: ThreadLocal with guaranteed removal via try/finally
    // =========================================================================

    @Slf4j
    public static class RequestContextSafe {

        private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
        private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

        /**
         * FIXED: Wraps execution so that ThreadLocal is always removed after the task.
         * Usage pattern mirrors OncePerRequestFilter.
         */
        public static <T> T withContext(String userId, String tenantId, Callable<T> task) throws Exception {
            USER_ID.set(userId);
            TENANT_ID.set(tenantId);
            log.debug("[Day83] Set context for user={} tenant={} on thread={}",
                    userId, tenantId, Thread.currentThread().getName());
            try {
                return task.call();
            } finally {
                USER_ID.remove();   // CRITICAL: always clear, even on exception
                TENANT_ID.remove();
                log.debug("[Day83] Cleared context on thread={}", Thread.currentThread().getName());
            }
        }

        public static String getUserId()   { return USER_ID.get(); }
        public static String getTenantId() { return TENANT_ID.get(); }
        public static boolean hasContext() { return USER_ID.get() != null; }
    }

    // =========================================================================
    // Stale data demo — proves the leak across thread reuse
    // =========================================================================

    @Slf4j
    public static class StaleDataDemo {

        private final ExecutorService pool;
        private final AtomicInteger staleReadCount = new AtomicInteger();
        private final AtomicInteger totalReadCount  = new AtomicInteger();

        public StaleDataDemo(int poolSize) {
            this.pool = Executors.newFixedThreadPool(poolSize);
        }

        /**
         * Simulates the BROKEN pattern.
         * Task 1 sets context but forgets to remove it.
         * Task 2 on the same thread reads the stale value.
         */
        public List<String> runLeakyRequests(int count) throws InterruptedException {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                final int idx = i;
                futures.add(pool.submit(() -> {
                    totalReadCount.incrementAndGet();
                    if (idx % 2 == 0) {
                        // Even tasks: set context but forget to remove
                        RequestContextLeaky.setContext("user-" + idx, "tenant-" + idx);
                        return "SET user-" + idx;
                    } else {
                        // Odd tasks: read — may see stale value from previous task on same thread
                        String userId = RequestContextLeaky.getUserId();
                        if (!"NOT_SET".equals(userId)) staleReadCount.incrementAndGet();
                        return "READ " + userId;
                    }
                }));
            }
            List<String> results = new ArrayList<>();
            for (Future<String> f : futures) {
                try { results.add(f.get(3, TimeUnit.SECONDS)); }
                catch (Exception e) { results.add("ERROR: " + e.getMessage()); }
            }
            return results;
        }

        public int getStaleReadCount() { return staleReadCount.get(); }
        public int getTotalReadCount() { return totalReadCount.get(); }

        public void shutdown() {
            pool.shutdown();
            // Clean up all remaining ThreadLocal values set by pool threads
            RequestContextLeaky.clear();
        }
    }
}
