package com.techleadguru.phase2.day34;

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
 * DAY 34 — Fix N+1 with @BatchSize
 *
 * WHAT IS @BatchSize:
 *   A Hibernate-specific annotation that changes lazy collection loading
 *   from "N individual SELECTs" to "M batched SELECTs using IN clause".
 *
 *   Without @BatchSize:  N SELECTs: WHERE order_id = 'a', WHERE order_id = 'b', ...
 *   With @BatchSize(50): SELECT items WHERE order_id IN ('a','b','c',...up to 50)
 *
 *   For N=100 and batch=50: 100 individual queries → 2 batch queries.
 *   For N=50 and batch=50: 50 individual queries → 1 batch query.
 *
 * TWO WAYS TO APPLY:
 *   1. On the collection field: @BatchSize(size=50) on @OneToMany
 *   2. On the entity class: @BatchSize(size=50) on @Entity (batches entity loading)
 *   3. Global: spring.jpa.properties.hibernate.default_batch_fetch_size=50
 *
 * @BatchSize vs JOIN FETCH vs @EntityGraph:
 *   - @BatchSize: Lazy loading is preserved. Access triggers batched IN queries.
 *     Best when you DON'T always need the association but want efficient fallback.
 *   - JOIN FETCH: Always loads the association eagerly in a JOIN. Best for exports.
 *   - @EntityGraph: Configurable per-query eagerness. Best for API endpoints.
 *
 * WHEN TO USE @BatchSize:
 *   - You genuinely need lazy loading in some code paths, not others.
 *   - Multiple associations: global default_batch_fetch_size covers all lazily.
 *   - Legacy code: can't change queries but want to reduce query count.
 *   - Simple drop-in improvement with no query rewrites.
 *
 * IMPLEMENTATION NOTE:
 *   The shared Order entity already has @BatchSize(size=10) on its items collection
 *   (see shared.Order.java). This class demonstrates usage via shared.OrderRepository
 *   and shared.OrderItemRepository — no custom entity classes needed.
 *
 * PRODUCTION SCENARIO:
 *   ProductCatalogService loads 200 categories. Sometimes it needs subcategories
 *   (for admin), sometimes not (for public API). @BatchSize(50) means admin use
 *   triggers 4 queries (200/50) instead of 200.
 *
 * LIMITATION:
 *   @BatchSize doesn't work in the same cross-vendor way as JOIN FETCH.
 *   It's a Hibernate extension (not JPA standard). Works fine with Spring Boot + Hibernate.
 *   The batch size is a hint — Hibernate may use smaller batches for edge cases.
 */
@Slf4j
public class Day34BatchSize {

    // ===================================================================================
    // Service
    // ===================================================================================

    @Service
    @Slf4j
    public static class BatchOrderService {

        private final OrderRepository orderRepository;
        private final OrderItemRepository orderItemRepository;

        public BatchOrderService(OrderRepository orderRepository,
                                 OrderItemRepository orderItemRepository) {
            this.orderRepository = orderRepository;
            this.orderItemRepository = orderItemRepository;
        }

        @Transactional
        public Order createOrder(String userId, List<String> products) {
            Order order = new Order(userId, BigDecimal.TEN);
            products.forEach(p -> order.addItem(new OrderItem(p, 1, new BigDecimal("5.00"))));
            return orderRepository.save(order);
        }

        /**
         * Uses findAll() — lazy load. When items.size() is called,
         * @BatchSize(10) on shared.Order.items ensures Hibernate batches the SELECTs.
         * For 25 orders: ceil(25/10) = 3 item queries instead of 25.
         */
        @Transactional(readOnly = true)
        public List<OrderSummary> getAllWithBatchLoading() {
            List<Order> orders = orderRepository.findAll();
            log.info("[Day34] Loading {} orders — @BatchSize(10) will batch item queries", orders.size());
            return orders.stream()
                .map(o -> {
                    int count = o.getItems().size();   // triggers batched IN query
                    log.info("[Day34] Order {} has {} items (loaded via batch)", o.getId(), count);
                    return new OrderSummary(o.getId(), o.getUserId(), count);
                })
                .toList();
        }
    }

    public record OrderSummary(String orderId, String userId, int itemCount) {}

    // ===================================================================================
    // Global configuration note
    // ===================================================================================

    /**
     * APPLICATION.PROPERTIES EQUIVALENT:
     *   spring.jpa.properties.hibernate.default_batch_fetch_size=50
     *
     * This sets a global default batch size for ALL lazy collections and entities.
     * Your application.properties already has this set for demonstration purposes.
     *
     * RULE OF THUMB:
     *   - default_batch_fetch_size=50 is a good starting point.
     *   - Fine-tune per entity/collection with explicit @BatchSize if needed.
     *   - For graphs with depth > 2, consider JOIN FETCH or @EntityGraph instead.
     */
    public static final String GLOBAL_CONFIG_NOTE = "See application.properties";
}
