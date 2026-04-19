package com.techleadguru.phase2.day36;

import com.techleadguru.phase2.day36.Day36HikariCpInternals.PoolInspector;
import com.techleadguru.phase2.day36.Day36HikariCpInternals.PoolStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 36 — Test: HikariCP connection pool internals.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day36HikariCpInternalsTest {

    @Autowired
    PoolInspector poolInspector;

    // -----------------------------------------------------------------------
    // Test 1: Verify HikariCP is wired as the DataSource
    // -----------------------------------------------------------------------
    @Test
    void hikari_is_the_datasource() {
        assertThat(poolInspector.isHikariDataSource()).isTrue();

        System.out.println("[DAY 36] HikariCP is the DataSource — confirmed.");
    }

    // -----------------------------------------------------------------------
    // Test 2: Read pool stats (active, idle, total, max)
    // -----------------------------------------------------------------------
    @Test
    void pool_stats_are_accessible() {
        PoolStats stats = poolInspector.getPoolStats();

        assertThat(stats.maxPoolSize()).isGreaterThan(0);
        assertThat(stats.totalConnections()).isGreaterThanOrEqualTo(0);
        assertThat(stats.connectionTimeoutMs()).isGreaterThan(0);

        System.out.println("[DAY 36] Pool stats:");
        System.out.printf("  maxPoolSize=%d, minIdle=%d%n", stats.maxPoolSize(), stats.minIdle());
        System.out.printf("  total=%d, active=%d, idle=%d, waiting=%d%n",
                stats.totalConnections(), stats.activeConnections(),
                stats.idleConnections(), stats.threadsAwaiting());
        System.out.printf("  connectionTimeout=%dms, idleTimeout=%dms, maxLifetime=%dms%n",
                stats.connectionTimeoutMs(), stats.idleTimeoutMs(), stats.maxLifetimeMs());
    }

    // -----------------------------------------------------------------------
    // Test 3: Borrow and return connection manually
    // -----------------------------------------------------------------------
    @Test
    void borrow_and_return_connection() throws Exception {
        String info = poolInspector.borrowAndReturnConnection();

        assertThat(info).isNotBlank();
        System.out.println("[DAY 36] Connection info: " + info);
        System.out.println("[DAY 36] Connection returned to pool after try-with-resources.");
    }

    // -----------------------------------------------------------------------
    // Test 4: Document HikariCP lifecycle
    // -----------------------------------------------------------------------
    @Test
    void document_hikari_lifecycle() {
        System.out.println("[DAY 36] HIKARICP LIFECYCLE:");
        System.out.println("  1. Startup: creates minimumIdle connections.");
        System.out.println("  2. getConnection(): borrows from pool (< 1ms if available).");
        System.out.println("  3. connection.close(): RETURNS to pool (not really closed).");
        System.out.println("  4. Grows to maximumPoolSize under load.");
        System.out.println("  5. Idle connections > minimumIdle closed after idleTimeout.");
        System.out.println("  6. Connections > maxLifetime cycled to prevent DB drops.");
        System.out.println();
        System.out.println("  KEY CONFIG:");
        System.out.println("  maximumPoolSize      — MOST critical. Default: 10.");
        System.out.println("  connectionTimeout    — max wait to borrow. Default: 30s.");
        System.out.println("  idleTimeout          — idle connection TTL. Default: 10min.");
        System.out.println("  maxLifetime          — connection max age. Default: 30min.");
        System.out.println("  leakDetectionThreshold — warn if held > Nms. Default: 0 (off).");
        assertThat(true).isTrue();
    }
}
