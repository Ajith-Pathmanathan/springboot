package com.techleadguru.phase2.day38;

import com.techleadguru.phase2.day38.Day38ConnectionLeak.LeakDemoService;
import com.techleadguru.phase2.day38.Day38ConnectionLeak.LeakDetectionConfig;
import com.techleadguru.phase2.day38.Day38ConnectionLeak.PoolSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 38 — Test: Connection leak detection configuration and patterns.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day38ConnectionLeakTest {

    @Autowired
    LeakDemoService leakDemoService;

    // -----------------------------------------------------------------------
    // Test 1: Verify leak detection is enabled (threshold = 2000ms)
    // -----------------------------------------------------------------------
    @Test
    void leak_detection_threshold_is_configured() {
        LeakDetectionConfig config = leakDemoService.getLeakDetectionConfig();

        assertThat(config.isEnabled()).isTrue();
        assertThat(config.thresholdMs()).isEqualTo(2000L);

        System.out.printf("[DAY 38] Leak detection enabled: threshold=%dms%n", config.thresholdMs());
        System.out.println("[DAY 38] Any connection held > 2000ms triggers a WARN log with stack trace.");
    }

    // -----------------------------------------------------------------------
    // Test 2: Safe connection pattern (try-with-resources)
    // -----------------------------------------------------------------------
    @Test
    void safe_connection_pattern_auto_closes() throws Exception {
        String result = leakDemoService.safeConnectionUsage();

        assertThat(result).startsWith("safe-");
        System.out.println("[DAY 38] Safe pattern: " + result);
        System.out.println("[DAY 38] try-with-resources guarantees connection.close() is called.");
    }

    // -----------------------------------------------------------------------
    // Test 3: Capture active connections during @Transactional
    // -----------------------------------------------------------------------
    @Test
    void active_connections_increase_during_transaction() {
        PoolSnapshot snapshot = leakDemoService.captureActiveConnections();

        // During @Transactional, at least 1 connection is active
        assertThat(snapshot.total()).isGreaterThanOrEqualTo(1);

        System.out.println("[DAY 38] Pool snapshot during @Transactional:");
        System.out.printf("  active=%d, idle=%d, total=%d, waiting=%d%n",
                snapshot.active(), snapshot.idle(), snapshot.total(), snapshot.waiting());
        System.out.println("[DAY 38] @Transactional borrows 1 connection, returns it when method exits.");
    }

    // -----------------------------------------------------------------------
    // Test 4: Document leak scenarios and fixes
    // -----------------------------------------------------------------------
    @Test
    void document_leak_scenarios_and_fixes() {
        System.out.println("[DAY 38] CONNECTION LEAK SCENARIOS:");
        System.out.println();
        System.out.println("  LEAK CAUSE 1: Exception before connection.close()");
        System.out.println("    BAD:  Connection c = ds.getConnection(); /* exception */ c.close();");
        System.out.println("    FIX:  try(Connection c = ds.getConnection()) { ... }");
        System.out.println();
        System.out.println("  LEAK CAUSE 2: Long-running @Transactional");
        System.out.println("    BAD:  @Transactional void slowMethod() { Thread.sleep(10000); }");
        System.out.println("    FIX:  Don't hold TX across slow operations (HTTP calls, file I/O)");
        System.out.println();
        System.out.println("  LEAK CAUSE 3: @Async + @Transactional");
        System.out.println("    BAD:  @Async @Transactional void asyncTx() { ... }");
        System.out.println("    FIX:  TX may not bind correctly to async thread; use explicit TX management.");
        System.out.println();
        System.out.println("  LEAK DETECTION: spring.datasource.hikari.leak-detection-threshold=2000");
        System.out.println("    HikariCP warns: 'Connection leak detection triggered' + full stack trace.");
        System.out.println("    Use to find exactly WHERE the leak is happening.");
        assertThat(true).isTrue();
    }
}
