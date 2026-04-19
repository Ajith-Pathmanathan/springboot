package com.techleadguru.phase1.day19;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * DAY 19 — THE SELF-INVOCATION TRAP ⚠️ (Most Important Day in Phase 1)
 *
 * THE RULE: When a Spring bean calls its OWN method directly (this.method()),
 * it bypasses the proxy. @Transactional, @Async, @Cacheable, @Retryable are ALL silently ignored.
 *
 * WHY:
 *   Spring wraps your @Service in a proxy (CGLIB or JDK).
 *   Callers go through the proxy → Spring intercepts → applies advice.
 *   BUT: when you call this.auditLog() inside the same class,
 *   "this" is the RAW object, NOT the proxy.
 *   The proxy is never entered. The advice never fires.
 *
 * PRODUCTION SCENARIO — Compliance violation (most severe consequence):
 *   OrderService.placeOrder() calls this.auditLog() internally.
 *   auditLog() has @Transactional(propagation = REQUIRES_NEW).
 *   Intended: audit log survives even if outer TX rolls back.
 *   Reality: audit log runs in the SAME TX as placeOrder().
 *   When payment fails and outer TX rolls back, audit log rolls back too.
 *   Regulators fine the company for missing audit trail on failed orders.
 *
 * THREE FIXES:
 *   FIX 1 (BEST): Extract auditLog() to a separate @Component — always goes through proxy.
 *   FIX 2: Inject self via AopContext.currentProxy() with @EnableAspectJAutoProxy(exposeProxy=true).
 *   FIX 3: @Autowired self-injection (works but looks odd, IDE warns about circular dep).
 */
@Slf4j
public class Day19SelfInvocationTrap {

    // ===================================================================================
    // Shared setup
    // ===================================================================================

    @Component
    public static class TxTracker {
        public String getCurrentTxName() {
            return TransactionSynchronizationManager.getCurrentTransactionName();
        }

        public boolean isTransactionActive() {
            return TransactionSynchronizationManager.isActualTransactionActive();
        }
    }

    // ===================================================================================
    // THE BUG: Both methods end up in the SAME transaction
    // ===================================================================================

    @Service("day19BrokenOrderService")
    @Slf4j
    public static class BrokenOrderService {

        private final TxTracker txTracker;

        public BrokenOrderService(TxTracker txTracker) {
            this.txTracker = txTracker;
        }

        @Transactional
        public String placeOrder(String orderId) {
            log.info("[BUG] placeOrder TX: {}", txTracker.getCurrentTxName());
            String outerTx = txTracker.getCurrentTxName();

            // BUG: Calls this.auditLog() — "this" is the RAW object, not the proxy.
            // @Transactional(REQUIRES_NEW) on auditLog() is completely ignored.
            // auditLog() runs in the SAME transaction as placeOrder().
            String auditTx = this.auditLog(orderId); // <- BROKEN: bypasses proxy

            if (outerTx != null && outerTx.equals(auditTx)) {
                log.error("[BUG CONFIRMED] Both methods run in the SAME TX: {}", outerTx);
                log.error("[BUG CONFIRMED] auditLog() REQUIRES_NEW was silently IGNORED!");
            }
            return outerTx;
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public String auditLog(String orderId) {
            // This should be a SEPARATE TX. But when called via this.auditLog(), it never is.
            String currentTx = txTracker.getCurrentTxName();
            log.info("[BUG] auditLog TX: {} (should be different from placeOrder TX)", currentTx);
            return currentTx;
        }
    }

    // ===================================================================================
    // FIX 1 (BEST): Extract to separate @Component — goes through proxy automatically
    // ===================================================================================

    @Component
    @Slf4j
    public static class AuditService {

        private final TxTracker txTracker;

        public AuditService(TxTracker txTracker) {
            this.txTracker = txTracker;
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public String auditLog(String orderId) {
            String currentTx = txTracker.getCurrentTxName();
            log.info("[FIX-1] AuditService.auditLog() TX: {} (separate from caller TX)", currentTx);
            return currentTx;
        }
    }

    @Service("fixedOrderServiceV1")
    @Slf4j
    public static class FixedOrderServiceV1 {

        private final AuditService auditService; // Separate bean — always proxied
        private final TxTracker txTracker;

        public FixedOrderServiceV1(AuditService auditService, TxTracker txTracker) {
            this.auditService = auditService;
            this.txTracker = txTracker;
        }

        @Transactional
        public void placeOrder(String orderId) {
            String outerTx = txTracker.getCurrentTxName();
            log.info("[FIX-1] placeOrder TX: {}", outerTx);

            // FIX: auditService is a separate Spring bean — goes through proxy
            // REQUIRES_NEW creates a genuine new transaction here
            auditService.auditLog(orderId);
            log.info("[FIX-1] audit logged in separate TX — confirms fix working");
        }
    }

    // ===================================================================================
    // FIX 2: AopContext.currentProxy() — call "self" through the proxy
    // Requires @EnableAspectJAutoProxy(exposeProxy=true) on main config (already set in Phase1Application)
    // ===================================================================================

    @Service("fixedOrderServiceV2")
    @Slf4j
    public static class FixedOrderServiceV2 {

        private final TxTracker txTracker;

        public FixedOrderServiceV2(TxTracker txTracker) {
            this.txTracker = txTracker;
        }

        @Transactional
        public void placeOrder(String orderId) {
            log.info("[FIX-2] placeOrder TX: {}", txTracker.getCurrentTxName());

            // FIX: cast through the Spring-managed proxy, not "this"
            // AopContext.currentProxy() returns the proxy object
            FixedOrderServiceV2 self = (FixedOrderServiceV2) AopContext.currentProxy();
            self.auditLog(orderId); // now goes through proxy -> REQUIRES_NEW is honoured
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void auditLog(String orderId) {
            log.info("[FIX-2] auditLog TX: {} (separate because proxy was used)", txTracker.getCurrentTxName());
        }
    }

    // ===================================================================================
    // Demo runner
    // ===================================================================================

    @Configuration
    static class Day19Config {
        @Bean
        public org.springframework.boot.ApplicationRunner day19Runner(
                BrokenOrderService broken,
                FixedOrderServiceV1 fixedV1) {
            return args -> {
                System.out.println("=== DAY 19: Self-Invocation Trap ===");
                System.out.println();
                System.out.println("THE RULE: Never call @Transactional/@Async/@Cacheable methods via this.xxx()");
                System.out.println("WHY:      'this' is the RAW object, not the Spring proxy.");
                System.out.println("FIX:      Extract to a separate @Component (always best).");
                System.out.println("FIX:      Use AopContext.currentProxy() (needs exposeProxy=true).");
                System.out.println();
                System.out.println("To see the TX names, start with a real DataSource (phase2-data module).");
                System.out.println("In this phase, observe the log messages to understand the concept.");
                System.out.println("====================================");
            };
        }
    }
}
