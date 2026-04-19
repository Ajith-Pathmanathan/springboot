package com.techleadguru.phase2.day37;

import com.techleadguru.phase2.day37.Day37PoolSizing.CurrentPoolConfig;
import com.techleadguru.phase2.day37.Day37PoolSizing.PoolSizingAdvisor;
import com.techleadguru.phase2.day37.Day37PoolSizing.PoolSizingResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 37 — Test: Pool sizing formula and configuration inspection.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day37PoolSizingTest {

    @Autowired
    PoolSizingAdvisor advisor;

    // -----------------------------------------------------------------------
    // Test 1: Recommended pool size formula
    // -----------------------------------------------------------------------
    @Test
    void recommended_pool_size_formula() {
        // 4-core server + SSD (spindle count = 1)
        int recommended = advisor.recommendedPoolSize(4, 1);

        assertThat(recommended).isEqualTo(9); // (4 * 2) + 1 = 9

        System.out.println("[DAY 37] Formula: (cores * 2) + spindles = recommended");
        System.out.printf("[DAY 37] 4 cores + 1 SSD = %d connections recommended%n", recommended);
    }

    // -----------------------------------------------------------------------
    // Test 2: Calculate max per instance given DB constraints
    // -----------------------------------------------------------------------
    @Test
    void max_per_instance_calculation() {
        // DB max_connections=500, DBA reserve=50, 10 app instances
        PoolSizingResult result = advisor.calculateMaxPerInstance(500, 50, 10);

        assertThat(result.availableConnections()).isEqualTo(450);
        assertThat(result.perInstance()).isEqualTo(45);
        assertThat(result.safePerInstance()).isEqualTo(40); // 90% of 45

        System.out.printf("[DAY 37] DB max=500, reserve=50(DBA), available=%d%n", result.availableConnections());
        System.out.printf("[DAY 37] 10 instances → max per instance=%d, safe=%d%n",
                result.perInstance(), result.safePerInstance());
    }

    // -----------------------------------------------------------------------
    // Test 3: Current pool configuration matches application.properties
    // -----------------------------------------------------------------------
    @Test
    void current_pool_config_matches_properties() {
        CurrentPoolConfig config = advisor.getCurrentConfig();

        assertThat(config.maxPoolSize()).isEqualTo(10);
        assertThat(config.minIdle()).isEqualTo(2);
        assertThat(config.connectionTimeoutMs()).isEqualTo(30000);

        System.out.println("[DAY 37] Current pool config:");
        System.out.printf("  maxPoolSize=%d, minIdle=%d%n", config.maxPoolSize(), config.minIdle());
        System.out.printf("  connectionTimeout=%dms%n", config.connectionTimeoutMs());
        System.out.printf("  idleTimeout=%dms, maxLifetime=%dms%n",
                config.idleTimeoutMs(), config.maxLifetimeMs());
        System.out.printf("  poolName=%s%n", config.poolName());
    }

    // -----------------------------------------------------------------------
    // Test 4: Document Actuator metrics
    // -----------------------------------------------------------------------
    @Test
    void document_actuator_metrics() {
        System.out.println("[DAY 37] HIKARICP ACTUATOR METRICS:");
        System.out.println("  Add spring-boot-actuator dependency to expose:");
        System.out.println("  GET /actuator/metrics/hikaricp.connections.active");
        System.out.println("  GET /actuator/metrics/hikaricp.connections.idle");
        System.out.println("  GET /actuator/metrics/hikaricp.connections.pending   ← ALERT if > 0");
        System.out.println("  GET /actuator/metrics/hikaricp.connections.timeout   ← CRITICAL if > 0");
        System.out.println("  GET /actuator/metrics/hikaricp.connections.acquire   ← histogram");
        System.out.println();
        System.out.println("[DAY 37] SIZING ANTI-PATTERNS:");
        System.out.println("  ✗ maximumPoolSize=100 (too large, causes DB thrashing)");
        System.out.println("  ✗ minimumIdle=0 (cold start latency on every burst)");
        System.out.println("  ✓ maximumPoolSize=10 (for 4-core server + SSD DB)");
        System.out.println("  ✓ minimumIdle=2 (keep 2 warm, grow up to 10 under load)");
        assertThat(true).isTrue();
    }
}
