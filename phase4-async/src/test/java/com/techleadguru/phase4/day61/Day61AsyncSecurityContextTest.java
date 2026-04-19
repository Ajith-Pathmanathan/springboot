package com.techleadguru.phase4.day61;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 61 — @Async + SecurityContext Trap Test
 *
 * Verifies:
 * 1. Without TaskDecorator, SecurityContext is NULL on the async thread (returns ANONYMOUS)
 * 2. With SecurityContextPropagatingDecorator, the identity is correctly propagated
 *
 * Note: Spring Security is excluded in test application.properties, but
 * SecurityContextHolder is always available — we set it manually.
 */
@SpringBootTest(classes = Phase4Application.class)
class Day61AsyncSecurityContextTest {

    @Autowired Day61AsyncSecurityContext.AuditService auditService;

    @BeforeEach
    void setSecurityContext() {
        // Manually set authentication so it's in ThreadLocal SecurityContext
        var auth = new UsernamePasswordAuthenticationToken(
                "john.doe", "password", List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void brokenExecutor_returns_anonymous_because_context_not_propagated() throws Exception {
        // SecurityContext is set on THIS thread, but brokenExecutor has no TaskDecorator
        // → async thread starts with an empty SecurityContext → returns "ANONYMOUS/NULL"
        String user = auditService.auditBroken("login").get(3, TimeUnit.SECONDS);
        assertThat(user).contains("ANONYMOUS");
    }

    @Test
    void fixedExecutor_propagates_security_context_to_async_thread() throws Exception {
        // fixedExecutor has SecurityContextPropagatingDecorator
        // → copies SecurityContext to async thread → authentication is available
        String user = auditService.auditFixed("login").get(3, TimeUnit.SECONDS);
        assertThat(user).isEqualTo("john.doe");
    }

    @Test
    void broken_and_fixed_give_different_results_for_same_input() throws Exception {
        String brokenUser = auditService.auditBroken("action").get(3, TimeUnit.SECONDS);
        String fixedUser  = auditService.auditFixed("action").get(3, TimeUnit.SECONDS);

        assertThat(brokenUser).isNotEqualTo(fixedUser);
        assertThat(brokenUser).contains("ANONYMOUS");
        assertThat(fixedUser).isEqualTo("john.doe");
    }

    @Test
    void document_security_context_propagation_pattern() {
        // This test documents the correct pattern:
        // ❌ BROKEN  → @Async without decorator:  SecurityContextHolder.getContext() returns empty
        // ✅ FIXED   → @Async with SecurityContextPropagatingDecorator:
        //                new TaskDecorator that:
        //                  1. Captures context from caller
        //                  2. Sets it on the async thread
        //                  3. Clears it in finally block
        //
        // Alternative: Spring Security's DelegatingSecurityContextAsyncTaskExecutor
        //              (wraps Spring's ThreadPoolTaskExecutor automatically)
        assertThat(true).isTrue(); // documentation test — intent verified by above tests
    }
}
