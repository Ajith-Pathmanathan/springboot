package com.techleadguru.phase2.day25;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * DAY 25 — MANDATORY, NEVER, NOT_SUPPORTED, SUPPORTS
 *
 * These four propagation modes are rarer than REQUIRED/REQUIRES_NEW/NESTED,
 * but knowing when to use them prevents hard-to-debug production bugs.
 *
 * MANDATORY:
 *   - There MUST be an active transaction. If none: throw IllegalTransactionStateException.
 *   - Use for: internal utility methods that should NEVER be the outermost TX boundary.
 *   - Example: a method that writes to an outbox table — must be part of the calling TX.
 *   - If caller forgets @Transactional, MANDATORY fails immediately at dev time, not silently.
 *
 * NEVER:
 *   - There must NOT be an active transaction. If one exists: throw IllegalTransactionStateException.
 *   - Use for: read-only operations where a TX would cause locking overhead.
 *   - Example: reporting queries on a replica where TX semantics are unwanted.
 *
 * NOT_SUPPORTED:
 *   - Suspend any active TX, execute WITHOUT a transaction, resume the TX afterward.
 *   - Use for: operations that must not participate in the TX (e.g., external API calls
 *     that should not hold DB connections open while waiting for remote responses).
 *   - Example: call payment gateway inside a TX → connection held open for 5s → pool exhaustion.
 *   - FIX: Move external call to a @Transactional(NOT_SUPPORTED) method.
 *
 * SUPPORTS:
 *   - Joins an existing TX if one exists. Runs without TX if none exists.
 *   - Use for: read-only helpers that work correctly in both TX and non-TX contexts.
 *   - Less common — REQUIRED usually covers this. Use SUPPORTS when you explicitly WANT
 *     a method to work both in and out of transactions.
 *
 * PRODUCTION SCENARIO — HikariCP pool exhaustion caused by external call inside TX:
 *   CheckoutService.checkout() is @Transactional (REQUIRED).
 *   Inside checkout(): calls PayPal API (avg 3s response).
 *   The DB connection is HELD for those 3 seconds waiting for PayPal.
 *   Under load: 100 concurrent checkouts → 100 connections held → pool exhausted.
 *   FIX: PaypalService.charge() → @Transactional(NOT_SUPPORTED).
 *        Connection released before PayPal call. Picked up again after.
 */
@Slf4j
public class Day25PropagationRare {

    // ===================================================================================
    // MANDATORY: write to outbox table — MUST be called inside a TX
    // ===================================================================================

    @Service
    @Slf4j
    public static class OutboxService {

        /**
         * MANDATORY: caller MUST have opened a transaction.
         * If called without a TX, Spring throws IllegalTransactionStateException.
         * This is a deliberate design guard: outbox writes must be atomic with the main operation.
         */
        @Transactional(propagation = Propagation.MANDATORY)
        public String writeOutboxEvent(String eventType, String payload) {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("[Day25] OutboxService MANDATORY: txActive={}", txActive);
            // In a real impl: save to outbox table in same TX
            return "OUTBOX[" + eventType + ":" + payload + "]";
        }
    }

    // ===================================================================================
    // NEVER: reporting query — must not run inside a TX (read-only replica usage)
    // ===================================================================================

    @Service
    @Slf4j
    public static class ReportingService {

        /**
         * NEVER: if a TX is active, throws IllegalTransactionStateException.
         * Use when running queries on read replicas where TX context is wrong.
         */
        @Transactional(propagation = Propagation.NEVER)
        public String generateReport(String reportId) {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("[Day25] ReportingService NEVER: txActive={} (must be false)", txActive);
            return "REPORT[" + reportId + "]";
        }
    }

    // ===================================================================================
    // NOT_SUPPORTED: external API call — must not hold DB connection open
    // ===================================================================================

    @Service
    @Slf4j
    public static class PaymentGatewayService {

        /**
         * NOT_SUPPORTED: suspends any active TX before calling external service.
         * DB connection returned to pool during external API call.
         * TX resumed after the external call completes.
         */
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        public String chargeCard(String amount) {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("[Day25] PaymentGatewayService NOT_SUPPORTED: txActive={} (should be false)", txActive);
            // Simulate external API call (no DB connection held during this)
            return "CHARGED:" + amount;
        }
    }

    // ===================================================================================
    // SUPPORTS: read-only helper — works in TX or non-TX context
    // ===================================================================================

    @Service
    @Slf4j
    public static class ProductLookupService {

        /**
         * SUPPORTS: joins TX if one exists, runs without TX if none.
         * Good for: simple lookups called from both TX and non-TX contexts.
         */
        @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
        public String getProductName(String productId) {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.debug("[Day25] ProductLookupService SUPPORTS: txActive={}", txActive);
            return "Product[" + productId + "]";
        }
    }

    // ===================================================================================
    // Caller service to demonstrate the combinations
    // ===================================================================================

    @Service
    @Slf4j
    public static class CheckoutService {

        private final OutboxService outboxService;
        private final PaymentGatewayService paymentGatewayService;
        private final ProductLookupService productLookupService;

        public CheckoutService(OutboxService outboxService,
                               PaymentGatewayService paymentGatewayService,
                               ProductLookupService productLookupService) {
            this.outboxService = outboxService;
            this.paymentGatewayService = paymentGatewayService;
            this.productLookupService = productLookupService;
        }

        @Transactional
        public CheckoutResult checkout(String userId, String productId, String amount) {
            // 1. SUPPORTS: product lookup joins this TX
            String product = productLookupService.getProductName(productId);

            // 2. NOT_SUPPORTED: payment gateway suspends TX — connection returned to pool
            String payment = paymentGatewayService.chargeCard(amount);

            // 3. MANDATORY: outbox write must be in THIS TX (OK — we have one)
            String outbox = outboxService.writeOutboxEvent("ORDER_PLACED", userId + ":" + productId);

            return new CheckoutResult(product, payment, outbox);
        }
    }

    public record CheckoutResult(String product, String payment, String outboxEntry) {}
}
