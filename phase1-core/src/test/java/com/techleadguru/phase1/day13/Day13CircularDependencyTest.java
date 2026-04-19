package com.techleadguru.phase1.day13;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 13 — Test: Circular dependency fix via shared component extraction.
 */
class Day13CircularDependencyTest {

    // -----------------------------------------------------------------------
    // Test 1: Shared context fix — both services use the same SharedAuthContext
    // -----------------------------------------------------------------------
    @Test
    void shared_auth_context_breaks_circular_dependency() {
        var ctx = new Day13CircularDependency.SharedAuthContext();
        var authService = new Day13CircularDependency.AuthService(ctx);
        var userService = new Day13CircularDependency.UserService(ctx);

        // AuthService sets context, UserService reads it — no circular dep
        authService.login("user-42");
        String profile = userService.getProfile();

        assertThat(profile).isEqualTo("Profile[user-42]");
        System.out.println("[DAY 13] Circular dep resolved via SharedAuthContext: " + profile);
    }

    // -----------------------------------------------------------------------
    // Test 2: Before login, profile is NOT_LOGGED_IN
    // -----------------------------------------------------------------------
    @Test
    void user_service_returns_not_logged_in_when_no_current_user() {
        var ctx = new Day13CircularDependency.SharedAuthContext();
        var userService = new Day13CircularDependency.UserService(ctx);

        String profile = userService.getProfile();

        assertThat(profile).isEqualTo("NOT_LOGGED_IN");
        System.out.println("[DAY 13] No user in context → NOT_LOGGED_IN");
    }

    // -----------------------------------------------------------------------
    // Test 3: Document why circular dep is a design smell
    // -----------------------------------------------------------------------
    @Test
    void document_circular_dependency_rules() {
        System.out.println("[DAY 13] CIRCULAR DEPENDENCY RULES:");
        System.out.println("  Spring Boot 2.6+: circular dependencies DISABLED by default.");
        System.out.println("  spring.main.allow-circular-references=true re-enables BUT is a band-aid.");
        System.out.println();
        System.out.println("  ROOT CAUSE: Classes have too many responsibilities (violates SRP).");
        System.out.println();
        System.out.println("  FIXES (in order of preference):");
        System.out.println("  1. Extract shared logic to a 3rd @Component (breaks the cycle).");
        System.out.println("  2. Use event/listener (ApplicationEvent) to invert the dep.");
        System.out.println("  3. @Lazy on one constructor parameter (proxy injected, real bean later).");
        System.out.println("  4. setter injection with @Autowired (last resort, avoids final fields).");
        System.out.println();
        System.out.println("  NEVER: allow-circular-references=true in production code.");
        assertThat(true).isTrue();
    }
}
