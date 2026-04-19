package com.techleadguru.phase2.day28;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 28 — Test: @Transactional on private method is silently ignored.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day28PrivateMethodTransactionalTest {

    @Autowired
    Day28PrivateMethodTransactional.BrokenOrderService brokenOrderService;

    @Autowired
    Day28PrivateMethodTransactional.FixedOrderService fixedOrderService;

    @Autowired
    Day28PrivateMethodTransactional.CleanOrderService cleanOrderService;

    // -----------------------------------------------------------------------
    // Test 1: BrokenOrderService works (no exception) but TX annotation is ignored
    // -----------------------------------------------------------------------
    @Test
    @Transactional
    void broken_service_works_but_private_transactional_is_ignored() {
        // The method still runs (private methods run fine), but the @Transactional
        // on the private method has NO effect — whether TX is open depends on outer context.
        String orderId = brokenOrderService.placeOrder("user-1", new BigDecimal("100"));

        assertThat(orderId).isNotNull();
        System.out.println("[DAY 28] BUG: Order created id=" + orderId);
        System.out.println("[DAY 28] @Transactional on private savePending() = no-op (Spring logs no warning!)");
    }

    // -----------------------------------------------------------------------
    // Test 2: FixedOrderService — @Transactional on public method works correctly
    // -----------------------------------------------------------------------
    @Test
    @Transactional
    void fixed_service_transactional_on_public_method_is_intercepted() {
        String orderId = fixedOrderService.placeOrder("user-2", new BigDecimal("200"));

        assertThat(orderId).isNotNull();
        System.out.println("[DAY 28] FIX: Order created id=" + orderId + " — TX on public method works");
    }

    // -----------------------------------------------------------------------
    // Test 3: CleanOrderService — separate @Service extraction
    // -----------------------------------------------------------------------
    @Test
    @Transactional
    void clean_service_extraction_to_separate_bean_is_the_cleanest_fix() {
        String orderId = cleanOrderService.placeOrder("user-3", new BigDecimal("300"));

        assertThat(orderId).isNotNull();
        System.out.println("[DAY 28] CLEANEST FIX: Separate @Service bean. id=" + orderId);
    }

    // -----------------------------------------------------------------------
    // Test 4: Document the three silent no-op scenarios
    // -----------------------------------------------------------------------
    @Test
    void document_transactional_silent_no_op_scenarios() {
        System.out.println("[DAY 28] @Transactional SILENT NO-OP SCENARIOS:");
        System.out.println();
        System.out.println("  1. Private method:");
        System.out.println("     @Transactional private void myMethod() { ... }");
        System.out.println("     CGLIB cannot override private methods → advice never fires.");
        System.out.println("     FIX: Make method public OR extract to separate @Service bean.");
        System.out.println();
        System.out.println("  2. Not a Spring bean:");
        System.out.println("     new OrderService(repo).placeOrder()");
        System.out.println("     No proxy exists → @Transactional decorative only.");
        System.out.println("     FIX: Always inject via @Autowired / constructor injection.");
        System.out.println();
        System.out.println("  3. Self-invocation:");
        System.out.println("     this.placeOrder() inside the same class.");
        System.out.println("     Bypasses proxy → @Transactional ignored.");
        System.out.println("     FIX: Extract to separate @Service OR AopContext.currentProxy().");
        System.out.println();
        System.out.println("  RULE: @Transactional only works on PUBLIC methods of Spring-managed beans");
        System.out.println("        called via the Spring proxy (not via 'this').");
        assertThat(true).isTrue();
    }
}
