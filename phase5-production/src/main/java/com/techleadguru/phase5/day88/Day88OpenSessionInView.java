package com.techleadguru.phase5.day88;

import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DAY 88 — OpenSessionInView (OSIV) Anti-Pattern
 *
 * WHAT IS OSIV?
 *   spring.jpa.open-in-view=true (default in Spring Boot!)
 *   Opens a Hibernate Session at the beginning of the HTTP request and closes it
 *   AFTER the view is rendered (including JSON serialization).
 *   This means lazy-loaded associations can be loaded during serialization.
 *
 * WHY IT'S AN ANTI-PATTERN:
 *   1. DB connection held open for the entire HTTP request duration
 *      → requests that take 200ms hold a connection for 200ms
 *      → with max pool 10, only 10 concurrent requests can hold DB
 *      → pool exhaustion under load (same as Day 85 pattern)
 *
 *   2. N+1 queries happen silently in the view layer
 *      → controller returns List<Order>, serializer accesses order.getItems()
 *      → each .getItems() fires a new query → 1 + N queries total
 *      → performance cliff with large datasets
 *
 *   3. Lazy loading hides real data access patterns from developers
 *      → "works on my machine" breaks in production under load
 *
 * THE FIX (phase5 application.properties has it already):
 *   spring.jpa.open-in-view=false
 *   → Forces you to load everything inside the @Transactional boundary
 *   → LazyInitializationException happens in dev, not prod = fail fast
 *   → Use @EntityGraph or JOIN FETCH to eagerly load needed associations
 *
 * WARNINGS IN LOG:
 *   Spring Boot logs at startup: "spring.jpa.open-in-view is enabled by default."
 *   This is a hint that you should disable it.
 */
@Slf4j
public class Day88OpenSessionInView {

    // =========================================================================
    // Entities
    // =========================================================================

    @Entity
    @Table(name = "orders_day88")
    public static class Order {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String customerId;

        @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
        private List<OrderItem> items;

        public Order() {}
        public Order(String customerId) { this.customerId = customerId; }
        public Long getId()              { return id; }
        public String getCustomerId()    { return customerId; }
        public List<OrderItem> getItems() { return items; }
        public void setItems(List<OrderItem> items) { this.items = items; }
    }

    @Entity
    @Table(name = "order_items_day88")
    public static class OrderItem {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "order_id")
        private Order order;

        private String productName;
        private int quantity;

        public OrderItem() {}
        public OrderItem(Order order, String productName, int quantity) {
            this.order = order; this.productName = productName; this.quantity = quantity;
        }
        public Long getId()          { return id; }
        public String getProductName() { return productName; }
        public int getQuantity()     { return quantity; }
    }

    // =========================================================================
    // Repositories
    // =========================================================================

    public interface OrderRepository extends org.springframework.data.jpa.repository.JpaRepository<Order, Long> {

        /** With OSIV=false, this will trigger LazyInitializationException if items accessed outside TX */
        List<Order> findByCustomerId(String customerId);

        /** FIXED: JOIN FETCH inside the query loads items eagerly */
        @org.springframework.data.jpa.repository.Query(
                "SELECT DISTINCT o FROM Day88OpenSessionInView$Order o " +
                "LEFT JOIN FETCH o.items WHERE o.customerId = :customerId")
        List<Order> findByCustomerIdWithItems(String customerId);
    }

    // =========================================================================
    // Service — demonstrates both patterns
    // =========================================================================

    @Service
    @Slf4j
    public static class OrderService {

        private final OrderRepository repos;

        public OrderService(OrderRepository repos) {
            this.repos = repos;
        }

        @Transactional
        public Order createOrder(String customerId, List<String> products) {
            Order order = repos.save(new Order(customerId));
            // Use mutable ArrayList — Hibernate requires it for collection management
            List<OrderItem> items = new java.util.ArrayList<>();
            for (String p : products) {
                items.add(new OrderItem(order, p, 1));
            }
            order.setItems(items);
            return repos.save(order);
        }

        /**
         * BROKEN pattern (with OSIV=true it works; with OSIV=false it throws):
         * Returns detached Order — if caller accesses .getItems() outside TX,
         * LazyInitializationException fires.
         */
        @Transactional(readOnly = true)
        public List<Order> findOrders(String customerId) {
            return repos.findByCustomerId(customerId);
        }

        /**
         * FIXED pattern: eagerly load items inside the transaction.
         * Works correctly regardless of OSIV setting.
         */
        @Transactional(readOnly = true)
        public List<Order> findOrdersWithItems(String customerId) {
            List<Order> orders = repos.findByCustomerIdWithItems(customerId);
            log.debug("[Day88] Loaded {} orders with items for customer {}", orders.size(), customerId);
            return orders;
        }

        /**
         * Demonstrates LazyInitializationException when OSIV=false and
         * lazy association accessed outside transaction.
         */
        public boolean wouldThrowLazyInitExceptionOutsideTx(Long orderId) {
            // orderId entity is detached once @Transactional method returns
            Order order = findOrderById(orderId);
            try {
                int size = order.getItems().size(); // triggers lazy load → FAILS outside TX
                return false; // returned false — didn't throw (OSIV=true mode)
            } catch (LazyInitializationException e) {
                log.info("[Day88] LazyInitializationException caught — OSIV=false is working correctly");
                return true; // threw LazyInitializationException — OSIV=false mode
            }
        }

        @Transactional(readOnly = true)
        public Order findOrderById(Long id) {
            return repos.findById(id).orElseThrow();
        }

        @Transactional(readOnly = true)
        public long countOrders() { return repos.count(); }
    }
}
