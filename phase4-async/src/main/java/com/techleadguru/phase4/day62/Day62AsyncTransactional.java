package com.techleadguru.phase4.day62;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;

/**
 * DAY 62 — @Async + @Transactional: Separate Transaction Per Call
 *
 * THE RULE:
 *   @Transactional binds a transaction to the CURRENT THREAD via ThreadLocal.
 *   @Async switches to a DIFFERENT THREAD.
 *   Therefore: the async method ALWAYS runs in a NEW, SEPARATE transaction.
 *
 * COMBINING @TRANSACTIONAL + @ASYNC on the SAME METHOD:
 *   @Async
 *   @Transactional                     ← The transaction is on the ASYNC thread
 *   public void processAsync() {}       ← NOT the caller's transaction
 *
 *   This is fine when you WANT a separate transaction (e.g., independent audit log).
 *   This is a BUG when you expect the async method to participate in the caller's transaction.
 *
 * THE VISIBILITY PROBLEM (without explicit ordering):
 *   Thread A: @Transactional — saves Order(id=1) → has NOT committed yet
 *   Thread B: @Async         — tries to read Order(id=1) → NOT FOUND (uncommitted)
 *   Thread A: commits        → Order visible now, but Thread B already failed
 *
 *   Solution: Don't call @Async from within an open transaction that the async
 *             method needs to read. Use TransactionSynchronizationManager.registerSynchronization()
 *             to trigger the async call AFTER the caller commits.
 *
 * AFTERCOMMIT PATTERN (correct):
 *   @Transactional
 *   public void placeOrder(Order order) {
 *       orderRepo.save(order);
 *       TransactionSynchronizationManager.registerSynchronization(
 *           new TransactionSynchronizationAdapter() {
 *               @Override
 *               public void afterCommit() {
 *                   notificationService.sendEmailAsync(order); // runs after TX commits
 *               }
 *           }
 *       );
 *   }
 *
 * ANNOTATION ORDER:
 *   @Async + @Transactional: @Async creates the proxy that submits to pool.
 *   @Transactional wraps the execution ON the async thread.
 *   The resulting CGLIB proxy chain: AsyncProxy → TxProxy → realBean.doWork()
 */
@Slf4j
public class Day62AsyncTransactional {

    // =========================================================================
    // Service demonstrating the separate-TX behaviour
    // =========================================================================

    @Service
    @Slf4j
    public static class OrderProcessingService {

        /**
         * @Async @Transactional — transaction opens on the ASYNC thread.
         * Any call to this from another @Transactional method gets a NEW transaction.
         * The caller's transaction changes are NOT visible until caller commits.
         */
        @Async
        @Transactional
        public CompletableFuture<String> processOrderAsync(String orderId) {
            boolean inTx = TransactionSynchronizationManager.isActualTransactionActive();
            String txName = TransactionSynchronizationManager.getCurrentTransactionName();
            log.info("[Day62] @Async @Transactional: orderId={}, inTx={}, txName={}, thread={}",
                    orderId, inTx, txName, Thread.currentThread().getName());
            // This runs in its OWN transaction on the async thread
            // If caller hasn't committed yet, saved data won't be visible here!
            return CompletableFuture.completedFuture(
                    "processed-" + orderId + "-inTx=" + inTx + "-thread=" + Thread.currentThread().getName()
            );
        }

        /**
         * Caller method: @Transactional. Saves entity; calls async BEFORE committing.
         * The async method's separate transaction will NOT see these changes.
         */
        @Transactional
        public CompletableFuture<String> placeOrderBadPattern(String orderId) {
            boolean callerInTx = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("[Day62] Caller: inTx={}, thread={}", callerInTx, Thread.currentThread().getName());
            // BAD: calling async before this TX commits → async won't see uncommitted data
            CompletableFuture<String> future = processOrderAsync(orderId);
            // ... caller TX still open here, hasn't committed ...
            return future;
        }

        /**
         * CORRECT PATTERN: Register afterCommit hook.
         * Async work starts only after caller's transaction commits.
         */
        @Transactional
        public void placeOrderCorrectPattern(String orderId, Runnable afterCommitAction) {
            boolean callerInTx = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("[Day62] Correct pattern: saving order={}, inTx={}", orderId, callerInTx);
            // ... save entity ...
            // Register: afterCommitAction runs AFTER this @Transactional method commits
            if (callerInTx) {
                TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                log.info("[Day62] afterCommit: now safe to call async with committed data");
                                afterCommitAction.run();
                            }
                        }
                );
            }
        }

        /**
         * Shows whether @Transactional annotation is effective on this async method.
         * Running this via Spring proxy chain confirms a TX exists.
         */
        @Async
        @Transactional
        public CompletableFuture<Boolean> isTransactionActiveAsync() {
            boolean active = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("[Day62] isTransactionActiveAsync(): {}", active);
            return CompletableFuture.completedFuture(active);
        }
    }
}
