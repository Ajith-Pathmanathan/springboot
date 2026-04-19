package com.techleadguru.phase1.day17;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * DAY 17 — @Around Advice: The Silent Data Loss Bug
 *
 * @Around IS THE MOST POWERFUL AND MOST DANGEROUS ADVICE TYPE.
 *
 * POWER:
 *   - Controls whether the real method runs (must call pjp.proceed()).
 *   - Can modify input arguments before calling the real method.
 *   - Can modify or replace the return value after the call.
 *   - Can suppress exceptions and return a default value.
 *   - Can retry failed calls.
 *
 * THE SILENT DATA LOSS BUG:
 *   @Around advice forgets to return the value from pjp.proceed().
 *   Method executes, data is written to DB, but caller receives null/0/false.
 *   API returns 0 quantity, negative amounts, null IDs.
 *   No exception thrown. No log. Silent data corruption.
 *
 *   public Object loggingAdvice(ProceedingJoinPoint pjp) throws Throwable {
 *       log.info("calling...");
 *       pjp.proceed(); // ← BUG: return value DISCARDED
 *       return null;   // ← caller always gets null
 *   }
 *
 * THE SWALLOWED EXCEPTION BUG:
 *   @Around catches Throwable and doesn't re-throw.
 *   Service method throws ValidationException.
 *   Aspect catches it, logs it, returns null.
 *   Controller sees null — computes wrong result — data written with wrong state.
 *
 * CORRECT PATTERN:
 *   1. ALWAYS return the result of pjp.proceed().
 *   2. ALWAYS re-throw exceptions unless explicitly converting them.
 *   3. If modifying return value, keep original type compatibility.
 *
 * PRODUCTION SCENARIO:
 *   Team adds @Around logging aspect to all @Service methods before release.
 *   Forgot to return pjp.proceed() result. Deployed Friday.
 *   Saturday: all API responses return null data. Creates Orders return null orderId.
 *   Payments process but users see "Order not created". Support tickets flood in.
 *   Root cause found Monday. Zero test coverage on the aspect itself.
 */
@Slf4j
public class Day17AroundAdvice {

    // ===================================================================================
    // Target service
    // ===================================================================================

    public static class InventoryService {

        public int getStock(String productId) {
            log.info("[Day17] getStock({})", productId);
            return 42; // always 42 units in stock
        }

        public String reserveStock(String productId, int quantity) {
            log.info("[Day17] reserveStock({}, {})", productId, quantity);
            if (quantity > 100) throw new IllegalArgumentException("Cannot reserve more than 100");
            return "RESERVATION:" + productId + ":" + quantity;
        }
    }

    // ===================================================================================
    // THE BUG: @Around that discards the return value
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class BrokenLoggingAspect {

        @Pointcut("execution(* com.techleadguru.phase1.day17.Day17AroundAdvice.InventoryService.*(..))")
        public void inventoryMethods() {}

        @Around("inventoryMethods()")
        public Object log(ProceedingJoinPoint pjp) throws Throwable {
            log.info("[BROKEN] Before: {}", pjp.getSignature().getName());
            pjp.proceed(); // BUG: return value discarded!
            log.info("[BROKEN] After: {}", pjp.getSignature().getName());
            return null; // ← caller always gets null — silent data loss!
        }
    }

    // ===================================================================================
    // THE CORRECT @Around
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class CorrectLoggingAspect {

        @Pointcut("execution(* com.techleadguru.phase1.day17.Day17AroundAdvice.InventoryService.*(..))")
        public void inventoryMethods() {}

        @Around("inventoryMethods()")
        public Object log(ProceedingJoinPoint pjp) throws Throwable {
            log.info("[CORRECT] Before: {}", pjp.getSignature().getName());
            Object result = pjp.proceed(); // ← capture result
            log.info("[CORRECT] After: {} returned: {}", pjp.getSignature().getName(), result);
            return result; // ← MUST return it!
        }
    }

    // ===================================================================================
    // THE SWALLOWED EXCEPTION BUG
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class ExceptionSwallowingAspect {

        @Pointcut("execution(* com.techleadguru.phase1.day17.Day17AroundAdvice.InventoryService.*(..))")
        public void inventoryMethods() {}

        @Around("inventoryMethods()")
        public Object swallowException(ProceedingJoinPoint pjp) {
            try {
                return pjp.proceed();
            } catch (Throwable t) {
                log.error("[SWALLOWED] Exception in {}: {}", pjp.getSignature().getName(), t.getMessage());
                return null; // BUG: exception swallowed, caller gets null instead of exception
            }
        }
    }

    // ===================================================================================
    // CORRECT exception handling in @Around
    // ===================================================================================

    @Aspect
    @Slf4j
    public static class CorrectExceptionHandlingAspect {

        @Pointcut("execution(* com.techleadguru.phase1.day17.Day17AroundAdvice.InventoryService.*(..))")
        public void inventoryMethods() {}

        @Around("inventoryMethods()")
        public Object handleException(ProceedingJoinPoint pjp) throws Throwable {
            try {
                return pjp.proceed();
            } catch (IllegalArgumentException e) {
                // Convert to domain exception — explicit, documented behaviour
                log.warn("[CORRECT] Converting IllegalArgumentException: {}", e.getMessage());
                throw new IllegalStateException("Inventory reservation failed: " + e.getMessage(), e);
            }
            // Other Throwable types are NOT caught — they propagate naturally
        }
    }

    // ===================================================================================
    // Configuration
    // ===================================================================================

    @Configuration
    @EnableAspectJAutoProxy
    public static class Day17Config {

        @Bean
        public InventoryService inventoryService() { return new InventoryService(); }
    }
}
