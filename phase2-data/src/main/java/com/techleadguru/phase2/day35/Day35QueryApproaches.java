package com.techleadguru.phase2.day35;

import com.techleadguru.phase2.shared.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DAY 35 — JPQL vs Criteria API vs Native Query
 *
 * FOUR WAYS TO QUERY IN SPRING DATA JPA:
 *
 *   1. DERIVED QUERIES: findByUserIdAndStatus(String, String)
 *      - Pros: Zero code, Spring generates SQL. Easy to read.
 *      - Cons: Complex queries become unreadable method names. No dynamic predicates.
 *      - Use: Simple field equality/range queries.
 *
 *   2. JPQL (@Query): "SELECT o FROM Order o WHERE o.userId = :userId"
 *      - Pros: Database-agnostic (uses entity/field names). Readable. JOIN FETCH works.
 *      - Cons: Strings (no compile-time checks). Can't build dynamically.
 *      - Use: Static complex queries, JOIN FETCH, aggregate functions.
 *
 *   3. CRITERIA API: JPA type-safe programmatic query builder
 *      - Pros: 100% type-safe, refactor-proof. Perfect for dynamic filters.
 *      - Cons: Very verbose. Hard to read for complex queries.
 *      - Use: Dynamic search APIs (e.g. admin filter UI with 10 optional fields).
 *
 *   4. NATIVE SQL (@Query nativeQuery=true): Raw SQL
 *      - Pros: Full SQL power (window functions, DB-specific syntax, CTEs).
 *      - Cons: Not portable, tightly coupled to schema column names.
 *      - Use: Reports with complex SQL, performance-critical tuned queries, DB migrations.
 *
 * SPRING DATA SPECIFICATION (JpaSpecificationExecutor):
 *   Thin wrapper over Criteria API. Enables composable, reusable predicates.
 *   OrderSpec.hasStatus("PENDING").and(OrderSpec.forUser("u1")) — clean for filter APIs.
 *
 * PRODUCTION SCENARIO:
 *   Admin API: GET /orders?userId=x&status=y&minTotal=z&maxTotal=w
 *   Fields are ALL optional. Using JPQL means 2^4 = 16 different query combinations.
 *   Criteria API / Specification = 1 method, dynamic predicates, compile-time safety.
 *
 * IMPLEMENTATION NOTE:
 *   OrderJpqlRepository is defined as a top-level interface in this package.
 *   (Spring Data JPA requires top-level interfaces for reliable component scanning.)
 */
@Slf4j
public class Day35QueryApproaches {

    // ===================================================================================
    // Criteria API — direct EntityManager usage
    // ===================================================================================

    @Service
    @Slf4j
    public static class OrderCriteriaService {

        @PersistenceContext private EntityManager em;

        /**
         * Dynamically builds a query based on which filter fields are provided.
         * This is where Criteria API shines — no JPQL string manipulation needed.
         */
        @Transactional(readOnly = true)
        public List<Order> searchOrders(String userId, String status,
                                        BigDecimal minTotal, BigDecimal maxTotal) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Order> cq = cb.createQuery(Order.class);
            Root<Order> root = cq.from(Order.class);

            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minTotal != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("total"), minTotal));
            }
            if (maxTotal != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("total"), maxTotal));
            }

            cq.where(predicates.toArray(new Predicate[0]));
            cq.orderBy(cb.desc(root.get("total")));

            TypedQuery<Order> query = em.createQuery(cq);
            List<Order> results = query.getResultList();
            log.info("[Day35] Criteria search → {} results (filters: userId={}, status={}, min={}, max={})",
                results.size(), userId, status, minTotal, maxTotal);
            return results;
        }
    }

    // ===================================================================================
    // Spring Data Specification — composable Criteria predicates
    // ===================================================================================

    public static class OrderSpecs {

        public static Specification<Order> hasUserId(String userId) {
            return (root, query, cb) -> userId == null
                ? cb.conjunction()
                : cb.equal(root.get("userId"), userId);
        }

        public static Specification<Order> hasStatus(String status) {
            return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
        }

        public static Specification<Order> totalBetween(BigDecimal min, BigDecimal max) {
            return (root, query, cb) -> {
                if (min == null && max == null) return cb.conjunction();
                if (min == null) return cb.lessThanOrEqualTo(root.get("total"), max);
                if (max == null) return cb.greaterThanOrEqualTo(root.get("total"), min);
                return cb.between(root.get("total"), min, max);
            };
        }
    }

    // ===================================================================================
    // Service using both Criteria and Specification
    // ===================================================================================

    @Service
    @Slf4j
    public static class OrderSearchService {

        private final OrderJpqlRepository orderRepository;
        private final OrderCriteriaService criteriaService;

        public OrderSearchService(OrderJpqlRepository orderRepository,
                                  OrderCriteriaService criteriaService) {
            this.orderRepository = orderRepository;
            this.criteriaService = criteriaService;
        }

        // JPQL — static
        @Transactional(readOnly = true)
        public List<Order> findByUserAndStatus(String userId, String status) {
            return orderRepository.findByUserIdAndStatusJpql(userId, status);
        }

        // Specification — dynamic, composable
        @Transactional(readOnly = true)
        public List<Order> searchWithSpec(String userId, String status,
                                          BigDecimal minTotal, BigDecimal maxTotal) {
            Specification<Order> spec = OrderSpecs.hasUserId(userId)
                .and(OrderSpecs.hasStatus(status))
                .and(OrderSpecs.totalBetween(minTotal, maxTotal));
            return orderRepository.findAll(spec);
        }

        // Criteria API — direct
        @Transactional(readOnly = true)
        public List<Order> searchWithCriteria(String userId, String status,
                                              BigDecimal minTotal, BigDecimal maxTotal) {
            return criteriaService.searchOrders(userId, status, minTotal, maxTotal);
        }

        // Native SQL — raw performance
        @Transactional(readOnly = true)
        public List<Order> topOrdersForUser(String userId, BigDecimal minTotal, int limit) {
            return orderRepository.findTopOrdersByUserIdNative(userId, minTotal, limit);
        }
    }
}
