package com.techleadguru.phase2.day33;

import com.techleadguru.phase2.shared.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * DAY 33 — Fix N+1 with @EntityGraph
 *
 * WHAT IS @EntityGraph:
 *   A JPA 2.1 feature that lets you declare WHICH associations to eagerly load
 *   for a specific query — without writing JPQL. Spring Data supports it as
 *   an annotation on repository methods.
 *
 * @EntityGraph vs JOIN FETCH:
 *   - @EntityGraph works on any derived query, findById, findAll with Pageable.
 *   - JOIN FETCH requires a custom @Query — breaks when you add Pageable.
 *   - @EntityGraph is cleaner for Spring Data repositories: no JPQL needed.
 *
 * PAGINATION + @EntityGraph:
 *   @EntityGraph with Pageable uses a different strategy: Hibernate executes
 *   the paginated query first (correct SQL LIMIT/OFFSET), then loads associations
 *   in a second query using IN clause batching — safe! No in-memory pagination.
 *   (Contrast with JOIN FETCH + Pageable which loads everything then paginates in memory.)
 *
 * TWO FLAVORS:
 *   1. LOAD graph: specified attributes are EAGER, others use their declared default.
 *   2. FETCH graph: specified attributes are EAGER, ALL others default to LAZY.
 *   Spring Data @EntityGraph uses FETCH graph by default (EntityGraph.EntityGraphType.FETCH).
 *
 * MULTIPLE ASSOCIATIONS:
 *   @EntityGraph(attributePaths = {"items", "customer", "payments"})
 *   Loads all three associations in one query. JOIN FETCH would need multiple FETCH joins
 *   and might produce a Cartesian explosion. @EntityGraph handles multiple collections
 *   better by using separate SQL joins or batching.
 *
 * PRODUCTION SCENARIO:
 *   Admin dashboard shows orders with item details, paginated 20 per page.
 *   JOIN FETCH breaks pagination. @EntityGraph solves both N+1 AND pagination safely.
 */
@Slf4j
public class Day33EntityGraph {

    // Repository is defined as top-level: OrderEntityGraphRepository.java
    // (Spring Data JPA requires top-level interfaces for reliable component scanning)

    // ===================================================================================
    // Service
    // ===================================================================================

    @Service
    @Slf4j
    public static class OrderSummaryServiceEntityGraph {

        private final OrderEntityGraphRepository orderRepository;

        public OrderSummaryServiceEntityGraph(OrderEntityGraphRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        @Transactional(readOnly = true)
        public List<OrderSummary> getAllOrderSummaries() {
            List<Order> orders = orderRepository.findAll(); // 1 query via @EntityGraph
            log.info("[Day33] Loaded {} orders via @EntityGraph", orders.size());
            return orders.stream()
                .map(o -> new OrderSummary(o.getId(), o.getUserId(), o.getItems().size()))
                .toList();
        }

        @Transactional(readOnly = true)
        public List<OrderSummary> getOrdersForUser(String userId) {
            return orderRepository.findByUserId(userId)
                .stream()
                .map(o -> new OrderSummary(o.getId(), o.getUserId(), o.getItems().size()))
                .toList();
        }

        @Transactional(readOnly = true)
        public Optional<OrderSummary> getOrderById(String id) {
            return orderRepository.findWithItemsById(id)
                .map(o -> new OrderSummary(o.getId(), o.getUserId(), o.getItems().size()));
        }

        /**
         * Paginated load with eager items — works correctly unlike JOIN FETCH + Pageable.
         */
        @Transactional(readOnly = true)
        public List<OrderSummary> getOrderSummariesPaginated(Pageable pageable) {
            Page<Order> page = orderRepository.findAll(pageable);
            log.info("[Day33] Page {}/{} via @EntityGraph + Pageable",
                page.getNumber(), page.getTotalPages());
            return page.stream()
                .map(o -> new OrderSummary(o.getId(), o.getUserId(), o.getItems().size()))
                .toList();
        }
    }

    // ===================================================================================
    // Comparison summary (documentation-as-code)
    // ===================================================================================

    /**
     * DECISION GUIDE:
     *
     *   JOIN FETCH (@Query):
     *     ✅ Max performance (true single SQL JOIN)
     *     ✅ Easy to customize the query
     *     ❌ Breaks with Pageable (in-memory pagination + HHH90003004 warning)
     *     ❌ Duplicate rows without DISTINCT
     *     ❌ Can't JOIN FETCH multiple collections in one query (MultipleBagFetchException)
     *     USE WHEN: batch exports, no pagination, single collection
     *
     *   @EntityGraph:
     *     ✅ Works with Pageable (safe pagination)
     *     ✅ No JPQL needed (works with derived queries)
     *     ✅ Multiple attributePaths supported
     *     ✅ Cleaner Spring Data integration
     *     ⚠️ Hibernate may issue 2 queries for paginated result (but they're efficient)
     *     USE WHEN: REST API pages, multiple associations, need derived queries
     */
    public static final String DECISION_GUIDE = "See Javadoc above";

    public record OrderSummary(String orderId, String userId, int itemCount) {}
}
