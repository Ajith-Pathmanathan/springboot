package com.techleadguru.phase4.day74;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DAY 74 — ReentrantLock vs synchronized: tryLock, Fairness, Interruptible Lock
 *
 * WHY ReentrantLock over synchronized?
 *
 *  | Feature                        | synchronized | ReentrantLock |
 *  |--------------------------------|--------------|---------------|
 *  | Acquire with timeout           | ✗            | ✓ tryLock()   |
 *  | Interruptible lock acquisition | ✗            | ✓ lockInterruptibly() |
 *  | Fair ordering (FIFO)           | ✗            | ✓ new ReentrantLock(true) |
 *  | Multiple Condition variables   | ✗            | ✓ newCondition() |
 *  | Try-lock (non-blocking attempt)| ✗            | ✓ tryLock()   |
 *  | Explicit unlock required       | ✗ (auto)     | ✓ MUST call unlock() in finally |
 *  | Reentrancy                     | ✓            | ✓             |
 *  | Visibility guarantee           | ✓            | ✓             |
 *
 * GOLDEN RULE: Always unlock in finally block:
 *   lock.lock();
 *   try { ... } finally { lock.unlock(); }
 *
 * WHEN TO USE EACH:
 *   synchronized  → simple critical section, JIT optimizes it well (lock elision)
 *   ReentrantLock → need tryLock / interruptible / fair / multiple conditions
 *
 * FAIRNESS tradeoff:
 *   Unfair (default): higher throughput (threads can "barge in"), possible starvation
 *   Fair: strict FIFO ordering, no starvation, but lower throughput due to scheduling overhead
 */
@Slf4j
public class Day74ReentrantLock {

    // =========================================================================
    // 1. Basic comparison: synchronized vs ReentrantLock
    // =========================================================================

    /** Baseline counter using synchronized — simple, JIT-friendly */
    public static class SynchronizedCounter {
        private int count = 0;

        public synchronized void increment() { count++; }
        public synchronized int get()        { return count; }
    }

    /** Counter using explicit ReentrantLock */
    public static class ReentrantLockCounter {
        private final ReentrantLock lock = new ReentrantLock();
        private int count = 0;

        public void increment() {
            lock.lock();
            try { count++; }
            finally { lock.unlock(); }
        }

        public int get() {
            lock.lock();
            try { return count; }
            finally { lock.unlock(); }
        }
    }

    // =========================================================================
    // 2. tryLock(timeout) — prevents blocking forever → avoids deadlocks
    // =========================================================================

    /**
     * Safe bank transfer using tryLock with timeout.
     * If both locks can't be acquired within 1 second, abort and report failure.
     * This breaks potential deadlock cycles (Day 72).
     */
    public static class TryLockTransferService {
        private final ReentrantLock lockA;
        private final ReentrantLock lockB;
        private final AtomicInteger transferCount = new AtomicInteger(0);
        private final AtomicInteger failCount = new AtomicInteger(0);

        public TryLockTransferService() {
            this.lockA = new ReentrantLock();
            this.lockB = new ReentrantLock();
        }

        /**
         * Transfers amount from A to B with tryLock timeout.
         * Returns true if transfer succeeded, false if timed out.
         */
        public boolean transfer(long amount, long timeoutMs) throws InterruptedException {
            boolean acquiredA = false, acquiredB = false;
            try {
                acquiredA = lockA.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
                if (!acquiredA) {
                    log.warn("Could not acquire lockA within {}ms", timeoutMs);
                    failCount.incrementAndGet();
                    return false;
                }
                acquiredB = lockB.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
                if (!acquiredB) {
                    log.warn("Could not acquire lockB within {}ms", timeoutMs);
                    failCount.incrementAndGet();
                    return false;
                }
                // Both locks held — safe to proceed
                log.info("Transfer of {} succeeded", amount);
                transferCount.incrementAndGet();
                return true;
            } finally {
                if (acquiredB) lockB.unlock();
                if (acquiredA) lockA.unlock();
            }
        }

        public int getTransferCount() { return transferCount.get(); }
        public int getFailCount()     { return failCount.get(); }
    }

    // =========================================================================
    // 3. lockInterruptibly() — allows lock to be cancelled via Thread.interrupt()
    // =========================================================================

    public static class InterruptibleLockService {
        private final ReentrantLock lock = new ReentrantLock();

        /**
         * Acquires lock but can be interrupted while waiting.
         * Unlike lock.lock(), this will throw InterruptedException if
         * the thread is interrupted while blocked on lock acquisition.
         *
         * Use case: Cancellable tasks — if user cancels a long-running operation,
         * the waiting thread gets interrupted and stops cleanly.
         */
        public String doWorkInterruptibly() throws InterruptedException {
            lock.lockInterruptibly(); // throws if interrupted while waiting
            try {
                return "work done on " + Thread.currentThread().getName();
            } finally {
                lock.unlock();
            }
        }

        public ReentrantLock getLock() { return lock; }
    }

    // =========================================================================
    // 4. Condition variables — like wait/notify but more flexible
    // =========================================================================

    /**
     * Bounded buffer using two Condition variables.
     *
     * notFull  → producers wait here when buffer is full
     * notEmpty → consumers wait here when buffer is empty
     *
     * With synchronized + wait/notify, you'd have to notifyAll() which wakes
     * up both producers AND consumers (wasted wake-ups). With Conditions you
     * signal the right set of waiters.
     */
    public static class BoundedBuffer<T> {
        private final Object[] items;
        private int head, tail, count;

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull  = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        public BoundedBuffer(int capacity) {
            items = new Object[capacity];
        }

        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                while (count == items.length) notFull.await(); // wait if full
                items[tail] = item;
                tail = (tail + 1) % items.length;
                count++;
                notEmpty.signal(); // wake one consumer
            } finally {
                lock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public T take() throws InterruptedException {
            lock.lock();
            try {
                while (count == 0) notEmpty.await(); // wait if empty
                T item = (T) items[head];
                items[head] = null;
                head = (head + 1) % items.length;
                count--;
                notFull.signal(); // wake one producer
                return item;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try { return count; }
            finally { lock.unlock(); }
        }
    }

    // =========================================================================
    // 5. Fair vs unfair ReentrantLock
    // =========================================================================

    public static class FairnessDemo {
        private final ReentrantLock unfairLock = new ReentrantLock(false); // default
        private final ReentrantLock fairLock   = new ReentrantLock(true);  // FIFO queue

        public ReentrantLock getUnfairLock() { return unfairLock; }
        public ReentrantLock getFairLock()   { return fairLock; }

        /**
         * Fair lock characteristics:
         * - Threads are served in the order they started waiting (FIFO)
         * - No thread starves indefinitely
         * - Lower throughput: OS must schedule threads in FIFO order
         *
         * Unfair lock characteristics:
         * - A thread that just released the lock can re-acquire it immediately
         *   (avoids expensive OS context switch → higher throughput)
         * - Possible starvation if contention is constant
         */
        public boolean isFair(ReentrantLock lock) { return lock.isFair(); }
    }
}
