package com.techleadguru.phase2.day37;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * DAY 37 — Connection Pool Sizing Formula + Actuator Metrics
 *
 * THE RIGHT POOL SIZE — THE FORMULA:
 *   Contrary to intuition, BIGGER POOL ≠ BETTER PERFORMANCE.
 *   A seminal study (HikariCP author, Percona): "About Pool Sizing" shows:
 *     - 10,000 requests × 1 connection each = slower than 100 connections each handling 100 requests.
 *     - CPU cores are the bottleneck — not connections.
 *
 *   FORMULA (PostgreSQL's recommendation):
 *     pool_size = ((core_count * 2) + effective_spindle_count)
 *     Where: effective_spindle_count ≈ 1 for SSD, disk count for HDD RAID.
 *
 *   EXAMPLE: 4-core app server + SSD postgres:
 *     pool_size = (4 * 2) + 1 = 9 ≈ 10
 *
 *   WHY SMALL POOL WINS:
 *     Each DB query needs CPU time. With 100 concurrent transactions each needing 1 CPU cycle,
 *     they ALL compete. Context switching overhead crushes throughput.
 *     With 10 connections: 10 run on CPUs, 90 wait in queue. No CPU thrashing.
 *
 * PRODUCTION SIZING CONSTRAINTS:
 *   DB max_connections = 500 (PostgreSQL default).
 *   DBA reserve = 50 (for DBA tools, monitoring).
 *   Available = 500 - 50 = 450.
 *   App instances = 10.
 *   Per-instance max = 450 / 10 = 45. Set maximumPoolSize = 40 (leave buffer).
 *
 * ACTUATOR METRICS (Day 37):
 *   Spring Boot Actuator exposes pool metrics at:
 *     GET /actuator/metrics/hikaricp.connections.active
 *     GET /actuator/metrics/hikaricp.connections.idle
 *     GET /actuator/metrics/hikaricp.connections.pending
 *     GET /actuator/metrics/hikaricp.connections.timeout
 *     GET /actuator/metrics/hikaricp.connections.max
 *     GET /actuator/metrics/hikaricp.connections.min
 *     GET /actuator/metrics/hikaricp.connections.acquire  (acquisition time histogram)
 *     GET /actuator/metrics/hikaricp.connections.usage    (connection usage time histogram)
 *     GET /actuator/metrics/hikaricp.connections.creation (connection creation time histogram)
 *
 *   ALERTS TO SET IN PRODUCTION:
 *     - hikaricp.connections.pending > 0 for > 5 sec → pool exhaustion risk
 *     - hikaricp.connections.timeout.total > 0 → connections being rejected (CRITICAL)
 *     - hikaricp.connections.acquire.max > 500ms → pool under pressure
 */
@Slf4j
public class Day37PoolSizing {

    @Service
    @Slf4j
    public static class PoolSizingAdvisor {

        private final DataSource dataSource;

        public PoolSizingAdvisor(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        /**
         * Calculates the recommended pool size based on the standard formula.
         * In production, use actual CPU core count and spindle count.
         */
        public int recommendedPoolSize(int cpuCores, int spindleCount) {
            int recommended = (cpuCores * 2) + spindleCount;
            log.info("[Day37] Recommended pool size: cores={}, spindles={}, recommended={}",
                    cpuCores, spindleCount, recommended);
            return recommended;
        }

        /**
         * Calculates max safe pool size per instance given DB constraints.
         */
        public PoolSizingResult calculateMaxPerInstance(int dbMaxConnections, int dbaReserve, int appInstances) {
            int available = dbMaxConnections - dbaReserve;
            int perInstance = available / appInstances;
            int safePerInstance = (int) (perInstance * 0.9); // 10% buffer

            log.info("[Day37] DB max={}, reserve={}, available={}, instances={} → max per instance={}",
                    dbMaxConnections, dbaReserve, available, appInstances, safePerInstance);

            return new PoolSizingResult(available, perInstance, safePerInstance);
        }

        /**
         * Unwraps HikariDataSource through any JDBC proxy (e.g., P6Spy).
         */
        private HikariDataSource getHikari() {
            try {
                return dataSource.unwrap(HikariDataSource.class);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Returns the current pool configuration.
         */
        public CurrentPoolConfig getCurrentConfig() {
            HikariDataSource hikari = getHikari();
            if (hikari != null) {
                return new CurrentPoolConfig(
                        hikari.getMaximumPoolSize(),
                        hikari.getMinimumIdle(),
                        hikari.getConnectionTimeout(),
                        hikari.getIdleTimeout(),
                        hikari.getMaxLifetime(),
                        hikari.getPoolName()
                );
            }
            return new CurrentPoolConfig(0, 0, 0, 0, 0, "unknown");
        }
    }

    public record PoolSizingResult(int availableConnections, int perInstance, int safePerInstance) {}

    public record CurrentPoolConfig(
            int maxPoolSize,
            int minIdle,
            long connectionTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs,
            String poolName
    ) {}
}
