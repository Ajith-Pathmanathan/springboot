package com.techleadguru.phase1.day17;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * DAY 17 — Test: @Around advice bugs and correct patterns.
 */
class Day17AroundAdviceTest {

    private Day17AroundAdvice.InventoryService inventoryService;

    @BeforeEach
    void setup() {
        inventoryService = new Day17AroundAdvice.InventoryService();
    }

    // -----------------------------------------------------------------------
    // Test 1: Broken @Around — discards return value → caller gets null
    // -----------------------------------------------------------------------
    @Test
    void broken_around_discards_return_value_causing_null_result() throws Throwable {
        var brokenAspect = new Day17AroundAdvice.BrokenLoggingAspect();

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(pjp.getSignature().getName()).thenReturn("getStock");
        when(pjp.proceed()).thenReturn(42); // real method would return 42

        Object result = brokenAspect.log(pjp);

        assertThat(result).isNull(); // BUG: 42 was discarded, null returned!
        System.out.println("[DAY 17] BROKEN @Around: real result 42 discarded, returned: " + result);
    }

    // -----------------------------------------------------------------------
    // Test 2: Correct @Around — returns pjp.proceed() result
    // -----------------------------------------------------------------------
    @Test
    void correct_around_returns_proceed_result() throws Throwable {
        var correctAspect = new Day17AroundAdvice.CorrectLoggingAspect();

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(pjp.getSignature().getName()).thenReturn("getStock");
        when(pjp.proceed()).thenReturn(42);

        Object result = correctAspect.log(pjp);

        assertThat(result).isEqualTo(42); // FIX: result correctly propagated
        System.out.println("[DAY 17] CORRECT @Around: returned " + result);
    }

    // -----------------------------------------------------------------------
    // Test 3: Swallowing exception — caller gets null instead of exception
    // -----------------------------------------------------------------------
    @Test
    void swallowing_aspect_returns_null_when_exception_thrown() throws Throwable {
        var swallowingAspect = new Day17AroundAdvice.ExceptionSwallowingAspect();

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(pjp.getSignature().getName()).thenReturn("reserveStock");
        when(pjp.proceed()).thenThrow(new IllegalArgumentException("Cannot reserve more than 100"));

        Object result = swallowingAspect.swallowException(pjp);

        assertThat(result).isNull(); // BUG: exception swallowed, null returned
        System.out.println("[DAY 17] SWALLOWED exception → caller got null (silent data loss!)");
    }

    // -----------------------------------------------------------------------
    // Test 4: Correct exception handling — converts exception with context
    // -----------------------------------------------------------------------
    @Test
    void correct_exception_handling_converts_and_rethrows() throws Throwable {
        var correctAspect = new Day17AroundAdvice.CorrectExceptionHandlingAspect();

        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(pjp.getSignature().getName()).thenReturn("reserveStock");
        when(pjp.proceed()).thenThrow(new IllegalArgumentException("Cannot reserve more than 100"));

        assertThatThrownBy(() -> correctAspect.handleException(pjp))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Inventory reservation failed");

        System.out.println("[DAY 17] CORRECT: exception wrapped with context and re-thrown");
    }

    // -----------------------------------------------------------------------
    // Test 5: The real service works correctly without any aspect
    // -----------------------------------------------------------------------
    @Test
    void real_inventory_service_returns_correct_values() {
        assertThat(inventoryService.getStock("SKU-001")).isEqualTo(42);
        assertThat(inventoryService.reserveStock("SKU-001", 10)).isEqualTo("RESERVATION:SKU-001:10");
        assertThatThrownBy(() -> inventoryService.reserveStock("SKU-001", 200))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
