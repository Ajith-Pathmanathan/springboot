package com.techleadguru.phase4.day75;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * DAY 75 — ConcurrentHashMap vs HashMap Under Load
 *
 * THE PROBLEM WITH HashMap IN MULTITHREADED CODE:
 *
 *   HashMap is NOT thread-safe. Under concurrent access you get:
 *   1. Lost updates  — two threads write simultaneously; one update is silently lost
 *   2. Corrupt state — infinite loop in Java 6 (resize race), NPE, wrong values
 *   3. ConcurrentModificationException — iterator + concurrent modification
 *
 *   Example (HashMap count example):
 *   Thread A: value = map.get("x");  // reads 5
 *   Thread B: value = map.get("x");  // reads 5
 *   Thread A: map.put("x", 5 + 1);  // writes 6
 *   Thread B: map.put("x", 5 + 1);  // writes 6 — Thread A's increment is LOST!
 *   Expected: 7. Got: 6.
 *
 * ConcurrentHashMap SOLUTIONS:
 *
 *   map.put(k, v)                 → atomic for the single put (but not compound ops)
 *   map.compute(k, fn)            → atomic read-modify-write
 *   map.merge(k, 1, Integer::sum) → atomic increment (best for counters)
 *   map.computeIfAbsent(k, fn)    → atomic create-if-absent
 *   map.putIfAbsent(k, v)         → atomic put-if-not-exists
 *
 * SEGMENTED LOCKING (Java 7):
 *   16 segments, each with its own lock. 16 threads can write simultaneously.
 *
 * FINE-GRAINED LOCKING (Java 8+):
 *   Lock per bucket. N threads can write to N different buckets simultaneously.
 *   Better throughput than synchronized(map) for high concurrency.
 *
 * WHEN TO USE WHAT:
 *   Single thread           → HashMap (no overhead)
 *   Concurrent + high read  → ConcurrentHashMap
 *   Read-mostly, rare write → Collections.unmodifiableMap() or CopyOnWriteArrayList
 *   Need sorted+concurrent  → ConcurrentSkipListMap (also implements SortedMap)
 *   Simple synchronized     → Collections.synchronizedMap (coarse lock, lower throughput)
 */
@Slf4j
public class Day75ConcurrentHashMap {

    // =========================================================================
    // Race condition demo — HashMap is unsafe under concurrent writes
    // =========================================================================

    public static class RaceConditionCounter {

        /**
         * Increments a counter N times across T threads using a plain HashMap.
         *
         * Due to race conditions, the final count will USUALLY be less than T * N.
         * Sometimes the program might throw exceptions or show other corruption.
         *
         * NOTE: Non-deterministic — may occasionally "work" on lightly loaded machines.
         */
        public Map<String, Integer> countWithHashMap(int threads, int incrementsPerThread)
                throws InterruptedException {
            Map<String, Integer> map = new HashMap<>();
            map.put("counter", 0);

            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done  = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                new Thread(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < incrementsPerThread; j++) {
                            // NOT ATOMIC: read → increment → write
                            Integer val = map.get("counter");
                            if (val != null) {
                                map.put("counter", val + 1);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }).start();
            }
            start.countDown(); // release all threads at once — maximizes contention
            done.await();
            return map;
        }
    }

    // =========================================================================
    // Safe counter — ConcurrentHashMap with atomic operations
    // =========================================================================

    public static class SafeCounter {

        private final ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        /**
         * merge() is atomic: reads the current value, applies the function, writes result.
         * Equivalent to a CAS (compare-and-swap) loop internally.
         *
         * Perfect for counters: map.merge(key, 1, Integer::sum)
         */
        public void increment(String key) {
            map.merge(key, 1, Integer::sum);
        }

        /**
         * compute() is also atomic — use when the new value depends on old value
         * with more complex logic than merge().
         */
        public void computeIncrement(String key) {
            map.compute(key, (k, v) -> v == null ? 1 : v + 1);
        }

        /**
         * computeIfAbsent() atomically creates a value only if key doesn't exist.
         * Safe alternative to: if (!map.containsKey(k)) { map.put(k, create()); }
         * (That check-then-act pattern is a TOCTOU race condition!)
         */
        public Integer getOrCreate(String key) {
            return map.computeIfAbsent(key, k -> 0);
        }

        public Integer get(String key) { return map.getOrDefault(key, 0); }
        public Map<String, Integer> getAll() { return Map.copyOf(map); }
    }

    // =========================================================================
    // Parallel counter using ConcurrentHashMap — provably correct
    // =========================================================================

    public static class ParallelSafeCounter {

        /**
         * Increments a counter T*N times across T threads using ConcurrentHashMap.merge().
         * The result is ALWAYS exactly T * incrementsPerThread — guaranteed.
         */
        public int countWithConcurrentHashMap(int threads, int incrementsPerThread)
                throws InterruptedException {
            ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done  = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                new Thread(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < incrementsPerThread; j++) {
                            map.merge("counter", 1, Integer::sum); // ATOMIC!
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }).start();
            }
            start.countDown();
            done.await();
            return map.getOrDefault("counter", 0);
        }
    }

    // =========================================================================
    // ConcurrentHashMap utility patterns
    // =========================================================================

    public static class ConcurrentMapPatterns {

        private final ConcurrentHashMap<String, java.util.List<String>> groupMap =
                new ConcurrentHashMap<>();

        /**
         * Atomic "put if absent" — safe lazy initialization.
         * Using computeIfAbsent prevents the double-init race in check-then-act.
         */
        public void addToGroup(String group, String item) {
            groupMap.computeIfAbsent(group, k -> new CopyOnWriteArrayList<>()).add(item);
        }

        /**
         * putIfAbsent returns the EXISTING value if key already present,
         * or null if it was newly inserted. Useful for "get-or-create" patterns.
         */
        public Integer initializeIfNeeded(String key, Integer defaultValue) {
            groupMap.putIfAbsent(key, new CopyOnWriteArrayList<>());
            return defaultValue;
        }

        /**
         * ConcurrentHashMap allows concurrent reads ALWAYS — no locking on read.
         * Writes use fine-grained bucket-level locking (Java 8+).
         * size() and isEmpty() are approximate under concurrent modification.
         */
        public int size() { return groupMap.size(); }
    }
}
