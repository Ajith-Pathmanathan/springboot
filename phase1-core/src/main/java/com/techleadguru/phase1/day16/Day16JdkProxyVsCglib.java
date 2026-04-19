package com.techleadguru.phase1.day16;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * DAY 16 — JDK Proxy vs CGLIB: Why final Classes Break Spring AOP
 *
 * TWO PROXY STRATEGIES:
 *
 *   JDK Dynamic Proxy:
 *     - Works by creating a proxy class that IMPLEMENTS the same interface.
 *     - Target class MUST implement an interface.
 *     - Proxy is typed as the interface, not the concrete class.
 *     - @Autowired interface type → JDK proxy works.
 *     - @Autowired concrete class type → fails (can't cast proxy to concrete class).
 *
 *   CGLIB Proxy:
 *     - Works by creating a SUBCLASS of the target class (bytecode generation).
 *     - Does NOT require an interface.
 *     - @Autowired concrete class type → CGLIB proxy works.
 *     - LIMITATION: Cannot proxy a FINAL class (can't subclass it).
 *     - LIMITATION: Cannot proxy a FINAL method (can't override it).
 *     - LIMITATION: Cannot proxy classes without a no-arg constructor (Spring Boot: overcome by Objenesis).
 *
 * SPRING BOOT DEFAULT (since 2.0):
 *   @EnableAspectJAutoProxy(proxyTargetClass=true) — CGLIB is the default.
 *   If target class implements interface AND you autowire the interface → JDK proxy.
 *   If target class has no interface OR you @Autowire concrete class → CGLIB.
 *
 * THE FINAL CLASS BUG:
 *   @Transactional on a Kotlin data class (all Kotlin classes are final by default).
 *   Spring cannot create CGLIB subclass. At startup or first call:
 *   BeanCreationException: "Cannot subclass final class".
 *   FIX in Kotlin: add "open" to class and method, or use kotlin-spring compiler plugin.
 *   FIX in Java: never mark @Service/@Component classes or their @Transactional methods as final.
 *
 * PRODUCTION SCENARIO — Security library breaks @Transactional:
 *   Security library wraps UserService in a final SecurityWrapper that delegates to the real service.
 *   Spring Boot tries to CGLIB-proxy SecurityWrapper → fails, it's final.
 *   All @Transactional on UserService methods silently skipped.
 *   Data inconsistency in production. No test catches it (test uses @Transactional on test).
 *   FIX: Inject UserService directly via constructor. Make SecurityWrapper non-final.
 */
@Slf4j
public class Day16JdkProxyVsCglib {

    // ===================================================================================
    // Case 1: Interface-based — JDK proxy or CGLIB both work
    // ===================================================================================

    public interface PaymentGateway {
        String charge(String amount);
        boolean isCglib();
    }

    @Slf4j
    public static class StripeGateway implements PaymentGateway {

        @Override
        public String charge(String amount) {
            log.info("[Day16] STRIPE charge: {}", amount);
            return "STRIPE:" + amount;
        }

        @Override
        public boolean isCglib() {
            // If Spring created a CGLIB subclass, this instance's class name contains "$$"
            return this.getClass().getName().contains("$$");
        }
    }

    // ===================================================================================
    // Case 2: No interface — ONLY CGLIB works
    // ===================================================================================

    @Slf4j
    public static class EmailService {
        // No interface — Spring MUST use CGLIB to proxy this class

        public String sendEmail(String to, String subject) {
            log.info("[Day16] Email to {} subject: {}", to, subject);
            return "SENT:" + to;
        }

        public boolean isCglib() {
            return this.getClass().getName().contains("$$");
        }
    }

    // ===================================================================================
    // THE BROKEN CASE: final class breaks CGLIB
    // A final class is documented here but NOT wired into Spring to avoid startup failure.
    // ===================================================================================

    // If you put @Service here and @Transactional on the method, Spring will fail:
    // "Cannot subclass final class com.techleadguru.phase1.day16.Day16JdkProxyVsCglib$FinalService"
    public static final class FinalService {
        public String doWork() {
            return "FinalService.doWork()";
        }
    }

    // ===================================================================================
    // Aspect to prove proxy wrapping is in effect
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class ProxyDetectionAspect {

        @Around("execution(* com.techleadguru.phase1.day16.Day16JdkProxyVsCglib.StripeGateway.*(..)) " +
                "|| execution(* com.techleadguru.phase1.day16.Day16JdkProxyVsCglib.EmailService.*(..))")
        public Object logProxy(ProceedingJoinPoint pjp) throws Throwable {
            log.debug("[Day16] Proxy intercepted: {} on {}",
                    pjp.getSignature().getName(),
                    pjp.getTarget().getClass().getSimpleName());
            return pjp.proceed();
        }
    }

    // ===================================================================================
    // Configuration
    // ===================================================================================

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true) // force CGLIB even when interface exists
    public static class Day16Config {

        @Bean
        public StripeGateway stripeGateway() { return new StripeGateway(); }

        @Bean
        public EmailService emailService() { return new EmailService(); }

        @Bean
        public ProxyDetectionAspect proxyDetectionAspect() { return new ProxyDetectionAspect(); }
    }
}
