package com.techleadguru.phase2.day26;

import com.techleadguru.phase2.shared.User;
import com.techleadguru.phase2.shared.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * DAY 26 — Isolation Levels: Dirty Reads, Phantom Reads, Lost Updates
 *
 * DATABASE ISOLATION PROBLEMS:
 *
 *   Dirty Read:
 *     TX1 reads UNCOMMITTED data written by TX2.
 *     TX2 then rolls back — TX1's data is now invalid (it never existed).
 *     Example: TX2 sets balance=0 (not committed). TX1 reads 0 and blocks transfer.
 *              TX2 rolls back — balance is still 1000. TX1 made a wrong decision.
 *
 *   Non-repeatable Read:
 *     TX1 reads row. TX2 UPDATES that row. TX1 reads again — different value.
 *     Example: TX1 starts checkout, reads price=100. TX2 updates price to 150 and commits.
 *              TX1 reads price again (for confirmation page) — gets 150. Inconsistent.
 *
 *   Phantom Read:
 *     TX1 runs a range query. TX2 INSERTS a new row matching that range and commits.
 *     TX1 runs the same query — sees the new "phantom" row.
 *     Example: TX1 counts active users. TX2 adds a user. TX1 counts again — different count.
 *
 *   Lost Update:
 *     TX1 reads balance=100. TX2 reads balance=100.
 *     TX1 writes balance=90 (spent 10). TX2 writes balance=80 (spent 20).
 *     Final balance: 80 — but 30 was spent. 10 units lost.
 *
 * ISOLATION LEVELS (from weakest to strongest):
 *   READ_UNCOMMITTED: sees dirty reads. Almost never used.
 *   READ_COMMITTED:   prevents dirty reads. Default in PostgreSQL, Oracle.
 *   REPEATABLE_READ:  prevents dirty + non-repeatable reads. Default in MySQL InnoDB.
 *   SERIALIZABLE:     prevents all anomalies. Slowest — full serial execution.
 *
 * SPRING: @Transactional(isolation = Isolation.SERIALIZABLE)
 * Note: H2 defaults to SERIALIZABLE. PostgreSQL defaults to READ_COMMITTED.
 *
 * PRODUCTION SCENARIO — Bank transfer race condition:
 *   AccountService reads account balance. Uses READ_COMMITTED (standard).
 *   Two concurrent transfers from same account. Both read balance=1000.
 *   Both write balance=900 (each deducted 100). Final: 900. Should be 800.
 *   100 units created out of thin air — lost update.
 *   FIX: SERIALIZABLE or optimistic locking (@Version — Day 42).
 */
@Slf4j
public class Day26IsolationLevels {

    // ===================================================================================
    // Uses shared.User (maps to 'users' table) as the account entity
    // ===================================================================================

    @Service
    @Slf4j
    public static class BankService {

        private final UserRepository userRepository;

        public BankService(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        /**
         * READ_COMMITTED: default for PostgreSQL.
         * Prevents dirty reads but NOT non-repeatable reads.
         * Two concurrent transfers can cause a lost update without @Version.
         */
        @Transactional(isolation = Isolation.READ_COMMITTED)
        public void transferReadCommitted(String fromId, String toId, BigDecimal amount) {
            User from = userRepository.findById(fromId).orElseThrow();
            User to = userRepository.findById(toId).orElseThrow();

            if (from.getBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient funds");
            }

            from.setBalance(from.getBalance().subtract(amount));
            to.setBalance(to.getBalance().add(amount));

            userRepository.save(from);
            userRepository.save(to);
            log.info("[Day26] READ_COMMITTED transfer: {} → {} amount={}", fromId, toId, amount);
        }

        /**
         * SERIALIZABLE: strongest isolation.
         * Prevents all anomalies. Slowest. Suitable for financial operations.
         */
        @Transactional(isolation = Isolation.SERIALIZABLE)
        public void transferSerializable(String fromId, String toId, BigDecimal amount) {
            User from = userRepository.findById(fromId).orElseThrow();
            User to = userRepository.findById(toId).orElseThrow();

            if (from.getBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient funds");
            }

            from.setBalance(from.getBalance().subtract(amount));
            to.setBalance(to.getBalance().add(amount));

            userRepository.save(from);
            userRepository.save(to);
            log.info("[Day26] SERIALIZABLE transfer: {} → {} amount={}", fromId, toId, amount);
        }

        @Transactional
        public User createAccount(String email, String name, BigDecimal initialBalance) {
            return userRepository.save(new User(email, name, initialBalance));
        }

        @Transactional(readOnly = true)
        public BigDecimal getBalance(String accountId) {
            return userRepository.findById(accountId)
                    .map(User::getBalance)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        }
    }
}
