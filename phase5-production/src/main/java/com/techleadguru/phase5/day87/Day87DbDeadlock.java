package com.techleadguru.phase5.day87;

import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.*;

/**
 * DAY 87 — DB-Level Deadlock: Two Transactions Lock Rows in Opposite Order
 *
 * JVM DEADLOCK (Day 72) vs DB DEADLOCK (today):
 *   JVM: Java threads compete for ReentrantLock / synchronized monitors.
 *   DB:  SQL transactions hold row-level/page locks; deadlock detected by DB engine.
 *
 * DB DEADLOCK SCENARIO:
 *   TX1: UPDATE accounts SET balance=balance-100 WHERE id=1  →  locks row 1
 *        UPDATE accounts SET balance=balance+100 WHERE id=2  →  waits for TX2
 *   TX2: UPDATE accounts SET balance=balance-100 WHERE id=2  →  locks row 2
 *        UPDATE accounts SET balance=balance+100 WHERE id=1  →  waits for TX1
 *   → Cycle detected → DB picks a "deadlock victim" → rolls back that TX
 *
 * SYMPTOMS:
 *   - "Deadlock found when trying to get lock; try restarting transaction" (MySQL)
 *   - "ERROR: deadlock detected" (PostgreSQL)
 *   - Spring: org.springframework.dao.DeadlockLoserDataAccessException
 *
 * DIAGNOSIS:
 *   MySQL:      SHOW ENGINE INNODB STATUS → "LATEST DETECTED DEADLOCK"
 *   PostgreSQL: pg_locks + pg_stat_activity; SHOW DEADLOCK_TIMEOUT
 *
 * FIX STRATEGIES:
 *   1. Consistent lock order: always UPDATE by ascending ID  ← BEST
 *   2. SELECT ... FOR UPDATE to grab both locks before modifying
 *   3. Retry on DeadlockLoserDataAccessException (@Retryable)
 *   4. Application-level optimistic locking (@Version — Day 42)
 *   5. Reduce TX scope: keep transactions short
 */
@Slf4j
public class Day87DbDeadlock {

    // =========================================================================
    // Entity
    // =========================================================================

    @Entity
    @Table(name = "bank_accounts_day87")
    public static class BankAccount {
        @Id
        private Long id;
        private double balance;

        public BankAccount() {}
        public BankAccount(Long id, double balance) { this.id = id; this.balance = balance; }
        public Long getId()           { return id; }
        public double getBalance()    { return balance; }
        public void setBalance(double b) { this.balance = b; }
    }

    // =========================================================================
    // Repository
    // =========================================================================

    public interface BankAccountRepository extends org.springframework.data.jpa.repository.JpaRepository<BankAccount, Long> {
    }

    // =========================================================================
    // BROKEN Service: locks rows in inconsistent order → deadlock risk
    // =========================================================================

    @Service
    @Slf4j
    public static class DeadlockingTransferService {

        private final BankAccountRepository repo;

        public DeadlockingTransferService(BankAccountRepository repo) {
            this.repo = repo;
        }

        /**
         * BUG: locks fromId first, then toId.
         * If two concurrent calls do transfer(1→2) and transfer(2→1), deadlock!
         */
        @Transactional
        public void transfer(Long fromId, Long toId, double amount) {
            log.debug("[Day87] BROKEN transfer {} → {} amount={}", fromId, toId, amount);
            BankAccount from = repo.findById(fromId).orElseThrow();
            BankAccount to   = repo.findById(toId).orElseThrow();
            // Row lock acquired in arbitrary order ← risk
            from.setBalance(from.getBalance() - amount);
            to.setBalance(to.getBalance() + amount);
            repo.save(from);
            repo.save(to);
        }
    }

    // =========================================================================
    // FIXED Service: always lock in ascending ID order
    // =========================================================================

    @Service
    @Slf4j
    public static class SafeTransferService {

        private final BankAccountRepository repo;

        public SafeTransferService(BankAccountRepository repo) {
            this.repo = repo;
        }

        /**
         * FIX: always acquire row lock in ascending ID order.
         * Thread 1: transfer(1,2) → locks 1 then 2
         * Thread 2: transfer(2,1) → also locks 1 first, then 2  → no cycle!
         */
        @Transactional
        public void transfer(Long fromId, Long toId, double amount) {
            long firstId  = Math.min(fromId, toId);
            long secondId = Math.max(fromId, toId);
            boolean reversed = fromId > toId;

            log.debug("[Day87] SAFE transfer {} → {} (lock order: {}, {})", fromId, toId, firstId, secondId);

            BankAccount first  = repo.findById(firstId).orElseThrow();
            BankAccount second = repo.findById(secondId).orElseThrow();

            BankAccount from = reversed ? second : first;
            BankAccount to   = reversed ? first  : second;

            from.setBalance(from.getBalance() - amount);
            to.setBalance(to.getBalance() + amount);
            repo.save(from);
            repo.save(to);
        }
    }

    // =========================================================================
    // Concurrent transfer test harness
    // =========================================================================

    public static class ConcurrentTransferHarness {

        public result runConcurrent(SafeTransferService svc,
                                    BankAccountRepository repo,
                                    int threadCount) throws InterruptedException {
            CountDownLatch ready = new CountDownLatch(threadCount);
            CountDownLatch done  = new CountDownLatch(threadCount);
            java.util.concurrent.atomic.AtomicInteger errors = new java.util.concurrent.atomic.AtomicInteger();

            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                final boolean ab = i % 2 == 0;
                pool.submit(() -> {
                    ready.countDown();
                    try { ready.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    try {
                        if (ab) svc.transfer(1L, 2L, 10);
                        else    svc.transfer(2L, 1L, 10);
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        log.warn("[Day87] Transfer error: {}", e.getMessage());
                    } finally {
                        done.countDown();
                    }
                });
            }
            done.await(10, TimeUnit.SECONDS);
            pool.shutdown();

            double a1 = repo.findById(1L).map(BankAccount::getBalance).orElse(0.0);
            double a2 = repo.findById(2L).map(BankAccount::getBalance).orElse(0.0);
            return new result(errors.get(), a1, a2);
        }

        public record result(int errorCount, double balance1, double balance2) {}
    }
}
