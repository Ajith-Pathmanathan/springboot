package com.techleadguru.phase1.day16;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 16 — Test: JDK Proxy vs CGLIB.
 *
 * Without a Spring context here, we verify class-level proxy detection logic
 * and document the rules.
 */
class Day16JdkProxyVsCglibTest {

    // -----------------------------------------------------------------------
    // Test 1: Non-proxied instances are NOT CGLIB subclasses
    // -----------------------------------------------------------------------
    @Test
    void direct_instantiation_is_not_a_proxy() {
        var stripe = new Day16JdkProxyVsCglib.StripeGateway();
        var email = new Day16JdkProxyVsCglib.EmailService();

        assertThat(stripe.isCglib()).isFalse();
        assertThat(email.isCglib()).isFalse();
        System.out.println("[DAY 16] Direct instances: no proxy ($$ absent in class name)");
    }

    // -----------------------------------------------------------------------
    // Test 2: final class cannot be subclassed (simulates CGLIB failure)
    // -----------------------------------------------------------------------
    @Test
    void final_class_cannot_be_subclassed_by_cglib() {
        // FinalService is final — attempting to extend it fails at compile time.
        // In Spring: BeanCreationException at startup.
        var finalService = new Day16JdkProxyVsCglib.FinalService();

        // No proxy — direct instantiation works fine
        assertThat(finalService.doWork()).isEqualTo("FinalService.doWork()");

        System.out.println("[DAY 16] FinalService works when instantiated directly.");
        System.out.println("[DAY 16] BUT Spring CGLIB would fail if you annotated it with @Service + @Transactional");
        System.out.println("[DAY 16] Error: 'Cannot subclass final class'");
    }

    // -----------------------------------------------------------------------
    // Test 3: Document proxy selection rules
    // -----------------------------------------------------------------------
    @Test
    void document_proxy_selection_rules() {
        System.out.println("[DAY 16] PROXY SELECTION RULES:");
        System.out.println("  JDK Dynamic Proxy:");
        System.out.println("    - Creates proxy implementing same INTERFACE.");
        System.out.println("    - Used when: target implements interface AND proxyTargetClass=false.");
        System.out.println("    - @Autowire by interface type only (concrete type fails cast).");
        System.out.println();
        System.out.println("  CGLIB Proxy (Spring Boot default since 2.0):");
        System.out.println("    - Creates SUBCLASS of target via bytecode.");
        System.out.println("    - Used when: proxyTargetClass=true OR no interface on target.");
        System.out.println("    - @Autowire by concrete type works.");
        System.out.println("    - CANNOT proxy: final class, final method.");
        System.out.println();
        System.out.println("  RULES:");
        System.out.println("  1. NEVER mark @Service/@Component class or @Transactional method as final.");
        System.out.println("  2. Kotlin: add 'open' or use kotlin-spring plugin (opens all Spring classes).");
        System.out.println("  3. proxyTargetClass=true is the Spring Boot default — CGLIB for everything.");
        assertThat(true).isTrue();
    }
}
