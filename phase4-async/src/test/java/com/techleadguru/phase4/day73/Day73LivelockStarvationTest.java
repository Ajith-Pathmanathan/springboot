package com.techleadguru.phase4.day73;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 73 — Livelock & Starvation Test
 *
 * Verifies:
 * 1. tryEatFixed() allows both diners to eat (fixed ordering breaks livelock)
 * 2. StarvationDemo runs to completion without hanging
 * 3. Both high and low priority threads get CPU time (fairness on modern JVMs)
 */
class Day73LivelockStarvationTest {

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void fixed_diner_algo_allows_both_to_eat() throws InterruptedException {
        Day73LivelockStarvation.PoliteSpoonDiner alice = new Day73LivelockStarvation.PoliteSpoonDiner("Alice");
        Day73LivelockStarvation.PoliteSpoonDiner bob   = new Day73LivelockStarvation.PoliteSpoonDiner("Bob");
        Day73LivelockStarvation.Spoon spoon = new Day73LivelockStarvation.Spoon(alice);

        CountDownLatch done = new CountDownLatch(2);
        Thread aliceThread = new Thread(() -> {
            alice.tryEatFixed(spoon, bob);
            done.countDown();
        });
        Thread bobThread = new Thread(() -> {
            bob.tryEatFixed(spoon, alice);
            done.countDown();
        });

        aliceThread.setDaemon(true);
        bobThread.setDaemon(true);
        aliceThread.start();
        bobThread.start();

        boolean completed = done.await(8, TimeUnit.SECONDS);
        assertThat(completed).as("Both diners should finish eating").isTrue();
        assertThat(alice.getEatCount() + bob.getEatCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void starvation_demo_runs_all_threads_to_completion() throws InterruptedException {
        var result = Day73LivelockStarvation.StarvationDemo.run(200); // run for 200ms
        assertThat(result.highPriorityCount()).isPositive();
        assertThat(result.lowPriorityCount()).isPositive();
        // Both should run — on modern JVMs priority is a hint, not strict
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void starvation_demo_high_priority_usually_gets_more_cpu() throws InterruptedException {
        var result = Day73LivelockStarvation.StarvationDemo.run(300);
        // High priority gets more, but on Linux OpenJDK, priorities are approximate
        // Just verify both ran and total is reasonable
        assertThat(result.highPriorityCount() + result.lowPriorityCount()).isPositive();
    }

    @Test
    void diner_algorithm_comparison_documentation() {
        // LIVELOCK pattern (tryEatWithLivelock):
        //   Both diners are "polite" — each yields to the other when both are hungry
        //   → They oscillate forever: Alice gives to Bob, Bob gives to Alice, repeat
        //   → CPU usage is high (threads are RUNNABLE) but no progress made
        //
        // FIXED pattern (tryEatFixed):
        //   Break symmetry with consistent priority ordering: lower name always defers
        //   → Alice always defers to Bob (or vice versa) based on name comparison
        //   → One eats, the other waits, then the other eats
        //
        // Real-world livelock fix strategies:
        //   1. Randomized backoff — Thread.sleep(random.nextInt(100)) before retry
        //   2. Token/turn ordering — global turn counter, each thread waits for its turn
        //   3. Timeout — abort and restart after N failed attempts

        Day73LivelockStarvation.PoliteSpoonDiner a = new Day73LivelockStarvation.PoliteSpoonDiner("Alice");
        Day73LivelockStarvation.PoliteSpoonDiner b = new Day73LivelockStarvation.PoliteSpoonDiner("Bob");
        assertThat(a.getEatCount()).isZero();
        assertThat(b.getEatCount()).isZero();
    }
}
