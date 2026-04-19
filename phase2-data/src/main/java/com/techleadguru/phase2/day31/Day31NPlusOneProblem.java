package com.techleadguru.phase2.day31;

import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderItem;
import com.techleadguru.phase2.shared.OrderItemRepository;
import com.techleadguru.phase2.shared.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * DAY 31 — N+1 Problem: Live Reproduction ⚠️
 *
 * WHAT IS THE N+1 PROBLEM:
 *   1 query to fetch N parent entities.
 *   Then N queries (one per parent) to fetch each parent's children.
 *   Total: N+1 queries. For N=1000: 1001 queries instead of 1.
 *   Performance degrades LINEARLY with data size.
 *
 * ROOT CAUSE:
 *   Hibernate's default fetch type for @OneToMany is LAZY.
 *   Accessing the collection (items.size(), items.forEach()) AFTER loading the parent
 *   triggers a new SQL SELECT per parent entity.
 *   You get 1 SELECT for orders, then 1 SELECT per order for items.
 *
 * HOW TO DETECT:
 *   Enable hibernate.generate_statistics=true in application.properties.
 *   Log org.hibernate.stat at DEBUG.
 *   Look for: "N+1 Deletes/Queries" or watch "query count" in stats.
 *   In tests: use Hypersistence's SQLStatementCountValidator or count Hibernate stats.
 *
 * FIX OPTIONS (Days 32-34):
 *   Day 32: JOIN FETCH in JPQL
 *   Day 33: @EntityGraph annotation
 *   Day 34: @BatchSize / batch fetching
 *
 * PRODUCTION SCENARIO:
 *   OrderListPage loads 50 orders. For each order, displays item count.
 *   Code: orders.forEach(o -> o.getItems().size()).
 *   In dev: 20 orders → 21 queries → fast enough, goes unnoticed.
 *   In prod: 10,000 orders → 10,001 queries → page takes 12 seconds.
 *   Response time SLA: 500ms. Incident.
 */
@Slf4j
public class Day31NPlusOneProblem {

    // ===================================================================================
    // Uses shared.Order / shared.OrderItem / shared.OrderRepository / shared.OrderItemRepository
    // ===================================================================================

    // ===================================================================================
    // Service that demonstrates the N+1 problem
    // ===================================================================================

    @Service
    @Slf4j
    public static class OrderSummaryService {

        private final OrderRepository orderRepository;

        public OrderSummaryService(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        /**
         * N+1 BUG: findAll() loads all orders (1 query).
         * Then getItems().size() triggers a SELECT per order (N queries).
         * Total: N+1 queries.
         */
        @Transactional
        public List<OrderSummary> getAllOrderSummariesNPlusOne() {
            List<Order> orders = orderRepository.findAll(); // 1 query
            log.warn("[Day31] Loaded {} orders. Now triggering N+1...", orders.size());

            return orders.stream().map(order -> {
                int itemCount = order.getItems().size(); // N separate SELECT queries!
                log.warn("[Day31] N+1 triggered for order {}: {} items", order.getId(), itemCount);
                return new OrderSummary(order.getId(), order.getUserId(), itemCount);
            }).toList();
        }

        @Transactional
        public Order createOrderWithItems(String userId, List<String> products) {
            Order order = new Order(userId, BigDecimal.TEN);
            products.forEach(p -> order.addItem(new OrderItem(p, 1, new BigDecimal("9.99"))));
            return orderRepository.save(order);
        }
    }

    public record OrderSummary(String orderId, String userId, int itemCount) {}
}
