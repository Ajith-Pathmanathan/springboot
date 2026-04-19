package com.techleadguru.phase2.day24;

import com.techleadguru.phase2.day24.Day24NestedPropagation.BatchImportService;
import com.techleadguru.phase2.day24.Day24NestedPropagation.BatchLine;
import com.techleadguru.phase2.day24.Day24NestedPropagation.BatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 24 — Test: NESTED propagation with partial batch recovery.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day24NestedPropagationTest {

    @Autowired
    BatchImportService batchImportService;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM order_items");
        jdbc.update("DELETE FROM orders");
    }

    // -----------------------------------------------------------------------
    // Test 1: Valid lines commit, invalid lines are skipped via savepoint
    // -----------------------------------------------------------------------
    @Test
    void nested_tx_allows_partial_batch_success() {
        var lines = List.of(
                new BatchLine("user-A", new BigDecimal("10"), false),
                new BatchLine("user-B", new BigDecimal("20"), true),  // will fail
                new BatchLine("user-C", new BigDecimal("30"), false),
                new BatchLine("user-D", new BigDecimal("40"), true),  // will fail
                new BatchLine("user-E", new BigDecimal("50"), false)
        );

        BatchResult result = batchImportService.importBatch(lines);

        assertThat(result.success()).isEqualTo(3);
        assertThat(result.failure()).isEqualTo(2);
        assertThat(batchImportService.countOrders()).isEqualTo(3);

        System.out.println("[DAY 24] Batch result: " + result.success() + " success, " + result.failure() + " failed");
        System.out.println("[DAY 24] NESTED: failed lines rolled back to savepoint; valid lines still committed");
    }

    // -----------------------------------------------------------------------
    // Test 2: All valid — 100% success
    // -----------------------------------------------------------------------
    @Test
    void all_valid_lines_all_committed() {
        var lines = List.of(
                new BatchLine("user-X", new BigDecimal("100"), false),
                new BatchLine("user-Y", new BigDecimal("200"), false)
        );

        BatchResult result = batchImportService.importBatch(lines);

        assertThat(result.success()).isEqualTo(2);
        assertThat(result.failure()).isEqualTo(0);
        assertThat(batchImportService.countOrders()).isEqualTo(2);
        System.out.println("[DAY 24] All 2 lines committed successfully");
    }

    // -----------------------------------------------------------------------
    // Test 3: Document NESTED vs REQUIRES_NEW
    // -----------------------------------------------------------------------
    @Test
    void document_nested_vs_requires_new() {
        System.out.println("[DAY 24] NESTED vs REQUIRES_NEW:");
        System.out.println();
        System.out.println("  NESTED:");
        System.out.println("    - Same connection, same TX, uses JDBC savepoint.");
        System.out.println("    - Inner failure → rolls back to savepoint (outer TX unaffected).");
        System.out.println("    - Outer failure → ALL data lost (nested included).");
        System.out.println("    - Use for: partial batch recovery.");
        System.out.println();
        System.out.println("  REQUIRES_NEW:");
        System.out.println("    - New connection, new independent TX.");
        System.out.println("    - Outer failure → inner already committed. Survives.");
        System.out.println("    - More resource-intensive (2 connections at same time).");
        System.out.println("    - Use for: audit log, payment records that must survive.");
        System.out.println();
        System.out.println("  NESTED with JPA (Spring 6 NOTE):");
        System.out.println("    - JpaTransactionManager does NOT support NESTED.");
        System.out.println("    - Use DataSourceTransactionManager + JdbcTemplate for NESTED.");
        assertThat(true).isTrue();
    }
}
