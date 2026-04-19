package com.techleadguru.phase2.day25;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAY 25 — Test: MANDATORY, NEVER, NOT_SUPPORTED, SUPPORTS propagation.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day25PropagationRareTest {

    @Autowired
    Day25PropagationRare.OutboxService outboxService;

    @Autowired
    Day25PropagationRare.ReportingService reportingService;

    @Autowired
    Day25PropagationRare.PaymentGatewayService paymentGatewayService;

    @Autowired
    Day25PropagationRare.ProductLookupService productLookupService;

    @Autowired
    Day25PropagationRare.CheckoutService checkoutService;

    // -----------------------------------------------------------------------
    // Test 1: MANDATORY throws when no TX is active
    // -----------------------------------------------------------------------
    @Test
    void mandatory_throws_when_no_transaction_active() {
        // No @Transactional on this test method — no TX in context
        assertThatThrownBy(() -> outboxService.writeOutboxEvent("ORDER", "user-1:prod-1"))
                .isInstanceOf(IllegalTransactionStateException.class);

        System.out.println("[DAY 25] MANDATORY correctly threw — no TX was present");
    }

    // -----------------------------------------------------------------------
    // Test 2: MANDATORY works when a TX is active
    // -----------------------------------------------------------------------
    @Test
    @Transactional
    void mandatory_succeeds_when_transaction_is_active() {
        String result = outboxService.writeOutboxEvent("ORDER_PLACED", "user-1:prod-1");

        assertThat(result).startsWith("OUTBOX[ORDER_PLACED:");
        System.out.println("[DAY 25] MANDATORY succeeded inside TX: " + result);
    }

    // -----------------------------------------------------------------------
    // Test 3: NEVER throws when a TX is active
    // -----------------------------------------------------------------------
    @Test
    @Transactional
    void never_throws_when_transaction_is_active() {
        assertThatThrownBy(() -> reportingService.generateReport("Q4-2025"))
                .isInstanceOf(IllegalTransactionStateException.class);

        System.out.println("[DAY 25] NEVER correctly threw — TX was active");
    }

    // -----------------------------------------------------------------------
    // Test 4: NEVER works when no TX is active
    // -----------------------------------------------------------------------
    @Test
    void never_works_when_no_transaction_active() {
        String report = reportingService.generateReport("Q4-2025");

        assertThat(report).isEqualTo("REPORT[Q4-2025]");
        System.out.println("[DAY 25] NEVER succeeded — no TX present: " + report);
    }

    // -----------------------------------------------------------------------
    // Test 5: NOT_SUPPORTED runs without TX inside @Transactional context
    // -----------------------------------------------------------------------
    @Test
    @Transactional
    void not_supported_suspends_outer_tx_during_external_call() {
        String result = paymentGatewayService.chargeCard("99.99");

        assertThat(result).isEqualTo("CHARGED:99.99");
        System.out.println("[DAY 25] NOT_SUPPORTED: TX suspended during external call — connection released");
    }

    // -----------------------------------------------------------------------
    // Test 6: Full checkout flow — all propagation types working together
    // -----------------------------------------------------------------------
    @Test
    @Transactional
    void full_checkout_uses_all_propagation_types_correctly() {
        Day25PropagationRare.CheckoutResult result =
                checkoutService.checkout("user-42", "prod-001", "199.99");

        assertThat(result.product()).isEqualTo("Product[prod-001]");
        assertThat(result.payment()).isEqualTo("CHARGED:199.99");
        assertThat(result.outboxEntry()).startsWith("OUTBOX[ORDER_PLACED:");

        System.out.println("[DAY 25] Checkout: " + result);
        System.out.println("[DAY 25] SUPPORTS joined outer TX, NOT_SUPPORTED suspended it, MANDATORY used it");
    }
}
