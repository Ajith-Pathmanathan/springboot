package com.techleadguru.phase1.day20;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAY 20 — Test: exposeProxy=true and AopContext rules.
 */
class Day20ExposeProxyTest {

    // -----------------------------------------------------------------------
    // Test 1: AopContext.currentProxy() throws outside proxied call (correct behaviour)
    // -----------------------------------------------------------------------
    @Test
    void aop_context_throws_outside_proxied_call_when_not_in_proxy_context() {
        // When called outside a Spring-proxied method, AopContext.currentProxy()
        // throws IllegalStateException — this is CORRECT behaviour.
        assertThatThrownBy(() -> org.springframework.aop.framework.AopContext.currentProxy())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("proxy");

        System.out.println("[DAY 20] AopContext.currentProxy() correctly throws outside proxy context");
        System.out.println("[DAY 20] This means: exposeProxy is only useful INSIDE @Around / @Transactional / etc.");
    }

    // -----------------------------------------------------------------------
    // Test 2: ReportService executes both methods without Spring context (logic test)
    // -----------------------------------------------------------------------
    @Test
    void report_service_archives_report_when_called_directly() {
        var service = new Day20ExposeProxy.ReportService();

        service.generateAndArchive("REPORT-2025-Q4");

        assertThat(service.getTxLog()).hasSize(2);
        assertThat(service.getTxLog().get(0)).startsWith("OUTER_TX:");
        assertThat(service.getTxLog().get(1)).startsWith("ARCHIVE_TX:");
        System.out.println("[DAY 20] TX log: " + service.getTxLog());
        System.out.println("[DAY 20] Without Spring proxy: both calls use SAME TX (self-invocation trap shows here)");
    }

    // -----------------------------------------------------------------------
    // Test 3: Document exposeProxy and AspectJ comparison
    // -----------------------------------------------------------------------
    @Test
    void document_expose_proxy_and_aspectj_rules() {
        System.out.println("[DAY 20] exposeProxy=true RULES:");
        System.out.println("  @EnableAspectJAutoProxy(exposeProxy=true)");
        System.out.println("  → Stores the current proxy in a ThreadLocal at start of proxied call.");
        System.out.println("  → AopContext.currentProxy() retrieves it.");
        System.out.println("  → Use: ((MyService) AopContext.currentProxy()).annotatedMethod()");
        System.out.println("  → Cost: tiny ThreadLocal overhead per proxied call.");
        System.out.println();
        System.out.println("  SPRING AOP vs ASPECTJ:");
        System.out.println("  Spring AOP   → proxy-based, Spring beans only, method join points only.");
        System.out.println("               → Cannot intercept: private, static, final, self-invocation.");
        System.out.println("               → Zero setup. 95% of needs covered.");
        System.out.println("  AspectJ CTW  → compile-time weaving via aspectj compiler plugin.");
        System.out.println("               → Can intercept: private, static, self-invocation.");
        System.out.println("               → Requires build plugin. Overkill for most projects.");
        System.out.println("  AspectJ LTW  → load-time weaving via JVM agent (-javaagent).");
        System.out.println("               → Works on non-Spring third-party classes.");
        System.out.println("               → Use for truly intrusive legacy code only.");
        System.out.println();
        System.out.println("  DECISION TREE:");
        System.out.println("  Self-invocation issue? → FIX 1: Extract method to separate @Component (best).");
        System.out.println("                         → FIX 2: AopContext.currentProxy() (still Spring AOP).");
        System.out.println("                         → FIX 3: AspectJ LTW (only for unfixable legacy).");
        assertThat(true).isTrue();
    }
}
