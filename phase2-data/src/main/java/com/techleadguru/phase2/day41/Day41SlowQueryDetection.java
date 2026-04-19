package com.techleadguru.phase2.day41;

import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DAY 41 — Slow Query Detection: Hibernate Statistics + P6Spy
 *
 * TWO COMPLEMENTARY TOOLS:
 *
 * 1. HIBERNATE STATISTICS (built-in):
 *    Enable: spring.jpa.properties.hibernate.generate_statistics=true
 *    Log:    logging.level.org.hibernate.stat=DEBUG
 *    Shows:
 *      - Total queries executed
 *      - Query execution times (max, min, avg)
 *      - Flush count, session count
 *      - Cache hits/misses (if L2 cache enabled)
 *    LIMITATION: Aggregated stats only. Can't see individual slow queries.
 *
 * 2. P6SPY (JDBC proxy):
 *    Dependency: p6spy-spring-boot-starter
 *    Intercepts every SQL statement with execution time.
 *    spy.properties: filter.expression=executiontime>100 (show queries > 100ms)
 *    LOG SAMPLE:
 *      #1736843022391 | took 245ms | statement | connection 3| url jdbc:p6spy:h2...
 *      SELECT o.* FROM orders o WHERE o.user_id = ?
 *    Use for: finding exact slow queries in development + staging.
 *
 * 3. SLOW QUERY LOG (PostgreSQL / MySQL):
 *    PostgreSQL: log_min_duration_statement = 500ms   (log queries > 500ms)
 *    MySQL:      slow_query_log = ON; long_query_time = 0.5
 *    Use for: production slow query hunting without app-level overhead.
 *
 * 4. QUERY PLAN ANALYSIS:
 *    EXPLAIN ANALYZE SELECT ...  — shows the execution plan + actual times in PostgreSQL.
 *    Look for: "Seq Scan" on large tables (missing index), HashJoin on large datasets.
 *    Tool: explain.dalibo.com — visualizes PostgreSQL query plans.
 *
 * COMMON SLOW QUERY PATTERNS:
 *   1. N+1 queries — 100 orders → 100 user queries (Days 31-34)
 *   2. Missing index on WHERE/JOIN columns
 *   3. Full table scan: SELECT * FROM orders (no WHERE clause)
 *   4. Cartesian join: missing JOIN condition
 *   5. Over-fetching: SELECT * instead of SELECT only needed columns
 *
 * THIS DEMO: Uses Hibernate Statistics API to show query counts and timing.
 * P6Spy is configured in application.properties (spy.properties in resources).
 */
@Slf4j
public class Day41SlowQueryDetection {

    @Service
    @Slf4j
    public static class QueryStatisticsService {

        private final EntityManagerFactory emf;
        private final OrderRepository orderRepository;

        public QueryStatisticsService(EntityManagerFactory emf, OrderRepository orderRepository) {
            this.emf = emf;
            this.orderRepository = orderRepository;
        }

        /**
         * Returns current Hibernate statistics.
         * These are cumulative since app startup (or last clearStats() call).
         */
        public HibernateStats getHibernateStats() {
            Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
            return new HibernateStats(
                    stats.getQueryExecutionCount(),
                    stats.getQueryExecutionMaxTime(),
                    stats.getQueryExecutionMaxTimeQueryString(),
                    stats.getPrepareStatementCount(),
                    stats.getEntityLoadCount(),
                    stats.getEntityInsertCount(),
                    stats.getEntityUpdateCount(),
                    stats.getEntityDeleteCount(),
                    stats.getSessionOpenCount(),
                    stats.getFlushCount()
            );
        }

        /**
         * Resets Hibernate statistics to start fresh.
         */
        public void clearStats() {
            emf.unwrap(SessionFactory.class).getStatistics().clear();
            log.info("[Day41] Hibernate statistics cleared");
        }

        /**
         * Enables Hibernate statistics at runtime (normally set in application.properties).
         */
        public void enableStats() {
            emf.unwrap(SessionFactory.class).getStatistics().setStatisticsEnabled(true);
            log.info("[Day41] Hibernate statistics enabled");
        }

        /**
         * Runs a simple query and captures before/after statistics to measure it.
         */
        @Transactional
        public QueryMeasurement measureQueryCost(String userId) {
            Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
            long queriesBefore = stats.getQueryExecutionCount();
            long loadsBefore = stats.getEntityLoadCount();

            // Execute the query
            List<Order> orders = orderRepository.findByUserId(userId);

            long queriesAfter = stats.getQueryExecutionCount();
            long loadsAfter = stats.getEntityLoadCount();

            QueryMeasurement measurement = new QueryMeasurement(
                    (int) (queriesAfter - queriesBefore),
                    (int) (loadsAfter - loadsBefore),
                    orders.size()
            );
            log.info("[Day41] Query cost: queries={}, entities loaded={}, results={}",
                    measurement.queriesExecuted(), measurement.entitiesLoaded(), measurement.resultCount());
            return measurement;
        }

        /**
         * Creates test orders for benchmarking.
         */
        @Transactional
        public void createTestOrders(String userId, int count) {
            List<Order> orders = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                orders.add(new Order(userId, new BigDecimal("10.00").add(BigDecimal.valueOf(i))));
            }
            orderRepository.saveAll(orders);
            log.info("[Day41] Created {} test orders for userId={}", count, userId);
        }
    }

    public record HibernateStats(
            long queryExecutionCount,
            long queryMaxTime,
            String slowestQuery,
            long preparedStatements,
            long entityLoads,
            long entityInserts,
            long entityUpdates,
            long entityDeletes,
            long sessionsOpened,
            long flushCount
    ) {}

    public record QueryMeasurement(int queriesExecuted, int entitiesLoaded, int resultCount) {}
}
