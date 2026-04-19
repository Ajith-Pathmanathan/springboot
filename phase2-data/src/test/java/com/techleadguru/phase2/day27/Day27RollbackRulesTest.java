package com.techleadguru.phase2.day27;

import com.techleadguru.phase2.day27.Day27RollbackRules.BrokenPaymentService;
import com.techleadguru.phase2.day27.Day27RollbackRules.FixedPaymentService;
import com.techleadguru.phase2.day27.Day27RollbackRules.InventoryService;
import com.techleadguru.phase2.shared.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAY 27 — Test: Rollback rules for checked vs unchecked exceptions.
 *
 * CRITICAL TESTS — these prove the most commonly missed @Transactional rule.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day27RollbackRulesTest {

    @Autowired
    BrokenPaymentService brokenPaymentService;

    @Autowired
    FixedPaymentService fixedPaymentService;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    OrderRepository paymentRepository;

    @BeforeEach
    void cleanup() {
        paymentRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // Test 1: BUG — checked exception does NOT trigger rollback
    // -----------------------------------------------------------------------
    @Test
    void checked_exception_does_not_rollback_without_explicit_rollback_for() {
        // BrokenPaymentService: no rollbackFor — checked exception leaks past rollback
        assertThatThrownBy(() ->
                brokenPaymentService.processPayment("user-1", new BigDecimal("50"), true))
                .isInstanceOf(Day27RollbackRules.PaymentGatewayException.class);

        // BUG: record was commited despite the exception!
        long count = paymentRepository.count();
        assertThat(count).isEqualTo(1); // row committed — inconsistent state!

        System.out.println("[DAY 27] BUG: PaymentRecord count=" + count + " — row committed despite exception");
        System.out.println("[DAY 27] Checked exception without rollbackFor = partial commit = data corruption");
    }

    // -----------------------------------------------------------------------
    // Test 2: FIX — rollbackFor=Exception.class rolls back on checked exception
    // -----------------------------------------------------------------------
    @Test
    void rollback_for_exception_rolls_back_on_checked_exception() {
        assertThatThrownBy(() ->
                fixedPaymentService.processPayment("user-2", new BigDecimal("100"), true))
                .isInstanceOf(Day27RollbackRules.PaymentGatewayException.class);

        // FIX: record was rolled back
        long count = paymentRepository.count();
        assertThat(count).isEqualTo(0); // TX rolled back correctly

        System.out.println("[DAY 27] FIX: PaymentRecord count=" + count + " — rolled back correctly");
    }

    // -----------------------------------------------------------------------
    // Test 3: noRollbackFor — row committed even when exception thrown
    // -----------------------------------------------------------------------
    @Test
    void no_rollback_for_commits_despite_runtime_exception() {
        assertThatThrownBy(() ->
                inventoryService.reserveStock("user-3", 200)) // > 100 triggers exception
                .isInstanceOf(Day27RollbackRules.InsufficientStockException.class);

        // noRollbackFor = InsufficientStockException → row COMMITTED despite exception
        long count = paymentRepository.count();
        assertThat(count).isEqualTo(1);

        System.out.println("[DAY 27] noRollbackFor: audit row committed despite exception. Count=" + count);
    }

    // -----------------------------------------------------------------------
    // Test 4: Happy path — no exception, committed normally
    // -----------------------------------------------------------------------
    @Test
    void happy_path_commits_on_success() throws Day27RollbackRules.PaymentGatewayException {
        fixedPaymentService.processPayment("user-4", new BigDecimal("75"), false);

        assertThat(paymentRepository.count()).isEqualTo(1);
        System.out.println("[DAY 27] Happy path: payment committed successfully");
    }

    // -----------------------------------------------------------------------
    // Test 5: Document the rollback rules
    // -----------------------------------------------------------------------
    @Test
    void document_rollback_rules() {
        System.out.println("[DAY 27] @Transactional ROLLBACK RULES:");
        System.out.println("  DEFAULT: rolls back on RuntimeException and Error.");
        System.out.println("  DEFAULT: does NOT roll back on checked Exception.");
        System.out.println();
        System.out.println("  OVERRIDES:");
        System.out.println("  rollbackFor = IOException.class     → also rollback on IOException");
        System.out.println("  rollbackFor = Exception.class       → rollback on ALL exceptions");
        System.out.println("  noRollbackFor = BusinessException   → don't rollback on this runtime exception");
        System.out.println();
        System.out.println("  RULE: If your @Transactional method declares a checked exception in 'throws',");
        System.out.println("        you MUST add rollbackFor = Exception.class or the specific type.");
        assertThat(true).isTrue();
    }
}
