package com.techleadguru.phase2.day27;

import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * DAY 27 — Rollback Rules: Checked Exceptions Don't Rollback by Default
 *
 * THE DEFAULT RULE:
 *   @Transactional rolls back ONLY for:
 *     - RuntimeException and its subclasses (unchecked)
 *     - Error and its subclasses
 *
 *   @Transactional does NOT roll back for:
 *     - Checked exceptions (Exception subclasses that don't extend RuntimeException)
 *     - Unless you explicitly configure rollbackFor.
 *
 * THE TRAP (most commonly missed rule in @Transactional):
 *   Service calls external API. External API throws IOException (checked).
 *   @Transactional method catches and re-throws IOException.
 *   Spring sees IOException → NOT a RuntimeException → COMMITS the partial TX.
 *   Data is half-written. DB is in inconsistent state. No exception to alert monitoring.
 *
 * PRODUCTION SCENARIO — Half-processed payment records:
 *   PaymentService.processPayment() is @Transactional.
 *   Steps: (1) save LocalPayment record, (2) call PayPal API, (3) update record with PayPal ID.
 *   PayPal call throws IOException (network timeout) — checked exception.
 *   Spring commits step (1) — LocalPayment row saved.
 *   Steps (2) and (3) never happen. User charged but no PayPal reference.
 *   Finance reconciliation fails. Support tickets flood in.
 *   FIX: @Transactional(rollbackFor = IOException.class)
 *        — or wrap in RuntimeException before re-throwing.
 *
 * CUSTOM RULES:
 *   rollbackFor    = explicit exception types that SHOULD trigger rollback
 *   noRollbackFor  = exception types that should NOT trigger rollback (used for business exceptions
 *                    that are "expected failures" — still want to commit partial state)
 */
@Slf4j
public class Day27RollbackRules {

    // Uses shared.Order / shared.OrderRepository instead of inline PaymentRecord entity

    // Custom checked exception
    public static class PaymentGatewayException extends Exception {
        public PaymentGatewayException(String msg) { super(msg); }
    }

    // ===================================================================================
    // THE BUG: checked exception does NOT trigger rollback
    // ===================================================================================

    @Service
    @Slf4j
    public static class BrokenPaymentService {

        private final OrderRepository orderRepository;

        public BrokenPaymentService(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        /**
         * BUG: PaymentGatewayException is checked → no rollback by default.
         * Row saved to DB. Exception thrown. DB committed. Inconsistent state.
         */
        @Transactional // no rollbackFor!
        public void processPayment(String userId, BigDecimal amount, boolean simulateGatewayFailure)
                throws PaymentGatewayException {
            Order record = orderRepository.save(new Order(userId, amount));
            record.setStatus("PENDING");
            orderRepository.save(record);
            log.info("[Day27] BROKEN: Saved order id={}", record.getId());

            if (simulateGatewayFailure) {
                // Checked exception — Spring @Transactional will NOT rollback!
                throw new PaymentGatewayException("Network timeout connecting to payment gateway");
            }

            record.setStatus("COMPLETED");
            orderRepository.save(record);
        }
    }

    // ===================================================================================
    // THE FIX: explicit rollbackFor
    // ===================================================================================

    @Service
    @Slf4j
    public static class FixedPaymentService {

        private final OrderRepository orderRepository;

        public FixedPaymentService(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        /**
         * FIX: rollbackFor = Exception.class means ALL exceptions trigger rollback.
         * Including checked exceptions like PaymentGatewayException.
         */
        @Transactional(rollbackFor = Exception.class)
        public void processPayment(String userId, BigDecimal amount, boolean simulateGatewayFailure)
                throws PaymentGatewayException {
            Order record = orderRepository.save(new Order(userId, amount));
            record.setStatus("PENDING");
            orderRepository.save(record);
            log.info("[Day27] FIXED: Saved order id={}", record.getId());

            if (simulateGatewayFailure) {
                throw new PaymentGatewayException("Network timeout — will rollback"); // NOW rolls back
            }

            record.setStatus("COMPLETED");
            orderRepository.save(record);
        }
    }

    // ===================================================================================
    // noRollbackFor: expected business exceptions that should NOT trigger rollback
    // ===================================================================================

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String msg) { super(msg); }
    }

    @Service
    @Slf4j
    public static class InventoryService {

        private final OrderRepository orderRepository;

        public InventoryService(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        /**
         * noRollbackFor: InsufficientStockException is a KNOWN business case.
         * We want to commit the audit record even when stock is insufficient.
         * So we mark it as no-rollback.
         */
        @Transactional(noRollbackFor = InsufficientStockException.class)
        public void reserveStock(String userId, int quantity) {
            // Save audit of the attempt (commit even on failure)
            Order auditRecord = orderRepository.save(new Order(userId, BigDecimal.ONE));
            auditRecord.setStatus("STOCK_CHECK_ATTEMPTED");
            orderRepository.save(auditRecord);

            if (quantity > 100) {
                throw new InsufficientStockException("Only 100 units available");
                // Row above committed despite the exception — because noRollbackFor
            }
        }
    }
}
