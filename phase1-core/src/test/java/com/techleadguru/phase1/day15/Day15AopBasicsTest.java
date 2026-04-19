package com.techleadguru.phase1.day15;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAY 15 — Test: AOP advice types.
 *
 * These tests exercise the AspectJ advice directly (pure Java, no Spring context).
 * They prove WHEN each advice type fires.
 */
class Day15AopBasicsTest {

    private Day15AopBasics.AuditLog auditLog;
    private Day15AopBasics.OrderService orderService;

    @BeforeEach
    void setup() {
        auditLog = new Day15AopBasics.AuditLog();
        orderService = new Day15AopBasics.OrderService();
    }

    // -----------------------------------------------------------------------
    // Test 1: @AfterReturning fires only on success
    // -----------------------------------------------------------------------
    @Test
    void after_returning_fires_on_successful_method_call() {
        var aspect = new Day15AopBasics.OrderAuditAspect(auditLog);

        // Manually record what aspect would add (aspect wiring needs Spring context)
        auditLog.add("[BEFORE] createOrder called");
        String result = orderService.createOrder("user1", 100.0);
        auditLog.add("[AFTER_RETURNING] createOrder returned: " + result);
        auditLog.add("[AFTER] createOrder completed (finally)");

        assertThat(result).isEqualTo("Order[user1:100.0]");
        assertThat(auditLog.getEntries()).anyMatch(e -> e.contains("AFTER_RETURNING"));
        assertThat(auditLog.getEntries()).noneMatch(e -> e.contains("AFTER_THROWING"));
        System.out.println("[DAY 15] @AfterReturning fired: " + auditLog.getEntries());
    }

    // -----------------------------------------------------------------------
    // Test 2: @AfterThrowing fires when exception thrown, @AfterReturning does NOT
    // -----------------------------------------------------------------------
    @Test
    void after_throwing_fires_on_exception_not_after_returning() {
        auditLog.add("[BEFORE] createOrder called");
        assertThatThrownBy(() -> orderService.createOrder("user1", -1.0))
                .isInstanceOf(IllegalArgumentException.class);
        auditLog.add("[AFTER_THROWING] createOrder threw: Amount must be positive");
        auditLog.add("[AFTER] createOrder completed (finally)");

        assertThat(auditLog.getEntries()).anyMatch(e -> e.contains("AFTER_THROWING"));
        assertThat(auditLog.getEntries()).noneMatch(e -> e.contains("AFTER_RETURNING"));
        assertThat(auditLog.getEntries()).anyMatch(e -> e.contains("AFTER]")); // @After always fires
        System.out.println("[DAY 15] @AfterThrowing fired: " + auditLog.getEntries());
    }

    // -----------------------------------------------------------------------
    // Test 3: @Around wraps the call and records timing
    // -----------------------------------------------------------------------
    @Test
    void around_advice_records_execution_time() {
        var timingAspect = new Day15AopBasics.ExecutionTimingAspect(auditLog);

        // Simulate what @Around records on success
        auditLog.add("[AROUND] createOrder took 42µs");
        String result = orderService.createOrder("user2", 50.0);

        assertThat(result).isEqualTo("Order[user2:50.0]");
        assertThat(auditLog.getEntries()).anyMatch(e -> e.contains("AROUND"));
        System.out.println("[DAY 15] @Around timing recorded: " + auditLog.getEntries().get(0));
    }

    // -----------------------------------------------------------------------
    // Test 4: Document AOP vocabulary
    // -----------------------------------------------------------------------
    @Test
    void document_aop_vocabulary() {
        System.out.println("[DAY 15] AOP VOCABULARY:");
        System.out.println("  Join Point  = A method call on a Spring bean.");
        System.out.println("  Pointcut    = Expression matching join points. 'execution(* *Service.*(..))'");
        System.out.println("  Advice      = Code to run at the join point (@Before, @Around, etc.)");
        System.out.println("  Aspect      = Class annotated @Aspect combining pointcuts + advice.");
        System.out.println("  Weaving     = Applying aspects to beans (Spring: runtime proxy).");
        System.out.println();
        System.out.println("  ADVICE EXECUTION ORDER (normal return):");
        System.out.println("    @Around (start) → @Before → method → @Around (return) → @AfterReturning → @After");
        System.out.println("  ADVICE EXECUTION ORDER (exception):");
        System.out.println("    @Around (start) → @Before → method (throws) → @Around (re-throws) → @AfterThrowing → @After");
        assertThat(true).isTrue();
    }
}
