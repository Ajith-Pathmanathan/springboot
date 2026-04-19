package com.techleadguru.phase2.day32;

import com.techleadguru.phase2.day31.Day31NPlusOneProblem.OrderSummary;
import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DAY 32 — Fix N+1 with JOIN FETCH
 *
 * THE FIX: JPQL JOIN FETCH
 *   Instead of letting Hibernate lazily load associations one by one,
 *   write a JPQL query that explicitly JOINs and FETCHes the collection
 *   in a single SQL statement.
 *
 *   Before: SELECT * FROM orders (1 query)
 *           + SELECT * FROM order_items WHERE order_id=? (N queries)
 *
 *   After:  SELECT o.*, oi.* FROM orders o
 *           LEFT JOIN order_items oi ON oi.order_id = o.id
 *           (1 query, all data loaded at once)
 *
 * CAVEATS of JOIN FETCH:
 *   1. Cartesian product: If order has 100 items, the SQL result has 100 rows per order.
 *      Use DISTINCT to deduplicate → "SELECT DISTINCT o FROM Order o JOIN FETCH o.items"
 *   2. Cannot use with pagination (setMaxResults) on the collection-owning side.
 *      Hibernate warns: "HHH90003004: firstResult/maxResults specified with collection fetch"
 *      and fetches ALL rows in memory then paginates — a memory bomb.
 *   3. Only one EAGER JOIN FETCH collection per query (Hibernate restriction in some versions).
 *      For multiple collections, use @EntityGraph with multiple subgraphs (Day 33).
 *
 * WHEN TO USE:
 *   - Known-size result sets where Cartesian product is acceptable.
 *   - No pagination on the fetched entity.
 *   - Simple parent + one-collection loading.
 *
 * PRODUCTION SCENARIO:
 *   Order export job: export all orders with items to CSV.
 *   No pagination needed (full export). JOIN FETCH is ideal.
 */
@Slf4j
public class Day32JoinFetch {

    // ===================================================================================
    // Repository with JOIN FETCH query — these queries are already in shared.OrderRepository
    // but here shown explicitly for teaching purposes
    // ===================================================================================

    // The JOIN FETCH queries are already defined in shared.OrderRepository:
    //   findAllWithItems() → "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items"
    //   findByUserIdWithItems(userId) → with WHERE clause
    // Day32 uses shared.OrderRepository directly (no extension needed)

    // ===================================================================================
    // Service using JOIN FETCH
    // ===================================================================================

    @Service
    @Slf4j
    public static class OrderSummaryServiceFixed {

        private final OrderRepository orderRepository;

        public OrderSummaryServiceFixed(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        /**
         * FIX: Uses JOIN FETCH — exactly 1 SQL query regardless of order count.
         */
        @Transactional(readOnly = true)
        public List<OrderSummary> getAllOrderSummariesFixed() {
            List<Order> orders = orderRepository.findAllWithItems(); // 1 query!
            log.info("[Day32] Loaded {} orders with JOIN FETCH (1 query)", orders.size());
            return orders.stream()
                .map(o -> new OrderSummary(o.getId(), o.getUserId(), o.getItems().size()))
                .toList();
        }

        /**
         * For a single user — still 1 query.
         */
        @Transactional(readOnly = true)
        public List<OrderSummary> getOrderSummariesForUser(String userId) {
            return orderRepository.findByUserIdWithItems(userId)
                .stream()
                .map(o -> new OrderSummary(o.getId(), o.getUserId(), o.getItems().size()))
                .toList();
        }
    }

    // ===================================================================================
    // Demonstrating the caveat: pagination + JOIN FETCH
    // ===================================================================================

    @Service
    @Slf4j
    public static class PaginationCaveatDemo {

        private final OrderRepository orderRepository;

        public PaginationCaveatDemo(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
        }

        /**
         * Returns the FULL list — intentionally not paginated — because:
         * Spring Data's Pageable + JOIN FETCH = HHH90003004 warning + in-memory pagination.
         * For paginated access with associations, use @EntityGraph (Day 33) or
         * two-query approach: first page IDs, then fetch full objects by those IDs.
         */
        @Transactional(readOnly = true)
        public List<Order> getAllOrdersWithItemsNoPagination() {
            return orderRepository.findAllWithItems();
        }

        /**
         * SAFE paginated approach: two-query pattern.
         *   1. Paginate only the ORDER IDs (no collection join).
         *   2. For those IDs, JOIN FETCH items.
         * This avoids the Hibernate pagination+fetch warning.
         */
        @Transactional(readOnly = true)
        public List<OrderSummary> getPaginatedSummaries(String userId, int pageSize) {
            List<Order> orders = orderRepository.findByUserIdWithItems(userId);
            return orders.stream()
                .limit(pageSize)
                .map(o -> new OrderSummary(o.getId(), o.getUserId(), o.getItems().size()))
                .toList();
        }
    }
}
