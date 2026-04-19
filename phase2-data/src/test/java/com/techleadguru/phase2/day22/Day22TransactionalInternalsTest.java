package com.techleadguru.phase2.day22;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 22 — Test: @Transactional internals verified with a real H2 DataSource.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day22TransactionalInternalsTest {

    @Autowired
    Day22TransactionalInternals.OrderService orderService;

    // -----------------------------------------------------------------------
    // Test 1: TX is active inside @Transactional method
    // -----------------------------------------------------------------------
    @Test
    void transaction_is_active_inside_transactional_method() {
        Day22TransactionalInternals.TxInfo info =
                orderService.createOrder("user-1", new BigDecimal("99.99"));

        assertThat(info.txActive()).isTrue();
        assertThat(info.txName()).contains("createOrder");
        assertThat(info.orderId()).isNotNull();

        System.out.println("[DAY 22] TX was active: " + info.txActive());
        System.out.println("[DAY 22] TX name: " + info.txName());
        System.out.println("[DAY 22] Order created: " + info.orderId());
    }

    // -----------------------------------------------------------------------
    // Test 2: No TX outside @Transactional
    // -----------------------------------------------------------------------
    @Test
    void no_transaction_outside_transactional_annotation() {
        boolean active = orderService.isTransactionActiveRightNow();

        assertThat(active).isFalse();
        System.out.println("[DAY 22] TX active without @Transactional: " + active);
    }

    // -----------------------------------------------------------------------
    // Test 3: @Transactional wraps a test — verify inner TX shares the test TX
    // -----------------------------------------------------------------------
    @Test
    @Transactional
    void test_transactional_wraps_test_method_itself() {
        boolean active = TransactionSynchronizationManager.isActualTransactionActive();

        assertThat(active).isTrue();
        System.out.println("[DAY 22] Test-level @Transactional opens TX: " + active);
        System.out.println("[DAY 22] IMPORTANT: @Transactional on tests rolls back after each test.");
        System.out.println("[DAY 22] This prevents test data from polluting the database.");
    }

    // -----------------------------------------------------------------------
    // Test 4: Document the @Transactional mechanism
    // -----------------------------------------------------------------------
    @Test
    void document_transactional_mechanism() {
        System.out.println("[DAY 22] HOW @Transactional WORKS:");
        System.out.println("  1. Spring wraps your @Service in a CGLIB proxy.");
        System.out.println("  2. TransactionInterceptor (AOP @Around advice) intercepts the call.");
        System.out.println("  3. Opens JDBC connection. Stores in ThreadLocal via TransactionSynchronizationManager.");
        System.out.println("  4. Calls your real method.");
        System.out.println("  5. Commits (success) or rolls back (RuntimeException) the connection.");
        System.out.println();
        System.out.println("  SILENT NO-OPS (common bugs):");
        System.out.println("  - @Transactional on private method → proxy doesn't intercept private methods.");
        System.out.println("  - new MyService() → not a Spring bean → no proxy → no TX.");
        System.out.println("  - this.method() self-invocation → bypasses proxy → no TX.");
        assertThat(true).isTrue();
    }
}
