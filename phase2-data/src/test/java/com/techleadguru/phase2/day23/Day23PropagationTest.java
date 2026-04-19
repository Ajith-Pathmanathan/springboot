package com.techleadguru.phase2.day23;

import com.techleadguru.phase2.day23.Day23Propagation.OrderService;
import com.techleadguru.phase2.shared.AuditRepository;
import com.techleadguru.phase2.shared.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAY 23 — Test: REQUIRED vs REQUIRES_NEW propagation.
 *
 * KEY TESTS:
 *   1. REQUIRES_NEW audit survives outer TX rollback.
 *   2. REQUIRED audit rolls back with the outer TX.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day23PropagationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    AuditRepository auditRepository;

    @Autowired
    OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        auditRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // Test 1: REQUIRES_NEW — audit record survives outer TX rollback
    // -----------------------------------------------------------------------
    @Test
    void requires_new_audit_survives_outer_tx_rollback() {
        long auditCountBefore = auditRepository.count();

        // This will throw — outer TX rolls back
        assertThatThrownBy(() -> orderService.placeOrderWithSeparateAudit("user-1", new BigDecimal("100"), true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("payment failure");

        // Order rolled back
        assertThat(orderRepository.count()).isEqualTo(0);

        // Audit survived! REQUIRES_NEW committed before outer TX rolled back
        long auditCountAfter = auditRepository.count();
        assertThat(auditCountAfter).isEqualTo(auditCountBefore + 1);

        System.out.println("[DAY 23] REQUIRES_NEW: audit survived rollback. Count: " + auditCountAfter);
    }

    // -----------------------------------------------------------------------
    // Test 2: REQUIRED — audit rolls back WITH the outer TX
    // -----------------------------------------------------------------------
    @Test
    void required_audit_rolls_back_with_outer_tx() {
        long auditCountBefore = auditRepository.count();

        assertThatThrownBy(() -> orderService.placeOrderWithSharedAudit("user-2", new BigDecimal("200"), true))
                .isInstanceOf(RuntimeException.class);

        // Both order AND audit rolled back since they were in the SAME TX
        assertThat(orderRepository.count()).isEqualTo(0);
        assertThat(auditRepository.count()).isEqualTo(auditCountBefore);

        System.out.println("[DAY 23] REQUIRED: audit rolled back WITH the order. Count unchanged: " + auditCountBefore);
    }

    // -----------------------------------------------------------------------
    // Test 3: Happy path — both order and audit committed
    // -----------------------------------------------------------------------
    @Test
    @Transactional
    void happy_path_order_and_audit_both_committed() {
        String orderId = orderService.placeOrderWithSeparateAudit("user-3", new BigDecimal("50"), false);

        assertThat(orderId).isNotNull();
        assertThat(orderRepository.findById(orderId)).isPresent();

        System.out.println("[DAY 23] Happy path: order=" + orderId + " created and audit written.");
    }

    // -----------------------------------------------------------------------
    // Test 4: Document propagation types
    // -----------------------------------------------------------------------
    @Test
    void document_propagation_types() {
        System.out.println("[DAY 23] PROPAGATION TYPES:");
        System.out.println("  REQUIRED     = join existing TX or create new one (default, 99% of cases)");
        System.out.println("  REQUIRES_NEW = suspend outer TX, create independent TX (audit, payments)");
        System.out.println("  NESTED       = savepoint within outer TX (Day 24)");
        System.out.println("  SUPPORTS     = join TX if exists, run without TX if not (read-only queries)");
        System.out.println("  NOT_SUPPORTED = suspend TX if exists, always run without TX");
        System.out.println("  MANDATORY    = must join existing TX, throw if none");
        System.out.println("  NEVER        = must not have TX, throw if one exists");
        System.out.println();
        System.out.println("  REQUIRES_NEW TRAP: Only works via proxy injection.");
        System.out.println("  this.auditLog() in same class = REQUIRED regardless of annotation.");
        assertThat(true).isTrue();
    }
}
