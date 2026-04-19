package com.techleadguru.phase2.day28;

import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * DAY 28 — @Transactional on Private Method — Silent No-Op
 *
 * THE RULE:
 *   @Transactional on a PRIVATE method is silently ignored.
 *   Spring AOP relies on CGLIB to create a SUBCLASS proxy.
 *   CGLIB cannot override private methods (Java language rule: private = cannot subclass).
 *   The proxy is created but the @Transactional advice is NEVER applied.
 *
 * @Transactional on a NON-public method in an interface:
 *   Same problem with JDK proxy — proxy only mirrors the interface methods.
 *   Private, package-private, or protected methods in @Service classes:
 *   CGLIB can override package-private and protected methods technically,
 *   but Spring's TransactionInterceptor checks visibility and skips non-public.
 *   In practice: ONLY put @Transactional on PUBLIC methods.
 *
 * THREE FORMS OF SILENT NO-OP (@Transactional equivalent bugs):
 *   1. Private method — proxy cannot intercept.
 *   2. New-ing the class — not a Spring bean, no proxy exists.
 *   3. Self-invocation (this.method()) — bypasses proxy (Day 19).
 *
 * PRODUCTION SCENARIO — Audit log not rolling back:
 *   OrderService has a public placeOrder() that calls private writeAudit().
 *   writeAudit() is annotated @Transactional(REQUIRES_NEW).
 *   Dev tests: works correctly. Why? Because the test calls placeOrder() on the proxy.
 *   But writeAudit() itself is private — annotation has no effect.
 *   Under a specific failure path: placeOrder() TX rolls back, writeAudit() should
 *   survive (REQUIRES_NEW). It doesn't — it was in the same TX all along.
 *   Compliance audit trail incomplete.
 *   FIX: Make writeAudit() public — OR extract to AuditService @Service bean.
 */
@Slf4j
public class Day28PrivateMethodTransactional {

    // ===================================================================================
    // Uses shared.Order / shared.OrderRepository
    // ===================================================================================

    // ===================================================================================
    // THE BUG: @Transactional on private method
    // ===================================================================================

    @Service
    @Slf4j
    public static class BrokenOrderService {

        private final OrderRepository orderRepository;

        public BrokenOrderService(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        public String placeOrder(String userId, BigDecimal total) {
            // Calls private method — proxy bypassed, @Transactional ignored
            return savePending(userId, total);
        }

        // @Transactional here is SILENTLY IGNORED — private method, proxy can't intercept
        @Transactional
        private String savePending(String userId, BigDecimal total) {
            Order order = orderRepository.save(new Order(userId, total));
            log.warn("[Day28] BUG: @Transactional on private savePending() is ignored!");
            return order.getId();
        }
    }

    // ===================================================================================
    // THE FIX: move @Transactional to the public method
    // ===================================================================================

    @Service
    @Slf4j
    public static class FixedOrderService {

        private final OrderRepository orderRepository;

        public FixedOrderService(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        // @Transactional on PUBLIC method — proxy CAN intercept this
        @Transactional
        public String placeOrder(String userId, BigDecimal total) {
            return savePending(userId, total); // Private call is fine — TX already opened by proxy
        }

        private String savePending(String userId, BigDecimal total) {
            Order order = orderRepository.save(new Order(userId, total));
            log.info("[Day28] FIX: TX opened by public placeOrder(), private savePending() participates in it");
            return order.getId();
        }
    }

    // ===================================================================================
    // THE ALTERNATIVE FIX: Extract to separate @Service bean (cleanest)
    // ===================================================================================

    @Service
    @Slf4j
    public static class OrderPersistenceService {

        private final OrderRepository orderRepository;

        public OrderPersistenceService(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        // Now public in its own bean — proxy WILL intercept this
        @Transactional
        public String save(String userId, BigDecimal total, String status) {
            Order order = orderRepository.save(new Order(userId, total));
            order.setStatus(status);
            orderRepository.save(order);
            log.info("[Day28] OrderPersistenceService: saved id={}", order.getId());
            return order.getId();
        }
    }

    @Service
    @Slf4j
    public static class CleanOrderService {

        private final OrderPersistenceService persistenceService;

        public CleanOrderService(OrderPersistenceService persistenceService) {
            this.persistenceService = persistenceService;
        }

        public String placeOrder(String userId, BigDecimal total) {
            // Calls through a separate @Service — proxy intercepts perfectly
            return persistenceService.save(userId, total, "PENDING");
        }
    }
}
