package com.techleadguru.phase1.day18;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAY 18 — Test: Pointcut expression types.
 */
class Day18PointcutExpressionsTest {

    // -----------------------------------------------------------------------
    // Test 1: AccountService — @Audited methods execute correctly
    // -----------------------------------------------------------------------
    @Test
    void audited_methods_execute_correctly() {
        var service = new Day18PointcutExpressions.AccountService();

        String result = service.transfer("ACC-001", "ACC-002", 500.0);
        assertThat(result).isEqualTo("TRANSFERRED:500.0");
        System.out.println("[DAY 18] @Audited transfer() = " + result);
    }

    // -----------------------------------------------------------------------
    // Test 2: Non-@Audited getBalance still works normally
    // -----------------------------------------------------------------------
    @Test
    void non_audited_method_works_without_audit_interception() {
        var service = new Day18PointcutExpressions.AccountService();

        String balance = service.getBalance("ACC-001");
        assertThat(balance).isEqualTo("BALANCE:1000.00");
        System.out.println("[DAY 18] Non-@Audited getBalance() = " + balance);
    }

    // -----------------------------------------------------------------------
    // Test 3: Exception propagates correctly even with @Audited
    // -----------------------------------------------------------------------
    @Test
    void audited_method_propagates_exception() {
        var service = new Day18PointcutExpressions.AccountService();

        assertThatThrownBy(() -> service.transfer("ACC-001", "ACC-002", -1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");
    }

    // -----------------------------------------------------------------------
    // Test 4: Document pointcut expression syntax
    // -----------------------------------------------------------------------
    @Test
    void document_pointcut_expression_syntax() {
        System.out.println("[DAY 18] POINTCUT EXPRESSION CHEAT SHEET:");
        System.out.println();
        System.out.println("  execution(* com.example..service.*.*(..))");
        System.out.println("    ↑ all methods in any class matching *Service in service package (any depth)");
        System.out.println();
        System.out.println("  execution(public * *(..))");
        System.out.println("    ↑ all public methods anywhere");
        System.out.println();
        System.out.println("  within(com.example.service..*)");
        System.out.println("    ↑ all methods in service package and sub-packages");
        System.out.println();
        System.out.println("  @annotation(com.example.Audited)");
        System.out.println("    ↑ all methods annotated with @Audited");
        System.out.println();
        System.out.println("  @within(org.springframework.stereotype.Service)");
        System.out.println("    ↑ all methods in classes annotated with @Service");
        System.out.println();
        System.out.println("  Combining:");
        System.out.println("  execution(*.service.*.*(..)) && @annotation(Audited)");
        System.out.println("    ↑ only @Audited methods in service classes");
        System.out.println();
        System.out.println("  RULE: Prefer @annotation() pointcuts for explicit control.");
        System.out.println("        Use execution() or within() for blanket policies.");
        assertThat(true).isTrue();
    }
}
