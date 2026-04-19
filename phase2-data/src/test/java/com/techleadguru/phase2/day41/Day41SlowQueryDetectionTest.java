package com.techleadguru.phase2.day41;

import com.techleadguru.phase2.day41.Day41SlowQueryDetection.HibernateStats;
import com.techleadguru.phase2.day41.Day41SlowQueryDetection.QueryMeasurement;
import com.techleadguru.phase2.day41.Day41SlowQueryDetection.QueryStatisticsService;
import com.techleadguru.phase2.shared.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 41 — Test: Hibernate statistics and slow query detection.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day41SlowQueryDetectionTest {

    @Autowired
    QueryStatisticsService queryStatisticsService;

    @Autowired
    OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        queryStatisticsService.clearStats();
        queryStatisticsService.enableStats();
    }

    // -----------------------------------------------------------------------
    // Test 1: Hibernate stats are enabled
    // -----------------------------------------------------------------------
    @Test
    void hibernate_statistics_are_enabled() {
        HibernateStats stats = queryStatisticsService.getHibernateStats();

        // Stats object is accessible (not null)
        assertThat(stats).isNotNull();
        System.out.println("[DAY 41] Hibernate statistics accessible.");
        System.out.printf("  queries=%d, sessions=%d, loads=%d%n",
                stats.queryExecutionCount(), stats.sessionsOpened(), stats.entityLoads());
    }

    // -----------------------------------------------------------------------
    // Test 2: Query execution count increases with queries
    // -----------------------------------------------------------------------
    @Test
    void query_count_increases_with_each_query() {
        queryStatisticsService.createTestOrders("user-stats-1", 3);

        QueryMeasurement measurement = queryStatisticsService.measureQueryCost("user-stats-1");

        assertThat(measurement.queriesExecuted()).isGreaterThan(0);
        assertThat(measurement.resultCount()).isEqualTo(3);

        System.out.printf("[DAY 41] Queried userId=user-stats-1: queries=%d, entities=%d, results=%d%n",
                measurement.queriesExecuted(), measurement.entitiesLoaded(), measurement.resultCount());
    }

    // -----------------------------------------------------------------------
    // Test 3: Full stats inspection after operations
    // -----------------------------------------------------------------------
    @Test
    void full_stats_after_operations() {
        queryStatisticsService.createTestOrders("user-stats-2", 5);
        queryStatisticsService.measureQueryCost("user-stats-2");

        HibernateStats stats = queryStatisticsService.getHibernateStats();

        assertThat(stats.entityInserts()).isGreaterThanOrEqualTo(5);
        assertThat(stats.entityLoads()).isGreaterThan(0);

        System.out.println("[DAY 41] Hibernate statistics after 5 inserts + 1 query:");
        System.out.printf("  queries executed: %d%n", stats.queryExecutionCount());
        System.out.printf("  prepared statements: %d%n", stats.preparedStatements());
        System.out.printf("  entities loaded: %d, inserted: %d%n",
                stats.entityLoads(), stats.entityInserts());
        System.out.printf("  max query time: %dms%n", stats.queryMaxTime());
        if (stats.slowestQuery() != null && !stats.slowestQuery().isBlank()) {
            System.out.println("  slowest query: " + stats.slowestQuery());
        }
    }

    // -----------------------------------------------------------------------
    // Test 4: Document slow query detection strategies
    // -----------------------------------------------------------------------
    @Test
    void document_slow_query_detection_strategies() {
        System.out.println("[DAY 41] SLOW QUERY DETECTION STRATEGIES:");
        System.out.println();
        System.out.println("  1. HIBERNATE STATISTICS (app-level):");
        System.out.println("     spring.jpa.properties.hibernate.generate_statistics=true");
        System.out.println("     logging.level.org.hibernate.stat=DEBUG");
        System.out.println("     Shows: query count, max execution time, entity load counts.");
        System.out.println();
        System.out.println("  2. P6SPY (JDBC proxy):");
        System.out.println("     dependency: p6spy-spring-boot-starter");
        System.out.println("     spy.properties: filter.expression=executiontime>100");
        System.out.println("     Shows: each SQL statement with actual execution time.");
        System.out.println("     Use in: dev and staging. Overhead in production.");
        System.out.println();
        System.out.println("  3. DATABASE SLOW QUERY LOG:");
        System.out.println("     PostgreSQL: log_min_duration_statement = 500ms");
        System.out.println("     MySQL:      slow_query_log=ON; long_query_time=0.5");
        System.out.println("     Zero app overhead. Use in production.");
        System.out.println();
        System.out.println("  4. EXPLAIN ANALYZE:");
        System.out.println("     EXPLAIN ANALYZE SELECT * FROM orders WHERE user_id='xyz'");
        System.out.println("     Look for: Seq Scan on large table (missing index).");
        assertThat(true).isTrue();
    }
}
