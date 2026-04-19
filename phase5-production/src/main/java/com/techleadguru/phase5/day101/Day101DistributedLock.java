package com.techleadguru.phase5.day101;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;

/**
 * DAY 101 — Distributed Lock with Redis SETNX
 *
 * THE PROBLEM:
 *   Without distributed lock, when multiple instances run concurrently:
 *   - Scheduled jobs run on all N instances simultaneously
 *   - Inventory updates get double-decremented
 *   - Emails get sent N times (once per instance)
 *
 * REDIS SETNX PATTERN:
 *   SETNX key value  — SET if Not eXists (atomic)
 *   Equivalent: SET key value NX EX <ttlSeconds>
 *   → Only one instance gets the lock; others skip their work
 *   → TTL ensures lock is released even if holder crashes
 *
 * REDISSON (production-grade Redis client):
 *   RLock lock = redissonClient.getLock("myLock");
 *   if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
 *       try { doWork(); } finally { lock.unlock(); }
 *   }
 *   Features: watchdog (auto-extends TTL while holder is alive), fair lock, read/write lock
 *
 * FENCING TOKEN PATTERN (for strict safety):
 *   Redis returns a monotonically increasing token when lock is acquired.
 *   Storage layer rejects writes with older tokens (guards against clock skew).
 *   Use Redlock library for multi-node Redis (Martin Kleppmann's analysis).
 *
 * SIMPLER ALTERNATIVES:
 *   ShedLock — annotation-based distributed lock for @Scheduled jobs
 *   DB-based lock — SELECT FOR UPDATE (works without Redis)
 *
 * NOTE: In tests, Redisson is excluded (no Redis server in test environment).
 * The LockProvider abstraction below allows swapping in a test-double.
 */
public class Day101DistributedLock {

    // =========================================================================
    // Lock provider abstraction (enables testing without Redis)
    // =========================================================================

    public interface LockProvider {
        /**
         * Try to acquire a lock with the given key.
         * Returns a lock handle if successful, empty if lock is already held.
         */
        Optional<LockHandle> tryAcquire(String lockKey, long ttlMs);
    }

    public interface LockHandle extends AutoCloseable {
        String getLockKey();
        @Override
        void close(); // release the lock
    }

    /**
     * Execute a task only if the distributed lock can be acquired.
     * @return true if the task ran, false if the lock was held by another instance.
     */
    public static boolean runWithLock(LockProvider provider, String key, long ttlMs, Runnable task) {
        Optional<LockHandle> handle = provider.tryAcquire(key, ttlMs);
        if (handle.isEmpty()) {
            return false;
        }
        try (LockHandle lock = handle.get()) {
            task.run();
            return true;
        }
    }

    public static <T> Optional<T> supplyWithLock(LockProvider provider, String key,
                                                   long ttlMs, Supplier<T> supplier) {
        Optional<LockHandle> handle = provider.tryAcquire(key, ttlMs);
        if (handle.isEmpty()) {
            return Optional.empty();
        }
        try (LockHandle lock = handle.get()) {
            return Optional.ofNullable(supplier.get());
        }
    }

    // =========================================================================
    // In-memory lock provider for tests (simulates Redis SETNX behavior)
    // =========================================================================

    public static class InMemoryLockProvider implements LockProvider {

        private final ConcurrentHashMap<String, Long> locks = new ConcurrentHashMap<>();
        private final AtomicLong acquireCount = new AtomicLong();
        private final AtomicLong rejectCount  = new AtomicLong();

        @Override
        public Optional<LockHandle> tryAcquire(String lockKey, long ttlMs) {
            long expiry = System.currentTimeMillis() + ttlMs;

            // Atomically set only if key absent or expired
            Long existing = locks.get(lockKey);
            if (existing != null && System.currentTimeMillis() < existing) {
                rejectCount.incrementAndGet();
                return Optional.empty(); // lock held
            }

            // putIfAbsent handles concurrent attempts
            Long prev = locks.putIfAbsent(lockKey, expiry);
            if (prev != null && System.currentTimeMillis() < prev) {
                rejectCount.incrementAndGet();
                return Optional.empty();
            }
            locks.put(lockKey, expiry); // won the race (or replaced expired)
            acquireCount.incrementAndGet();

            return Optional.of(new InMemoryLockHandle(lockKey));
        }

        public long getAcquireCount() { return acquireCount.get(); }
        public long getRejectCount()  { return rejectCount.get(); }
        public boolean isHeld(String key) {
            Long expiry = locks.get(key);
            return expiry != null && System.currentTimeMillis() < expiry;
        }

        private class InMemoryLockHandle implements LockHandle {
            private final String key;
            InMemoryLockHandle(String key) { this.key = key; }

            @Override
            public String getLockKey() { return key; }

            @Override
            public void close() { locks.remove(key); }
        }
    }

    // =========================================================================
    // Redis SETNX illustration (documents what Redisson does under the hood)
    // =========================================================================

    public static class RedisLockDocumentation {

        /**
         * Pseudocode equivalent of what Redisson's tryLock does with Redis.
         */
        public static String setnxPattern() {
            return """
                    # Redis commands that implement distributed lock:
                    
                    # ACQUIRE (atomic, single command):
                    SET lock:invoice-job <unique-value> NX EX 30
                    # NX = only set if key does Not eXist
                    # EX = expire after 30 seconds (TTL — crash safety)
                    # Returns: OK if acquired, nil if already held
                    
                    # RELEASE (Lua script for atomicity):
                    # Must check value matches before deleting
                    # (prevents releasing another instance's lock after TTL)
                    if redis.call("GET", KEYS[1]) == ARGV[1] then
                        return redis.call("DEL", KEYS[1])
                    else
                        return 0
                    end
                    
                    # WATCHDOG (Redisson):
                    # Every ttl/3 seconds, if process is still alive:
                    EXPIRE lock:invoice-job 30   # reset TTL
                    # → Lock auto-expires if process crashes (no renewal)
                    """;
        }

        public static String redissonUsageExample() {
            return """
                    // Redisson distributed lock (production code — requires running Redis):
                    
                    @Service
                    public class InvoiceScheduler {
                        private final RedissonClient redissonClient;
                    
                        @Scheduled(cron = "0 0 * * * *")  // every hour
                        public void generateInvoices() {
                            RLock lock = redissonClient.getLock("lock:generate-invoices");
                            boolean acquired = lock.tryLock(0, 30, TimeUnit.SECONDS);
                            if (!acquired) {
                                log.info("Another instance is running the invoice job, skipping");
                                return;
                            }
                            try {
                                doGenerateInvoices();
                            } finally {
                                lock.unlock();
                            }
                        }
                    }
                    """;
        }
    }
}
