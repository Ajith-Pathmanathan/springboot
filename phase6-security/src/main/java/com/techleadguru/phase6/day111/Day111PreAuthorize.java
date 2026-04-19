package com.techleadguru.phase6.day111;

import java.util.List;
import java.util.Map;

/**
 * Day 111 — @PreAuthorize: expressions + #param binding.
 *
 * @PreAuthorize evaluates a Spring EL (SpEL) expression BEFORE a method runs.
 * It is one of the most powerful and commonly-used method security annotations.
 *
 * Common patterns:
 *   hasRole('ADMIN')                —  user must have ROLE_ADMIN authority
 *   hasAnyRole('USER','ADMIN')      —  user has at least one of the roles
 *   isAuthenticated()               —  any logged-in user
 *   #id == authentication.name     —  method arg #id must equal the logged-in username
 *   @myBean.isOwner(#id)            —  delegates to a Spring bean method
 *
 * Setup: add @EnableMethodSecurity to a @Configuration class.
 */
public class Day111PreAuthorize {

    /** Describes one @PreAuthorize expression pattern. */
    public record PermissionExample(
            String expression,
            String description,
            String exampleUseCase) {}

    /** Returns a catalogue of common @PreAuthorize expressions. */
    public static List<PermissionExample> commonExpressions() {
        return List.of(
            new PermissionExample(
                "hasRole('ADMIN')",
                "User must have ROLE_ADMIN authority",
                "Admin-only management endpoints"),
            new PermissionExample(
                "hasAnyRole('USER', 'ADMIN')",
                "User must have at least one of the listed roles",
                "Endpoints accessible to regular users and admins"),
            new PermissionExample(
                "isAuthenticated()",
                "Any authenticated user (not anonymous)",
                "General API that requires login"),
            new PermissionExample(
                "isAnonymous()",
                "Only anonymous users",
                "Redirect authenticated users away from login page"),
            new PermissionExample(
                "permitAll()",
                "No restriction — everyone including anonymous",
                "Public resources"),
            new PermissionExample(
                "#id == authentication.name",
                "Method param #id must equal the authenticated username",
                "Users can only access their own profile"),
            new PermissionExample(
                "authentication.principal.email == #email",
                "Principal field matches method param",
                "Users can only update their own email"),
            new PermissionExample(
                "@ownershipBean.isOwner(#resourceId, authentication.name)",
                "Delegates to a Spring bean for complex logic",
                "Custom ownership or ACL checks"),
            new PermissionExample(
                "hasAuthority('SCOPE_read:orders')",
                "User has a specific fine-grained authority/scope",
                "OAuth2 scope-based access"),
            new PermissionExample(
                "hasRole('ADMIN') or (#userId == authentication.name)",
                "Admin OR own resource",
                "Users manage own data; admins manage all")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Builder helpers — generate expression strings programmatically
    // ─────────────────────────────────────────────────────────────────────────

    public static String hasRole(String role) {
        return "hasRole('" + role + "')";
    }

    public static String hasAnyRole(String... roles) {
        StringBuilder sb = new StringBuilder("hasAnyRole(");
        for (int i = 0; i < roles.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("'").append(roles[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    public static String hasAuthority(String authority) {
        return "hasAuthority('" + authority + "')";
    }

    public static String paramMatchesCurrentUser(String paramName) {
        return "#" + paramName + " == authentication.name";
    }

    public static String beanMethod(String beanName, String method, String param) {
        return "@" + beanName + "." + method + "(#" + param + ", authentication.name)";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Common pitfalls
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns a map of common @PreAuthorize mistakes and their fixes. */
    public static Map<String, String> commonPitfalls() {
        return Map.of(
            "Using @PreAuthorize on private method",
            "Spring AOP cannot intercept private methods — use public or protected",
            "Using @PreAuthorize without @EnableMethodSecurity",
            "Add @EnableMethodSecurity to a @Configuration class",
            "Using @Secured instead of @PreAuthorize for SpEL",
            "@Secured only supports role names, not SpEL expressions — use @PreAuthorize",
            "Self-invocation bypasses @PreAuthorize",
            "Same as @Transactional: calling from within the same bean skips the AOP proxy"
        );
    }
}
