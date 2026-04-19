package com.techleadguru.phase1.day15;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.ArrayList;
import java.util.List;

/**
 * DAY 15 — AOP Basics: Join Points, Pointcuts, Advice, Aspect
 *
 * AOP VOCABULARY:
 *
 *   Join Point    — A specific moment in program execution where advice can be applied.
 *                   In Spring AOP: always a METHOD CALL on a Spring bean.
 *                   (No field access, no constructor join points in Spring AOP.)
 *
 *   Pointcut      — An EXPRESSION that matches zero or more join points.
 *                   "All methods on @Service classes" is a pointcut.
 *
 *   Advice        — The CODE that runs at a matched join point.
 *                   Types: @Before, @After, @AfterReturning, @AfterThrowing, @Around.
 *
 *   Aspect        — A class that COMBINES pointcuts + advice. Annotated with @Aspect.
 *
 *   Weaving       — The process of applying aspects to target objects.
 *                   Spring AOP: weaves at RUNTIME via proxy creation.
 *                   AspectJ: weaves at compile-time or load-time (more powerful).
 *
 * FIVE ADVICE TYPES:
 *   @Before         — Runs before the method. Cannot stop execution (throw to abort).
 *   @AfterReturning — Runs after normal return. Has access to return value.
 *   @AfterThrowing  — Runs if method throws exception. Has access to exception.
 *   @After          — Runs in all cases (finally semantics). Like try/finally.
 *   @Around         — Wraps the method. Must call ProceedingJoinPoint.proceed().
 *                     Most powerful: can modify args, return value, swallow exceptions.
 *
 * PRODUCTION SCENARIO — Execution time monitoring:
 *   Tech lead needs method-level timing on all @Service methods.
 *   Adding System.currentTimeMillis() to 200 service methods is not scalable.
 *   FIX: One @Around aspect that logs execution time for all @Service methods.
 *   Zero change to service code. Timing added/removed by toggling the aspect.
 */
@Slf4j
public class Day15AopBasics {

    // ===================================================================================
    // Target service (knows NOTHING about AOP)
    // ===================================================================================

    public static class OrderService {

        public String createOrder(String userId, double amount) {
            log.info("[Day15] Creating order for {} amount={}", userId, amount);
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
            return "Order[" + userId + ":" + amount + "]";
        }

        public List<String> listOrders(String userId) {
            log.info("[Day15] Listing orders for {}", userId);
            return List.of("Order1", "Order2");
        }
    }

    // ===================================================================================
    // Audit log — populated by advice
    // ===================================================================================

    public static class AuditLog {
        private final List<String> entries = new ArrayList<>();

        public void add(String entry) { entries.add(entry); }
        public List<String> getEntries() { return entries; }
    }

    // ===================================================================================
    // THE ASPECT: five advice types on OrderService methods
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class OrderAuditAspect {

        private final AuditLog auditLog;

        public OrderAuditAspect(AuditLog auditLog) {
            this.auditLog = auditLog;
        }

        // Pointcut: all methods in OrderService
        @Pointcut("execution(* com.techleadguru.phase1.day15.Day15AopBasics.OrderService.*(..))")
        public void orderServiceMethods() {}

        // @Before runs BEFORE the method
        @Before("orderServiceMethods()")
        public void beforeAdvice(JoinPoint jp) {
            String msg = "[BEFORE] " + jp.getSignature().getName() + " called";
            log.debug(msg);
            auditLog.add(msg);
        }

        // @AfterReturning runs after NORMAL return — has access to return value
        @AfterReturning(pointcut = "orderServiceMethods()", returning = "result")
        public void afterReturningAdvice(JoinPoint jp, Object result) {
            String msg = "[AFTER_RETURNING] " + jp.getSignature().getName() + " returned: " + result;
            log.debug(msg);
            auditLog.add(msg);
        }

        // @AfterThrowing runs when method THROWS — has access to the exception
        @AfterThrowing(pointcut = "orderServiceMethods()", throwing = "ex")
        public void afterThrowingAdvice(JoinPoint jp, Exception ex) {
            String msg = "[AFTER_THROWING] " + jp.getSignature().getName() + " threw: " + ex.getMessage();
            log.warn(msg);
            auditLog.add(msg);
        }

        // @After always runs (like finally) — no access to return value or exception
        @After("orderServiceMethods()")
        public void afterAdvice(JoinPoint jp) {
            String msg = "[AFTER] " + jp.getSignature().getName() + " completed (finally)";
            log.debug(msg);
            auditLog.add(msg);
        }
    }

    // ===================================================================================
    // @Around — most powerful, used for timing, retry, circuit breaker
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class ExecutionTimingAspect {

        private final AuditLog auditLog;

        public ExecutionTimingAspect(AuditLog auditLog) {
            this.auditLog = auditLog;
        }

        @Around("execution(* com.techleadguru.phase1.day15.Day15AopBasics.OrderService.*(..))")
        public Object timeExecution(ProceedingJoinPoint pjp) throws Throwable {
            long start = System.nanoTime();
            try {
                Object result = pjp.proceed(); // MUST call proceed() to execute the real method
                long elapsed = System.nanoTime() - start;
                String msg = "[AROUND] " + pjp.getSignature().getName() +
                        " took " + elapsed / 1_000 + "µs";
                log.debug(msg);
                auditLog.add(msg);
                return result;
            } catch (Throwable t) {
                auditLog.add("[AROUND] " + pjp.getSignature().getName() + " FAILED: " + t.getMessage());
                throw t; // re-throw — do NOT swallow exceptions silently!
            }
        }
    }

    // ===================================================================================
    // Configuration
    // ===================================================================================

    @Configuration
    @EnableAspectJAutoProxy
    public static class Day15Config {

        @Bean
        public AuditLog auditLog() { return new AuditLog(); }

        @Bean
        public OrderService orderService() { return new OrderService(); }

        @Bean
        public OrderAuditAspect orderAuditAspect(AuditLog auditLog) {
            return new OrderAuditAspect(auditLog);
        }

        @Bean
        public ExecutionTimingAspect executionTimingAspect(AuditLog auditLog) {
            return new ExecutionTimingAspect(auditLog);
        }
    }
}
