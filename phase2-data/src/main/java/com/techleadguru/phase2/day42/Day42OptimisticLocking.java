package com.techleadguru.phase2.day42;

import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderRepository;
import com.techleadguru.phase2.shared.User;
import com.techleadguru.phase2.shared.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * DAY 42 — Optimistic Locking: @Version prevents lost updates
 *
 * THE PROBLEM — LOST UPDATES:
 *   Two transactions try to update the same row concurrently:
 *     TX1: read row (balance=100) → modify (balance=80) → write back
 *     TX2: read row (balance=100) → modify (balance=60) → write back
 *   If TX1 and TX2 run concurrently, the second write OVERWRITES the first.
 *   Final balance = 60. TX1's update is LOST. Data corruption.
 *
 * PESSIMISTIC vs OPTIMISTIC LOCKING:
 *
 *   PESSIMISTIC:
 *     SELECT ... FOR UPDATE — locks the row, blocks other transactions.
 *     Prevents concurrent reads. High contention. Use when conflict rate is HIGH.
 *     Example: bank account that's debited thousands of times per second.
 *
 *   OPTIMISTIC:
 *     @Version column — each update increments version. If version mismatch → exception.
 *     No locks. Concurrent reads allowed. Conflict rare in practice.
 *     Use when conflict rate is LOW (typical for most apps).
 *     "Optimistic" = assume conflicts are unlikely; check only on update.
 *
 * HOW @Version WORKS:
 *   1. Entity has @Version Long version field (starts at 0 on first persist).
 *   2. On every update: WHERE version = <current_version>
 *      If 0 rows updated → OptimisticLockException (another TX modified it first).
 *   3. On success: version is incremented to version+1.
 *
 *   SQL generated:
 *     UPDATE orders SET total=?, status=?, version=1 WHERE id=? AND version=0
 *     If rows_affected = 0 → StaleObjectStateException / OptimisticLockingFailureException
 *
 * HANDLING OptimisticLockingFailureException:
 *   RETRY: Reload the entity and retry the operation (with exponential backoff).
 *   REJECT: Return 409 Conflict to the client (optimistic UI pattern).
 *   LOG + ALERT: If conflicts are frequent, the app needs pessimistic locking.
 *
 * PRODUCTION PATTERN — RETRY:
 *   @Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3,
 *              backoff = @Backoff(delay = 100, multiplier = 2))
 *   @Transactional
 *   public void updateUserBalance(String userId, BigDecimal delta) { ... }
 *
 * EXISTING @Version FIELDS:
 *   - shared.User.version (@Version Long)
 *   - shared.Order.version (@Version Long)
 *   Both are already in V1 migration schema (version BIGINT DEFAULT 0).
 */
@Slf4j
public class Day42OptimisticLocking {

    // ===================================================================================
    // Service demonstrating @Version optimistic locking on User entity
    // ===================================================================================

    @Service
    @Slf4j
    public static class UserBalanceService {

        private final UserRepository userRepository;

        public UserBalanceService(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        @Transactional
        public User createUser(String email, String name, BigDecimal initialBalance) {
            User user = new User(email, name);
            user.setBalance(initialBalance);
            User saved = userRepository.save(user);
            log.info("[Day42] Created user: id={}, version={}, balance={}", saved.getId(), saved.getVersion(), saved.getBalance());
            return saved;
        }

        /**
         * Demonstrates a successful update — version increments from 0 to 1.
         */
        @Transactional
        public User debitBalance(String userId, BigDecimal amount) {
            User user = userRepository.findById(userId).orElseThrow();
            long versionBefore = user.getVersion();
            user.setBalance(user.getBalance().subtract(amount));
            User saved = userRepository.save(user);
            log.info("[Day42] Debit success: userId={}, versionBefore={}, versionAfter={}, balance={}",
                    userId, versionBefore, saved.getVersion(), saved.getBalance());
            return saved;
        }

        /**
         * Returns an entity (outside TX) — it's now DETACHED with a stale version.
         * Useful for demonstrating version mismatch in the next test.
         */
        @Transactional
        public User loadUser(String userId) {
            return userRepository.findById(userId).orElseThrow();
        }

        /**
         * Simulates a LOST UPDATE scenario without @Version protection.
         * In this demo, the version field PREVENTS the lost update.
         *
         * TX1 loads user (version=0), modifies to balance=80.
         * TX2 loads same user (version=0), commits balance=60 (version becomes 1).
         * TX1 tries to save with version=0 → version mismatch → OptimisticLockException.
         */
        @Transactional
        public void demonstrateConcurrentUpdate(User staleEntity, BigDecimal newBalance) {
            staleEntity.setBalance(newBalance);
            userRepository.save(staleEntity); // Will throw OptimisticLockingFailureException if stale
        }

        /**
         * Production pattern: retry on optimistic lock conflict.
         * (In real code, use @Retryable from spring-retry)
         */
        @Transactional
        public User debitWithRetry(String userId, BigDecimal amount, int maxRetries) {
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    User user = userRepository.findById(userId).orElseThrow();
                    user.setBalance(user.getBalance().subtract(amount));
                    User saved = userRepository.save(user);
                    log.info("[Day42] Debit succeeded on attempt {}", attempt);
                    return saved;
                } catch (OptimisticLockingFailureException e) {
                    if (attempt == maxRetries) {
                        log.error("[Day42] Debit failed after {} attempts", maxRetries);
                        throw e;
                    }
                    log.warn("[Day42] OptimisticLock conflict on attempt {}. Retrying...", attempt);
                }
            }
            throw new IllegalStateException("Should not reach here");
        }
    }

    // ===================================================================================
    // Service demonstrating @Version on Order entity
    // ===================================================================================

    @Service
    @Slf4j
    public static class OrderVersionService {

        private final OrderRepository orderRepository;

        public OrderVersionService(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        @Transactional
        public Order createOrder(String userId, BigDecimal total) {
            Order order = new Order(userId, total);
            Order saved = orderRepository.save(order);
            log.info("[Day42] Created order: id={}, version={}", saved.getId(), saved.getVersion());
            return saved;
        }

        /**
         * Shows version incrementing with each update.
         */
        @Transactional
        public Order updateStatus(String orderId, String newStatus) {
            Order order = orderRepository.findById(orderId).orElseThrow();
            long versionBefore = order.getVersion();
            order.setStatus(newStatus);
            Order saved = orderRepository.save(order);
            log.info("[Day42] Status updated: orderId={}, v{}→v{}, status={}",
                    orderId, versionBefore, saved.getVersion(), newStatus);
            return saved;
        }

        /**
         * Loads an order (detached after TX ends).
         */
        @Transactional
        public Order loadOrder(String orderId) {
            return orderRepository.findById(orderId).orElseThrow();
        }
    }
}
