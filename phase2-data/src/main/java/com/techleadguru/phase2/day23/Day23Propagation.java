package com.techleadguru.phase2.day23;

import com.techleadguru.phase2.shared.AuditEntry;
import com.techleadguru.phase2.shared.AuditRepository;
import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

/**
 * DAY 23 — Transaction Propagation: REQUIRED vs REQUIRES_NEW
 *
 * PROPAGATION CONTROLS what happens to the TX when a @Transactional method calls another.
 *
 * REQUIRED (default):
 *   - If a TX exists: JOIN it. Method participates in the outer transaction.
 *   - If no TX: CREATE a new one.
 *   - Rollback in inner method rolls back the OUTER TX too (same TX = one unit).
 *   - 99% of your @Transactional methods should use REQUIRED.
 *
 * REQUIRES_NEW:
 *   - ALWAYS creates a NEW independent transaction.
 *   - Outer TX is SUSPENDED until inner TX completes.
 *   - Inner TX commits or rolls back INDEPENDENTLY of outer TX.
 *   - Use when: audit log that must survive even if main TX rolls back.
 *               Payment record that must be committed even if email sending fails.
 *
 * THE TRAP (Day 19 revisited with real TX proof):
 *   self.auditLog() called from within the same bean bypasses the proxy.
 *   REQUIRES_NEW is silently ignored — audit log runs in outer TX.
 *   When outer TX rolls back: audit log rolls back too. Compliance violation.
 *   FIX: Extract auditLog() to a separate @Service bean.
 *
 * PRODUCTION SCENARIO — Order audit table empty after payment failures:
 *   OrderService.placeOrder() calls this.writeAuditLog() with REQUIRES_NEW.
 *   Payment fails → outer TX rolls back → audit log gone.
 *   Operations can't see what happened — support team blind.
 *   FIX: AuditService @Service bean. placeOrder() calls auditService.writeAuditLog().
 *        Now REQUIRES_NEW creates a separate TX. Audit survives payment rollback.
 */
@Slf4j
public class Day23Propagation {

    // ===================================================================================
    // Entities/Repositories used: shared.Order, shared.AuditEntry, shared.OrderRepository, shared.AuditRepository
    // ===================================================================================

    // ===================================================================================
    // AuditService — SEPARATE @Service bean so REQUIRES_NEW goes through the proxy
    // ===================================================================================

    @Service
    @Slf4j
    public static class AuditService {

        private final AuditRepository auditRepository;

        public AuditService(AuditRepository auditRepository) {
            this.auditRepository = auditRepository;
        }

        /**
         * REQUIRES_NEW: always starts a fresh TX independent of the caller's TX.
         * Called from OrderService via Spring injection (not self-invocation).
         * Even if OrderService TX rolls back, this audit record is already committed.
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public AuditEntry recordAction(String action, String resourceId) {
            String txName = TransactionSynchronizationManager.getCurrentTransactionName();
            log.info("[Day23] AuditService TX (REQUIRES_NEW): {}", txName);
            return auditRepository.save(new AuditEntry(action, resourceId));
        }

        /**
         * REQUIRED: default. Joins the caller's TX if one exists.
         * If OrderService TX rolls back, this audit rolls back too.
         */
        @Transactional(propagation = Propagation.REQUIRED)
        public AuditEntry recordActionInSameTx(String action, String resourceId) {
            String txName = TransactionSynchronizationManager.getCurrentTransactionName();
            log.info("[Day23] AuditService TX (REQUIRED, joins outer): {}", txName);
            return auditRepository.save(new AuditEntry(action, resourceId));
        }
    }

    // ===================================================================================
    // OrderService — calls AuditService demonstrating both propagation types
    // ===================================================================================

    @Service
    @Slf4j
    public static class OrderService {

        private final OrderRepository orderRepository;
        private final AuditService auditService;

        public OrderService(OrderRepository orderRepository, AuditService auditService) {
            this.orderRepository = orderRepository;
            this.auditService = auditService;
        }

        /**
         * Main TX. AuditService uses REQUIRES_NEW → new TX.
         * If we throw here after audit is saved → order rolls back, audit survives.
         */
        @Transactional
        public String placeOrderWithSeparateAudit(String userId, BigDecimal total, boolean shouldFail) {
            String outerTxName = TransactionSynchronizationManager.getCurrentTransactionName();
            log.info("[Day23] OrderService outer TX: {}", outerTxName);

            Order order = orderRepository.save(new Order(userId, total));
            // Audit in SEPARATE TX — will survive even if outer TX rolls back
            auditService.recordAction("ORDER_CREATED", order.getId());

            if (shouldFail) {
                throw new RuntimeException("Simulated payment failure — outer TX will roll back");
            }
            return order.getId();
        }

        /**
         * Main TX. AuditService uses REQUIRED → same TX.
         * If we throw here → BOTH order and audit roll back.
         */
        @Transactional
        public String placeOrderWithSharedAudit(String userId, BigDecimal total, boolean shouldFail) {
            Order order = orderRepository.save(new Order(userId, total));
            // Audit in SAME TX — will roll back with the order
            auditService.recordActionInSameTx("ORDER_CREATED", order.getId());

            if (shouldFail) {
                throw new RuntimeException("Simulated payment failure — entire TX rolls back");
            }
            return order.getId();
        }
    }
}
