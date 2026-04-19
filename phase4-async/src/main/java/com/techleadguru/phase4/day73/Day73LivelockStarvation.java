package com.techleadguru.phase4.day73;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 73 — Livelock & Starvation
 *
 * LIVELOCK:
 *   Threads are NOT blocked — they keep running and responding to each other,
 *   but NO PROGRESS is made. Like two people in a hallway stepping the same way
 *   to avoid each other, over and over.
 *
 *   Classic example: Thread A and Thread B are both polite. When they conflict,
 *   each backs off and retries. If they always back off at the same time, they
 *   always conflict again → infinite cycle with no useful work done.
 *
 *   DETECTION: CPU usage is HIGH (threads are actively running), but application
 *   is making no progress. Thread dump shows threads RUNNABLE but counter not increasing.
 *
 *   FIX: Introduce randomized backoff — randomize wait time so threads de-sync.
 *   Or assign priority ordering: if both conflict, always let Thread A win.
 *
 * STARVATION:
 *   A thread is perpetually denied access to a shared resource because other
 *   higher-priority threads always get it first.
 *
 *   Java thread priorities (1-10):
 *     Thread.MIN_PRIORITY = 1
 *     Thread.NORM_PRIORITY = 5 (default)
 *     Thread.MAX_PRIORITY = 10
 *   NOTE: Thread priorities are HINTS to the OS scheduler, not guaranteed!
 *   On modern JVMs, priority starvation is rare but still possible.
 *
 *   More common starvation causes:
 *   - Unfair synchronized blocks: Thread A holding lock for long time; Thread B waiting forever
 *   - Unfair ReentrantLock (default is unfair — new threads can jump ahead of waiting threads)
 *   - Unfair lock: new ReentrantLock()        ← can starve
 *   - Fair lock:   new ReentrantLock(true)    ← guarantees FIFO order (Day 74)
 */
@Slf4j
public class Day73LivelockStarvation {

    // =========================================================================
    // LIVELOCK DEMO: Two polite diners both trying to give the spoon to the other
    // =========================================================================

    public static class PoliteSpoonDiner {

        private final String name;
        private boolean hungry;
        private final AtomicInteger attemptCount = new AtomicInteger(0);
        private volatile int eatCount = 0;

        public PoliteSpoonDiner(String name) {
            this.name = name;
            this.hungry = true;
        }

        /**
         * LIVELOCK pattern: Each diner is too polite.
         * When they both grab the spoon at the same time, each notices the other is
         * hungry and gives it up. They oscillate forever without eating.
         *
         * @param sharedSpoon the one spoon they share
         * @param other       the other diner at the table
         */
        public void tryEatWithLivelock(Spoon sharedSpoon, PoliteSpoonDiner other) {
            while (hungry) {
                if (sharedSpoon.owner() != this) {
                    // Don't have the spoon — wait
                    Thread.yield();
                    continue;
                }
                // "I have the spoon, but is the other person hungry too?"
                if (other.isHungry()) {
                    log.info("{}: You look hungry, you can have the spoon! (attempt #{})",
                            name, attemptCount.incrementAndGet());
                    sharedSpoon.setOwner(other); // give it up → LIVELOCK!
                    continue;
                }
                eat();
                sharedSpoon.setOwner(other);
            }
        }

        /**
         * FIXED: Break the livelock by prioritizing by name (consistent ordering).
         * The "lower priority" diner always defers; the "higher priority" one eats first.
         */
        public void tryEatFixed(Spoon sharedSpoon, PoliteSpoonDiner other) {
            while (hungry) {
                if (sharedSpoon.owner() != this) {
                    Thread.yield();
                    continue;
                }
                // FIXED: only defer if I'm the "lower priority" and the other is hungry
                // Otherwise, just eat. Simple priority ordering breaks the cycle.
                if (other.isHungry() && this.name.compareTo(other.name) > 0) {
                    sharedSpoon.setOwner(other);
                    continue;
                }
                eat();
                sharedSpoon.setOwner(other);
            }
        }

        private void eat() {
            hungry = false;
            eatCount++;
            log.info("{}: Finally eating! Total eats: {}", name, eatCount);
        }

        public boolean isHungry()    { return hungry; }
        public int getEatCount()     { return eatCount; }
        public int getAttemptCount() { return attemptCount.get(); }
        public void setHungry()      { hungry = true; }
    }

    public static class Spoon {
        private volatile PoliteSpoonDiner owner;

        public Spoon(PoliteSpoonDiner owner) { this.owner = owner; }
        public PoliteSpoonDiner owner()      { return owner; }
        public void setOwner(PoliteSpoonDiner d) { this.owner = d; }
    }

    // =========================================================================
    // STARVATION DEMO: Low-priority thread counting against high-priority threads
    // =========================================================================

    public static class StarvationDemo {

        /**
         * Creates threads at different priorities and measures counting progress.
         *
         * NOTE: On Linux with OpenJDK, thread priority has minimal effect
         * (the OS scheduler tries to be fair). This demo is primarily educational.
         * On Windows or some embedded JVMs, starvation is more pronounced.
         *
         * More realistic starvation: unfair lock + many competing threads.
         * The low-priority thread simply never gets scheduled.
         */
        public static StarvationResult run(int durationMs) throws InterruptedException {
            final AtomicInteger highPriorityCount = new AtomicInteger(0);
            final AtomicInteger lowPriorityCount  = new AtomicInteger(0);
            final long deadline = System.currentTimeMillis() + durationMs;

            Runnable highTask = () -> {
                while (System.currentTimeMillis() < deadline) {
                    highPriorityCount.incrementAndGet();
                    Thread.yield();
                }
            };
            Runnable lowTask = () -> {
                while (System.currentTimeMillis() < deadline) {
                    lowPriorityCount.incrementAndGet();
                    Thread.yield();
                }
            };

            Thread high1 = new Thread(highTask, "HIGH-1");
            Thread high2 = new Thread(highTask, "HIGH-2");
            Thread low   = new Thread(lowTask,  "LOW");

            high1.setPriority(Thread.MAX_PRIORITY);
            high2.setPriority(Thread.MAX_PRIORITY);
            low.setPriority(Thread.MIN_PRIORITY);

            high1.start(); high2.start(); low.start();
            high1.join(); high2.join(); low.join();

            log.info("HIGH threads combined: {} | LOW thread: {}",
                    highPriorityCount.get(), lowPriorityCount.get());

            return new StarvationResult(highPriorityCount.get(), lowPriorityCount.get());
        }

        public record StarvationResult(int highPriorityCount, int lowPriorityCount) {
            public double highToLowRatio() {
                return lowPriorityCount == 0 ? Double.MAX_VALUE
                        : (double) highPriorityCount / lowPriorityCount;
            }
        }
    }
}
