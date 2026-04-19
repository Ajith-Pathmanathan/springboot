package com.techleadguru.phase2.day36;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * DAY 36 — HikariCP Connection Pool Internals
 *
 * WHAT IS A CONNECTION POOL?
 *   Creating a new DB connection is EXPENSIVE:
 *     - TCP handshake with DB server
 *     - Authentication
 *     - Session initialization (≈5-20ms per connection)
 *   For 1,000 requests/sec each needing a connection → 5,000-20,000ms overhead/sec — unacceptable.
 *   Solution: pre-create N connections at startup, reuse them → borrowing from pool is < 1ms.
 *
 * HIKARICP — WHY IT'S THE DEFAULT IN SPRING BOOT:
 *   - Fastest JDBC connection pool (benchmarks: 2-3x faster than C3P0/DBCP)
 *   - Zero overhead: bytes-to-bus optimized, minimal lock contention
 *   - Tiny codebase (~120KB JAR) → easy to reason about
 *   - Used at Airbnb, Netflix, Yahoo Finance scale
 *
 * POOL LIFECYCLE:
 *   1. Startup: HikariCP creates `minimumIdle` connections.
 *   2. A thread calls DataSource.getConnection() → borrows from pool.
 *   3. Thread executes SQL, calls connection.close() → returns to pool (NOT really closed).
 *   4. Pool grows up to `maximumPoolSize` under load.
 *   5. Idle connections above `minimumIdle` are closed after `idleTimeout`.
 *   6. Connections alive > `maxLifetime` are cycled to prevent stale DB connections.
 *
 * KEY CONFIGURATION PROPERTIES:
 *   maximumPoolSize     — max connections (default: 10). MOST IMPORTANT tuning parameter.
 *   minimumIdle         — min idle connections kept warm (default: = maximumPoolSize).
 *   connectionTimeout   — max wait time to borrow a connection (default: 30s). Throw if exceeded.
 *   idleTimeout         — time before idle connection above minimumIdle is closed (default: 10min).
 *   maxLifetime         — max connection age (default: 30min). Must be shorter than DB wait_timeout.
 *   leakDetectionThreshold — warn if connection held > this ms (Day 38).
 *   keepaliveTime       — interval to PING idle connections to prevent firewall drops (default: 0).
 *
 * COMMON PRODUCTION MISTAKE: maximumPoolSize too large.
 *   DB max_connections = 100. App has 20 instances × 20 poolSize = 400. DB refuses connections.
 *   Rule: (total_app_connections) < (DB max_connections - DBA_reserve)
 *   See Day 37 for sizing formula.
 */
@Slf4j
public class Day36HikariCpInternals {

    @Service
    @Slf4j
    public static class PoolInspector {

        private final DataSource dataSource;

        public PoolInspector(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        /**
         * Unwraps HikariDataSource through any JDBC proxy (e.g., P6Spy).
         * DataSource.unwrap() traverses the wrapper chain to find the underlying type.
         */
        private HikariDataSource getHikari() {
            try {
                return dataSource.unwrap(HikariDataSource.class);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Returns live pool metrics by casting to HikariDataSource.
         * HikariPoolMXBean exposes pool state: active, idle, waiting, total.
         */
        public PoolStats getPoolStats() {
            HikariDataSource hikari = getHikari();
            if (hikari != null) {
                HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
                return new PoolStats(
                        pool.getActiveConnections(),
                        pool.getIdleConnections(),
                        pool.getThreadsAwaitingConnection(),
                        pool.getTotalConnections(),
                        hikari.getMaximumPoolSize(),
                        hikari.getMinimumIdle(),
                        hikari.getConnectionTimeout(),
                        hikari.getIdleTimeout(),
                        hikari.getMaxLifetime()
                );
            }
            return PoolStats.empty();
        }

        /**
         * Demonstrates borrowing a connection explicitly.
         * In normal Spring usage you never do this — @Transactional handles it.
         * This method shows what happens "under the hood" on every @Transactional method.
         */
        @Transactional
        public String borrowAndReturnConnection() throws SQLException {
            try (Connection conn = dataSource.getConnection()) {
                String connectionInfo = String.format(
                        "class=%s, catalog=%s, autoCommit=%s",
                        conn.getClass().getSimpleName(),
                        conn.getCatalog(),
                        conn.getAutoCommit()
                );
                log.info("[Day36] Borrowed connection: {}", connectionInfo);
                return connectionInfo;
            }
            // Connection returned to pool when try-with-resources closes it
        }

        /**
         * Shows the pool is named for diagnostics.
         */
        public String getPoolName() {
            HikariDataSource hikari = getHikari();
            return hikari != null ? hikari.getPoolName() : "unknown";
        }

        /**
         * Shows HikariCP is wired as the DataSource (possibly through a proxy like P6Spy).
         */
        public boolean isHikariDataSource() {
            return getHikari() != null;
        }
    }

    public record PoolStats(
            int activeConnections,
            int idleConnections,
            int threadsAwaiting,
            int totalConnections,
            int maxPoolSize,
            int minIdle,
            long connectionTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs
    ) {
        public static PoolStats empty() {
            return new PoolStats(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
