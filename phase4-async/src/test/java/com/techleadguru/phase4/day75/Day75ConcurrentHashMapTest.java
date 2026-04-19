package com.techleadguru.phase4.day75;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 75 — ConcurrentHashMap vs HashMap Under Load Test
 *
 * Verifies:
 * 1. ConcurrentHashMap always produces correct count under concurrent writes
 * 2. SafeCounter.merge() is atomic and race-condition-free
 * 3. computeIfAbsent() is safe for lazy initialization
 * 4. HashMap produces incorrect results under concurrent access (may lose updates)
 */
class Day75ConcurrentHashMapTest {

    @Test
    void concurrent_hash_map_always_gives_correct_count() throws InterruptedException {
        Day75ConcurrentHashMap.ParallelSafeCounter counter = new Day75ConcurrentHashMap.ParallelSafeCounter();
        int threads = 10;
        int incrementsPerThread = 200;
        int result = counter.countWithConcurrentHashMap(threads, incrementsPerThread);
        assertThat(result).isEqualTo(threads * incrementsPerThread); // ALWAYS 2000
    }

    @RepeatedTest(3)
    void safe_counter_merge_is_atomic() throws InterruptedException {
        Day75ConcurrentHashMap.ParallelSafeCounter counter =
                new Day75ConcurrentHashMap.ParallelSafeCounter();
        int result = counter.countWithConcurrentHashMap(8, 500);
        assertThat(result).isEqualTo(4000); // 8 × 500 always exact
    }

    @Test
    void safe_counter_increment_increments_atomically() {
        Day75ConcurrentHashMap.SafeCounter counter = new Day75ConcurrentHashMap.SafeCounter();
        for (int i = 0; i < 100; i++) {
            counter.increment("hits");
        }
        assertThat(counter.get("hits")).isEqualTo(100);
    }

    @Test
    void safe_counter_compute_increment_works() {
        Day75ConcurrentHashMap.SafeCounter counter = new Day75ConcurrentHashMap.SafeCounter();
        counter.computeIncrement("clicks");
        counter.computeIncrement("clicks");
        counter.computeIncrement("clicks");
        assertThat(counter.get("clicks")).isEqualTo(3);
    }

    @Test
    void safe_counter_get_or_create_returns_default_zero() {
        Day75ConcurrentHashMap.SafeCounter counter = new Day75ConcurrentHashMap.SafeCounter();
        Integer val = counter.getOrCreate("new-key");
        assertThat(val).isEqualTo(0);
    }

    @Test
    void safe_counter_multiple_keys_are_independent() {
        Day75ConcurrentHashMap.SafeCounter counter = new Day75ConcurrentHashMap.SafeCounter();
        counter.increment("a");
        counter.increment("a");
        counter.increment("b");
        assertThat(counter.get("a")).isEqualTo(2);
        assertThat(counter.get("b")).isEqualTo(1);
        assertThat(counter.get("unknown")).isEqualTo(0);
    }

    @Test
    void hashmap_under_concurrency_may_produce_wrong_result() throws InterruptedException {
        // This demonstrates the race condition — HashMap is NOT thread-safe.
        // Under concurrent access, updates are lost. The result is LESS than threads × increments.
        // Note: This test is intentionally non-deterministic. On some runs/machines,
        // it MAY produce the correct count due to timing. We don't assert exact value.
        // The lesson: never use HashMap for concurrent writes.
        Day75ConcurrentHashMap.RaceConditionCounter racy = new Day75ConcurrentHashMap.RaceConditionCounter();
        var result = racy.countWithHashMap(10, 100);
        // Just verify no exception was thrown and count exists
        assertThat(result).containsKey("counter");
        Integer count = result.get("counter");
        assertThat(count).isNotNull();
        // Note: count may be LESS than 1000 due to lost updates!
        // This is exactly what makes HashMap unsafe for concurrent use.
    }
}
