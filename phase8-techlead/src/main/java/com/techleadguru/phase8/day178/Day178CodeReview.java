package com.techleadguru.phase8.day178;

import java.util.*;

/**
 * Day 178 — Code Review as Tech Lead: What to Look For
 *
 * A tech lead's code review goes beyond syntax — it checks correctness,
 * security, performance, design, and maintainability. Good review comments
 * are specific, actionable, and suggest improvements.
 */
public class Day178CodeReview {

    // ─────────────────────────────────────────────────────────────────────────
    // Review categories
    // ─────────────────────────────────────────────────────────────────────────

    public enum ReviewCategory {
        CORRECTNESS,
        SECURITY,
        PERFORMANCE,
        MAINTAINABILITY,
        TESTING,
        DESIGN
    }

    public enum ReviewSeverity {
        BLOCKER,    // Must fix before merge
        MAJOR,      // Should fix; significant issue
        MINOR,      // Nice to fix; non-blocking
        NIT         // Style preference
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Review comment
    // ─────────────────────────────────────────────────────────────────────────

    public record ReviewComment(
            String         file,
            int            line,
            ReviewCategory category,
            ReviewSeverity severity,
            String         message,
            String         suggestion) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Checklist items
    // ─────────────────────────────────────────────────────────────────────────

    public record ChecklistItem(
            ReviewCategory category,
            String         item,
            String         antiPattern,
            String         fix) {}

    public static List<ChecklistItem> codeReviewChecklist() {
        return List.of(
            // Correctness
            new ChecklistItem(ReviewCategory.CORRECTNESS,
                "Null handling",
                "Optional.get() without isPresent(); NPE on returned collections",
                "Use Optional.orElseThrow() or map(); return empty collections not null"),
            new ChecklistItem(ReviewCategory.CORRECTNESS,
                "Concurrent state",
                "Mutable shared state (HashMap) accessed from multiple threads",
                "Use ConcurrentHashMap, AtomicReference, or @Synchronized"),
            new ChecklistItem(ReviewCategory.CORRECTNESS,
                "Transaction boundary",
                "Two @Transactional services calling each other — wrong propagation",
                "Understand Propagation.REQUIRED vs REQUIRES_NEW; avoid self-invocation pitfall"),

            // Security
            new ChecklistItem(ReviewCategory.SECURITY,
                "SQL injection",
                "String-concatenated JPQL or native queries",
                "Use @Query with :param or Criteria API"),
            new ChecklistItem(ReviewCategory.SECURITY,
                "Sensitive data logging",
                "log.info(\"password={}\", user.getPassword())",
                "Never log credentials, tokens, PII"),
            new ChecklistItem(ReviewCategory.SECURITY,
                "Authorisation check",
                "Service method trusts caller; no @PreAuthorize",
                "Add method-level @PreAuthorize or ownership check"),

            // Performance
            new ChecklistItem(ReviewCategory.PERFORMANCE,
                "N+1 query",
                "Lazy-loaded OneToMany fetched inside a loop",
                "Use JOIN FETCH or batch-size=25 or DTO projection"),
            new ChecklistItem(ReviewCategory.PERFORMANCE,
                "Missing cache",
                "Repeated identical DB calls within same request",
                "Cache with @Cacheable or local request-scoped Map"),
            new ChecklistItem(ReviewCategory.PERFORMANCE,
                "Unindexed query column",
                "WHERE clause on a column with no index — full table scan",
                "Add @Column + DB migration to create index"),

            // Maintainability
            new ChecklistItem(ReviewCategory.MAINTAINABILITY,
                "Magic numbers",
                "if (status == 3) — what does 3 mean?",
                "Extract to enum or named constant"),
            new ChecklistItem(ReviewCategory.MAINTAINABILITY,
                "Long method",
                "100+ line method with multiple concerns",
                "Extract to private methods; apply SRP"),
            new ChecklistItem(ReviewCategory.MAINTAINABILITY,
                "Hardcoded config",
                "Base URL, timeout, credentials in source code",
                "Move to application.properties + @Value or @ConfigurationProperties"),

            // Testing
            new ChecklistItem(ReviewCategory.TESTING,
                "No test for the change",
                "Feature added with no corresponding test",
                "Add unit test for new logic; integration test for new endpoint"),
            new ChecklistItem(ReviewCategory.TESTING,
                "Test asserts wrong thing",
                "assertEquals(true, result.isPresent()) — doesn't check value",
                "assertThat(result).hasValueSatisfying(v -> assertEquals(expected, v))"),

            // Design
            new ChecklistItem(ReviewCategory.DESIGN,
                "God class / violation of SRP",
                "OrderServiceImpl does create, price, notify, audit",
                "Extract PricingService, NotificationService, AuditService"),
            new ChecklistItem(ReviewCategory.DESIGN,
                "Leaking domain logic into controller",
                "Business rules evaluated in @RestController method",
                "Move domain logic to the service layer; controller only delegates")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Security review checklist (detailed)
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> securityChecklist() {
        return List.of(
            "Input validation: all user inputs validated at controller boundary",
            "SQL injection: no string concatenation in queries",
            "XSS: output is HTML-escaped when rendered (Thymeleaf th:text, not th:utext)",
            "CSRF: @CsrfToken or Spring Security CSRF filter for state-changing endpoints",
            "Authentication: all endpoints require auth unless explicitly public (permitAll)",
            "Authorisation: ownership checked — user can only access their own resources",
            "Secrets: no passwords, API keys, or tokens in source code or logs",
            "Dependency: no known CVEs in added dependencies (check with mvn dependency:check)",
            "HTTP headers: Security headers set (X-Content-Type, HSTS, CSP) via Spring Security",
            "Error messages: stack traces not exposed in API error responses"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Performance review checklist (detailed)
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> performanceChecklist() {
        return List.of(
            "N+1: every OneToMany relationship fetched lazily in a loop",
            "Missing index: WHERE / JOIN columns without DB index",
            "Large payload: returning full entity graph when only a few fields needed — use DTO",
            "Unbounded query: findAll() on large table without pagination",
            "Connection leak: Connection / PreparedStatement opened but not closed in finally block",
            "Cache missing: hot data (config, catalogue) fetched from DB on every request",
            "Sync I/O in loop: HTTP calls, file reads, DB queries inside a for-loop",
            "String concatenation in loop: use StringBuilder or String.join()",
            "Eager loading too much: FetchType.EAGER on large collections"
        );
    }
}
