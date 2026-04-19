package com.techleadguru.phase2.day24;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DAY 24 — Propagation: NESTED and Savepoints
 *
 * NESTED:
 *   - Executes within a SAVEPOINT inside the existing transaction.
 *   - Savepoint: a DB-level bookmark. If the nested TX rolls back, it rolls back ONLY to the savepoint.
 *   - The OUTER TX is NOT rolled back — it can still commit.
 *   - If the outer TX rolls back → EVERYTHING rolls back (nested included).
 *
 * vs REQUIRES_NEW:
 *   REQUIRES_NEW → NEW connection, NEW TX, truly independent. Outer TX suspended.
 *   NESTED       → SAME connection, SAME TX, but uses savepoint. Outer TX participates.
 *
 * DATABASE SUPPORT:
 *   - H2, PostgreSQL, MySQL: NESTED works via JDBC savepoints.
 *   - Oracle: works.
 *
 * TRANSACTION MANAGER REQUIREMENT FOR NESTED:
 *   JpaTransactionManager does NOT support NESTED propagation in Spring 6.
 *   Reason: JpaDialect.beginTransaction() must return a SavepointManager — neither
 *   DefaultJpaDialect nor HibernateJpaDialect implement this interface in Spring 6.
 *   SOLUTION: Use DataSourceTransactionManager + JdbcTemplate for NESTED operations.
 *   DataSourceTransactionManager works at raw JDBC level and fully supports savepoints.
 *   See: TransactionConfig.dataSourceTransactionManager()
 *
 * PRODUCTION SCENARIO — Batch import with partial recovery:
 *   Importing 10,000 product records. Each line processed in NESTED TX.
 *   Line 7,432 has a validation error.
 *   NESTED TX for that line rolls back to savepoint — only that line skipped.
 *   Outer TX continues, commits 9,999 valid records.
 *   With REQUIRED: one bad record rolls back the entire batch of 10,000.
 *   With REQUIRES_NEW: each record is an independent TX — no batch atomicity.
 *   NESTED is the right choice for this use case.
 *
 * IMPORTANT CAVEAT:
 *   NESTED is NOT the same as REQUIRES_NEW.
 *   If the OUTER TX's connection fails → nested savepoint data is lost too.
 *   Use REQUIRES_NEW when true independence is required (audit, payments).
 *   Use NESTED when partial-failure recovery within a batch is needed.
 */
@Slf4j
public class Day24NestedPropagation {

    // ===================================================================================
    // Uses JdbcTemplate + DataSourceTransactionManager for NESTED savepoint support.
    // JpaTransactionManager does NOT support NESTED in Spring 6 — see TransactionConfig.
    // ===================================================================================

    // ===================================================================================
    // Inner service with NESTED propagation
    // ===================================================================================

    @Service
    @Slf4j
    public static class LineItemProcessor {

        private final JdbcTemplate jdbc;

        public LineItemProcessor(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        /**
         * NESTED: runs in a savepoint within the outer TX.
         * If this throws, only this savepoint rolls back — not the outer TX.
         *
         * Uses "dataSourceTransactionManager" — required because JpaTransactionManager
         * does NOT support NESTED propagation in Spring 6 (JpaDialect has no SavepointManager).
         * JdbcTemplate participates correctly with DataSourceTransactionManager via
         * the thread-bound Connection obtained from DataSourceUtils.
         */
        @Transactional(propagation = Propagation.NESTED, transactionManager = "dataSourceTransactionManager")
        public String processLine(String userId, BigDecimal amount, boolean failThis) {
            String id = UUID.randomUUID().toString();
            jdbc.update(
                    "INSERT INTO orders (id, user_id, total, status, version) VALUES (?, ?, ?, ?, ?)",
                    id, userId, amount, "PROCESSED", 0L
            );
            log.info("[Day24] Processed line: orderId={}, userId={}", id, userId);
            if (failThis) {
                throw new RuntimeException("Line processing failed — nested rollback to savepoint");
            }
            return id;
        }
    }

    // ===================================================================================
    // Batch service using NESTED for partial recovery
    // ===================================================================================

    @Service
    @Slf4j
    public static class BatchImportService {

        private final LineItemProcessor lineItemProcessor;
        private final JdbcTemplate jdbc;

        public BatchImportService(LineItemProcessor lineItemProcessor, JdbcTemplate jdbc) {
            this.lineItemProcessor = lineItemProcessor;
            this.jdbc = jdbc;
        }

        /**
         * Processes each line in a NESTED TX.
         * Failed lines are skipped (savepoint rolled back) but the batch continues.
         * At the end, only valid lines are committed.
         *
         * Uses "dataSourceTransactionManager" — required for NESTED savepoint support.
         */
        @Transactional(transactionManager = "dataSourceTransactionManager")
        public BatchResult importBatch(java.util.List<BatchLine> lines) {
            int success = 0, failure = 0;

            for (BatchLine line : lines) {
                try {
                    lineItemProcessor.processLine(line.userId(), line.amount(), line.invalid());
                    success++;
                } catch (RuntimeException e) {
                    // NESTED rolled back to savepoint — we can continue processing
                    log.warn("[Day24] Line skipped (savepoint rolled back): {} — {}", line.userId(), e.getMessage());
                    failure++;
                }
            }

            return new BatchResult(success, failure);
        }

        public long countOrders() {
            Long count = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
            return count != null ? count : 0;
        }
    }

    public record BatchLine(String userId, BigDecimal amount, boolean invalid) {}
    public record BatchResult(int success, int failure) {}
}
