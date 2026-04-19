package com.techleadguru.phase2.day22;

import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

/**
 * DAY 22 — How @Transactional Really Works: AOP + TransactionSynchronizationManager
 *
 * WHAT SPRING DOES WHEN YOU CALL A @Transactional METHOD:
 *
 *   1. You call orderService.createOrder() on the PROXY, not the real bean.
 *   2. The CGLIB proxy intercepts the call.
 *   3. TransactionInterceptor (an @Around advice) fires:
 *        a. Looks up TransactionAttributeSource to get propagation, isolation, rollback rules.
 *        b. Calls PlatformTransactionManager.getTransaction() → opens JDBC connection.
 *        c. Binds the connection to the current thread via TransactionSynchronizationManager.
 *        d. Proceeds with the real method call.
 *        e. On success: PlatformTransactionManager.commit() → releases connection.
 *        f. On RuntimeException: PlatformTransactionManager.rollback() → releases connection.
 *
 * TransactionSynchronizationManager:
 *   - ThreadLocal map that holds the current TX resources (DataSource → Connection).
 *   - isActualTransactionActive() = is a TX open right now?
 *   - getCurrentTransactionName() = the fully-qualified method name of the TX boundary.
 *   - registerSynchronization() = hook into TX lifecycle (commit/rollback callbacks).
 *
 * WHY THIS MATTERS:
 *   - @Transactional on a private method does NOTHING — proxy cannot intercept it.
 *   - @Transactional on a non-Spring-bean does NOTHING — no proxy.
 *   - Self-invocation (this.method()) bypasses the proxy — @Transactional ignored.
 *
 * PRODUCTION SCENARIO — "Transaction not started" mystery:
 *   Team reports intermittent data inconsistency. Two tables should always update together.
 *   Review: service method has @Transactional. Looks correct.
 *   Root cause: service is newed up with `new OrderService()` in a utility class.
 *   It's NOT a Spring bean — no proxy, @Transactional is purely decorative.
 *   FIX: Always inject via Spring. Never `new` a @Service class manually.
 */
@Slf4j
public class Day22TransactionalInternals {

    // ===================================================================================
    // Entities are in com.techleadguru.phase2.shared (Order, OrderItem, User, AuditEntry)
    // Repositories are in com.techleadguru.phase2.shared (OrderRepository, etc.)
    // ===================================================================================

    // ===================================================================================
    // Service — @Transactional + TransactionSynchronizationManager inspection
    // ===================================================================================

    @Service
    @Slf4j
    public static class OrderService {

        private final OrderRepository orderRepository;

        public OrderService(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        /**
         * Creates an order. @Transactional opens a TX, AOP proxy wraps this method.
         * tx info becomes available via TransactionSynchronizationManager.
         */
        @Transactional
        public TxInfo createOrder(String userId, BigDecimal total) {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            String txName   = TransactionSynchronizationManager.getCurrentTransactionName();

            log.info("[Day22] TX active: {} | TX name: {}", txActive, txName);

            Order order = orderRepository.save(new Order(userId, total));
            return new TxInfo(order.getId(), txActive, txName);
        }

        /**
         * No @Transactional — no TX opened. DataSource connection only when JPA needs it.
         */
        public boolean isTransactionActiveRightNow() {
            return TransactionSynchronizationManager.isActualTransactionActive();
        }

        /**
         * @Transactional on private method — DOES NOTHING (proxy cannot intercept).
         * This method is called directly without going through the proxy.
         */
        @Transactional
        private void privateTransactionalMethod() {
            // This @Transactional is silently ignored — private method bypasses CGLIB proxy!
            log.warn("[Day22] @Transactional on private method is silently ignored!");
        }
    }

    // ===================================================================================
    // DTO to carry TX diagnostic info
    // ===================================================================================

    public record TxInfo(String orderId, boolean txActive, String txName) {}
}
