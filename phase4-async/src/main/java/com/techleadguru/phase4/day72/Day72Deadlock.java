package com.techleadguru.phase4.day72;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DAY 72 — Deadlock: Reproduce, Diagnose, Fix
 *
 * WHAT IS A DEADLOCK?
 *   Thread A holds Lock1, waits for Lock2.
 *   Thread B holds Lock2, waits for Lock1.
 *   Neither can proceed → program hangs forever.
 *
 * CLASSIC DEADLOCK RECIPE:
 *   1. Two or more threads
 *   2. Two or more resources (locks, DB rows, files)
 *   3. Each thread holds one resource and waits for the other
 *
 * DETECTION:
 *   ThreadMXBean.findDeadlockedThreads() → returns IDs of deadlocked threads
 *   jstack <pid> → "Found one Java-level deadlock:" section
 *   Spring Actuator: GET /actuator/threaddump → look for "BLOCKED" state
 *
 * THE FIX — CONSISTENT LOCK ORDERING:
 *   Always acquire locks in the same global order across all threads.
 *   If Thread A and Thread B both always lock accountA before accountB,
 *   no circular wait can form.
 *
 * OTHER FIXES:
 *   1. tryLock(timeout) — abandon if can't acquire in time (Day 74)
 *   2. Lock timeout + retry with backoff
 *   3. Reduce lock scope to one lock at a time
 *   4. Use higher-level abstractions (ConcurrentHashMap, atomic operations)
 *
 * REAL-WORLD DEADLOCK SCENARIOS:
 *   - Bank transfer: transferFrom(A, B) locks A then B; transferFrom(B, A) locks B then A
 *   - Singleton cache that calls synchronized methods on each other
 *   - JPA: two threads loading related entities that cascade-load each other
 *   - Connection pools: two transactions each holding a connection and requesting second
 */
@Slf4j
public class Day72Deadlock {

    // =========================================================================
    // Bank account model — shared mutable resource
    // =========================================================================

    public static class BankAccount {
        private final int id;
        private long balance;

        public BankAccount(int id, long initialBalance) {
            this.id = id;
            this.balance = initialBalance;
        }

        public int getId()        { return id; }
        public long getBalance()  { return balance; }
        public void debit(long amount)  { balance -= amount; }
        public void credit(long amount) { balance += amount; }
    }

    // =========================================================================
    // BROKEN: Transfer that causes deadlock (locks in arbitrary order)
    // =========================================================================

    public static class DeadlockingTransferService {

        /**
         * DEADLOCK WAITING TO HAPPEN!
         *
         * Thread 1: transfer(A → B) locks accountA first, then tries accountB
         * Thread 2: transfer(B → A) locks accountB first, then tries accountA
         * Circle: Thread1 holds A, wants B; Thread2 holds B, wants A.
         *
         * NOTE: This method is intentionally broken for demonstration.
         * Do NOT use in production code.
         */
        public void transfer(BankAccount from, BankAccount to, long amount)
                throws InterruptedException {
            synchronized (from) {
                Thread.sleep(10); // simulates work; gives the other thread time to grab its lock
                synchronized (to) {
                    from.debit(amount);
                    to.credit(amount);
                    log.info("BROKEN transfer {} → {} amount={} balance_from={} balance_to={}",
                            from.getId(), to.getId(), amount, from.getBalance(), to.getBalance());
                }
            }
        }
    }

    // =========================================================================
    // FIXED: Transfer with consistent lock ordering
    // =========================================================================

    public static class SafeTransferService {

        /**
         * FIX: Always lock accounts in ascending ID order.
         *
         * Regardless of which direction the transfer goes, both threads will
         * always lock the lower-ID account first, breaking the circular wait.
         */
        public void transfer(BankAccount from, BankAccount to, long amount) {
            BankAccount first  = from.getId() < to.getId() ? from : to;
            BankAccount second = from.getId() < to.getId() ? to   : from;

            synchronized (first) {
                synchronized (second) {
                    from.debit(amount);
                    to.credit(amount);
                    log.info("SAFE transfer {} → {} amount={} balance_from={} balance_to={}",
                            from.getId(), to.getId(), amount, from.getBalance(), to.getBalance());
                }
            }
        }
    }

    // =========================================================================
    // Deadlock creator (creates deadlock in background threads for detection demo)
    // =========================================================================

    public static class DeadlockCreator {

        private final Lock lockA = new ReentrantLock();
        private final Lock lockB = new ReentrantLock();

        /**
         * Starts two threads that will deadlock each other.
         * Returns the latch that fires once both threads have acquired their first lock
         * (i.e., the deadlock has occurred — safe to detect at this point).
         */
        public CountDownLatch createDeadlock() {
            CountDownLatch bothLockedFirstLock = new CountDownLatch(2);

            // Thread A: locks lockA, then waits for both to reach this point, then tries lockB
            Thread threadA = new Thread(() -> {
                lockA.lock();
                bothLockedFirstLock.countDown();
                try { bothLockedFirstLock.await(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lockA.unlock();
                    return;
                }
                lockB.lock(); // BLOCKS — threadB holds lockB and is waiting for lockA → DEADLOCK
                lockB.unlock();
                lockA.unlock();
            }, "deadlock-thread-A");

            // Thread B: locks lockB, then waits for both to reach this point, then tries lockA
            Thread threadB = new Thread(() -> {
                lockB.lock();
                bothLockedFirstLock.countDown();
                try { bothLockedFirstLock.await(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    lockB.unlock();
                    return;
                }
                lockA.lock(); // BLOCKS — threadA holds lockA and is waiting for lockB → DEADLOCK
                lockA.unlock();
                lockB.unlock();
            }, "deadlock-thread-B");

            threadA.setDaemon(true);
            threadB.setDaemon(true);
            threadA.start();
            threadB.start();

            return bothLockedFirstLock;
        }
    }

    // =========================================================================
    // Deadlock detector
    // =========================================================================

    public static class DeadlockDetector {

        private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        /**
         * Returns true if any deadlocked threads exist in the JVM right now.
         * Uses JDK's built-in deadlock detection (monitors + ownable synchronizers).
         */
        public boolean hasDeadlock() {
            long[] ids = threadMXBean.findDeadlockedThreads();
            return ids != null && ids.length > 0;
        }

        /**
         * Returns a description of the deadlock, or "" if none.
         */
        public String describeDeadlock() {
            long[] ids = threadMXBean.findDeadlockedThreads();
            if (ids == null || ids.length == 0) return "";

            var sb = new StringBuilder("DEADLOCK detected between:\n");
            for (var info : threadMXBean.getThreadInfo(ids, true, true)) {
                sb.append(String.format("  Thread \"%s\" [%s] waiting for: %s (held by \"%s\")%n",
                        info.getThreadName(),
                        info.getThreadState(),
                        info.getLockName(),
                        info.getLockOwnerName()));
            }
            return sb.toString();
        }
    }
}
