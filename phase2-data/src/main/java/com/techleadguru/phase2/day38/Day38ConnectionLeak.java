package com.techleadguru.phase2.day38;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * DAY 38 — Connection Leak Detection with `leakDetectionThreshold`
 *
 * WHAT IS A CONNECTION LEAK?
 *   A connection is borrowed from the pool but NEVER returned.
 *   Causes:
 *     1. Exception thrown before connection.close() is called.
 *     2. Forgot to close ResultSet/PreparedStatement (resources leak onto connection).
 *     3. Long-running transaction never commits/rolls back.
 *     4. Bug: @Transactional on wrong method, connection held across HTTP request.
 *
 *   SYMPTOMS:
 *     - hikaricp.connections.pending > 0 — threads waiting for connection
 *     - SQLTimeoutException: "Unable to acquire JDBC Connection"
 *     - App becomes unresponsive to all DB operations
 *     - Only fix: restart the application
 *
 * HIKARICP LEAK DETECTION:
 *   Set: spring.datasource.hikari.leak-detection-threshold=2000 (ms)
 *   Effect: If a connection is held for > 2000ms, HikariCP logs a WARN with the
 *   full stack trace of where the connection was borrowed.
 *
 *   LOG SAMPLE:
 *   WARN HikariPool - Connection leak detection triggered for conn0:
 *     url=jdbc:postgresql://... on thread http-nio-8080-exec-1
 *   java.lang.Exception: Apparent connection leak detected
 *     at com.example.OrderService.processOrder(OrderService.java:45)
 *     ...
 *
 *   IMPORTANT: This is a WARNING, not an error. The connection is NOT forcibly closed.
 *   After maxLifetime, the connection is cycled. But leak detection gives you the trace
 *   you need to find the bug.
 *
 * PRODUCTION RECOMMENDATION:
 *   Set leakDetectionThreshold to 2x your expected max query duration.
 *   If queries take max 500ms → set to 1000ms.
 *   For batch jobs that run long → disable or set high for that service.
 *   Value 0 = disabled (default).
 *
 * COMMON PATTERNS THAT CAUSE LEAKS:
 *   1. Manual connection management outside @Transactional.
 *   2. Using EntityManager directly without flush/close in scope.
 *   3. @Async methods with @Transactional — TX might not bind correctly.
 *   4. Reactive code (WebFlux) with blocking JPA — blocks thread, holds connection.
 */
@Slf4j
public class Day38ConnectionLeak {

    @Service
    @Slf4j
    public static class LeakDemoService {

        private final DataSource dataSource;

        public LeakDemoService(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        /**
         * Unwraps HikariDataSource through any JDBC proxy (e.g., P6Spy).
         */
        private HikariDataSource getHikari(DataSource ds) {
            try {
                return ds.unwrap(HikariDataSource.class);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * SAFE: try-with-resources ensures connection is returned even on exception.
         * This is the correct pattern for manual JDBC usage.
         */
        public String safeConnectionUsage() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                log.info("[Day38] Safe: connection borrowed, will be auto-returned by try-with-resources");
                return "safe-" + conn.getClass().getSimpleName();
            }
            // Connection returned here regardless of exceptions
        }

        /**
         * SAFE WITH SPRING: @Transactional manages the connection lifecycle entirely.
         * Spring binds the connection to the thread, releases it after the method.
         * You never call DataSource.getConnection() manually.
         */
        @Transactional
        public String transactionalPattern(DataSource ds) throws SQLException {
            // Spring has already borrowed a connection for this @Transactional method
            // We demonstrate by getting connection stats
            HikariDataSource hikari = getHikari(ds);
            if (hikari != null) {
                int active = hikari.getHikariPoolMXBean().getActiveConnections();
                log.info("[Day38] @Transactional: Spring manages connection. Active connections: {}", active);
                return "active=" + active;
            }
            return "unknown";
        }

        /**
         * Returns current pool leak detection configuration.
         */
        public LeakDetectionConfig getLeakDetectionConfig() {
            HikariDataSource hikari = getHikari(dataSource);
            if (hikari != null) {
                return new LeakDetectionConfig(
                        hikari.getLeakDetectionThreshold(),
                        hikari.getMaximumPoolSize(),
                        hikari.getConnectionTimeout()
                );
            }
            return new LeakDetectionConfig(0, 0, 0);
        }

        /**
         * Demonstrates what happens INSIDE a @Transactional method.
         * The connection is borrowed when the method enters and returned when it exits.
         */
        @Transactional
        public PoolSnapshot captureActiveConnections() {
            HikariDataSource hikari = getHikari(dataSource);
            if (hikari != null) {
                var mxBean = hikari.getHikariPoolMXBean();
                return new PoolSnapshot(
                        mxBean.getActiveConnections(),
                        mxBean.getIdleConnections(),
                        mxBean.getTotalConnections(),
                        mxBean.getThreadsAwaitingConnection()
                );
            }
            return new PoolSnapshot(0, 0, 0, 0);
        }
    }

    public record LeakDetectionConfig(long thresholdMs, int maxPoolSize, long connectionTimeoutMs) {
        public boolean isEnabled() {
            return thresholdMs > 0;
        }
    }

    public record PoolSnapshot(int active, int idle, int total, int waiting) {}
}
