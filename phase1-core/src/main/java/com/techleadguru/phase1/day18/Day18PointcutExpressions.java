package com.techleadguru.phase1.day18;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

/**
 * DAY 18 — Pointcut Expressions: execution, within, @annotation
 *
 * THREE MOST IMPORTANT POINTCUT DESIGNATORS:
 *
 *   execution(modifiers? returnType declaringType? method(params) throws?)
 *     - Matches specific method SIGNATURES.
 *     - Most common and precise pointcut.
 *     - Examples:
 *         execution(* *(..))                        — all methods
 *         execution(public String *(..))             — all public String-returning methods
 *         execution(* com.techleadguru..*Service.*(..))  — all methods in *Service classes
 *         execution(* *.save(..) throws IOException) — save() that declares IOException
 *
 *   within(TypePattern)
 *     - Matches ALL methods within matching TYPES (classes/packages).
 *     - Simpler than execution when you don't care about signatures.
 *     - Examples:
 *         within(com.techleadguru.phase1.day18.*)       — all classes in package
 *         within(@org.springframework.stereotype.Service *) — all @Service classes
 *
 *   @annotation(AnnotationType)
 *     - Matches methods ANNOTATED with a specific annotation.
 *     - Cleaner than execution when you want to mark specific methods.
 *     - Examples:
 *         @annotation(com.techleadguru.phase1.day18.Audited)
 *         @annotation(org.springframework.transaction.annotation.Transactional)
 *
 * COMBINING POINTCUTS:
 *   &&  — both must match: execution(*..Service.*(..)) && @annotation(Audited)
 *   ||  — either matches: within(..service..*) || within(..controller..*)
 *   !   — NOT: !@annotation(NoAudit)
 *
 * PRODUCTION SCENARIO — Audit log on 200 methods scattered across 30 classes:
 *   Business requirement: audit every state-changing operation.
 *   Adding explicit audit calls to each method = 200 changes, 200 merge conflicts.
 *   FIX: @Audited annotation. One @Before aspect. Zero method-level changes.
 *   To add a new audited method: just add @Audited. No aspect changes needed.
 */
@Slf4j
public class Day18PointcutExpressions {

    // ===================================================================================
    // Custom marker annotation
    // ===================================================================================

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Audited {
        String action() default "";
    }

    // ===================================================================================
    // Service with mixed methods — only some should be audited
    // ===================================================================================

    @Service
    @Slf4j
    public static class AccountService {

        @Audited(action = "TRANSFER")
        public String transfer(String fromAccount, String toAccount, double amount) {
            log.info("[Day18] Transfer {} from {} to {}", amount, fromAccount, toAccount);
            if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
            return "TRANSFERRED:" + amount;
        }

        @Audited(action = "CLOSE_ACCOUNT")
        public void closeAccount(String accountId) {
            log.info("[Day18] Close account: {}", accountId);
        }

        public String getBalance(String accountId) {
            // NOT audited — read operations don't need audit
            return "BALANCE:1000.00";
        }
    }

    // ===================================================================================
    // Aspect 1: execution() — all public methods in service package
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class ExecutionPointcutAspect {
        private final List<String> intercepted = new ArrayList<>();

        // Matches ALL methods in any class in day18 package
        @Pointcut("execution(* com.techleadguru.phase1.day18.Day18PointcutExpressions.AccountService.*(..))")
        public void allAccountServiceMethods() {}

        @Before("allAccountServiceMethods()")
        public void beforeMethod(JoinPoint jp) {
            String msg = "[execution] " + jp.getSignature().getName();
            log.debug(msg);
            intercepted.add(msg);
        }

        public List<String> getIntercepted() { return intercepted; }
    }

    // ===================================================================================
    // Aspect 2: @annotation() — only @Audited methods
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class AnnotationPointcutAspect {
        private final List<String> auditedCalls = new ArrayList<>();

        // Only methods annotated with @Audited
        @Pointcut("@annotation(audited)")
        public void auditedMethods(Audited audited) {}

        @Before("auditedMethods(audited)")
        public void auditBefore(JoinPoint jp, Audited audited) {
            String msg = "[audit] ACTION=" + audited.action() + " METHOD=" + jp.getSignature().getName();
            log.info(msg);
            auditedCalls.add(msg);
        }

        public List<String> getAuditedCalls() { return auditedCalls; }
    }

    // ===================================================================================
    // Aspect 3: within() — all methods in @Service-annotated classes
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class WithinPointcutAspect {
        private final List<String> withinCalls = new ArrayList<>();

        @Pointcut("within(@org.springframework.stereotype.Service *)")
        public void withinServiceClasses() {}

        @Before("withinServiceClasses()")
        public void beforeServiceMethod(JoinPoint jp) {
            String msg = "[within @Service] " + jp.getSignature().getDeclaringTypeName()
                    + "." + jp.getSignature().getName();
            log.debug(msg);
            withinCalls.add(msg);
        }

        public List<String> getWithinCalls() { return withinCalls; }
    }
}
