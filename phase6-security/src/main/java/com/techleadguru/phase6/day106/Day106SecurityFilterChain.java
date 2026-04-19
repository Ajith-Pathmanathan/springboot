package com.techleadguru.phase6.day106;

import java.util.List;
import java.util.Optional;

/**
 * Day 106 — SecurityFilterChain: all filters, order, inspect.
 *
 * Spring Security inserts a chain of Servlet filters in a fixed order.
 * Understanding the order is essential for debugging auth issues and writing
 * custom filters that plug in at the right point.
 *
 * Key idea:
 *   Each filter handles one cross-cutting security concern.
 *   Filters run in ascending `order` number.
 *   Custom filters use @Order or FilterRegistrationBean to set their position.
 */
public class Day106SecurityFilterChain {

    /** Describes one filter in the Spring Security filter chain. */
    public record FilterEntry(int order, String name, String purpose) {}

    /**
     * Returns the standard Spring Security filter chain entries in order.
     * (Approximate positions — Spring Security uses internal constants.)
     */
    public static List<FilterEntry> standardFilterOrder() {
        return List.of(
            new FilterEntry(100,  "DisableEncodeUrlFilter",
                "Prevents URL-encoding of session IDs (security hardening)"),
            new FilterEntry(200,  "WebAsyncManagerIntegrationFilter",
                "Propagates SecurityContext to async dispatch threads"),
            new FilterEntry(300,  "SecurityContextHolderFilter",
                "Loads/saves SecurityContext around the request lifecycle"),
            new FilterEntry(400,  "HeaderWriterFilter",
                "Writes security headers: X-Frame-Options, HSTS, X-Content-Type"),
            new FilterEntry(500,  "CorsFilter",
                "CORS pre-flight and response header management"),
            new FilterEntry(600,  "CsrfFilter",
                "Validates CSRF tokens for state-changing HTTP methods"),
            new FilterEntry(700,  "LogoutFilter",
                "Intercepts logout URL, invalidates session, clears context"),
            new FilterEntry(800,  "UsernamePasswordAuthenticationFilter",
                "Processes HTML login form (POST /login)"),
            new FilterEntry(850,  "BearerTokenAuthenticationFilter",
                "Extracts and validates Bearer JWT from Authorization header"),
            new FilterEntry(900,  "BasicAuthenticationFilter",
                "Processes HTTP Basic credentials from Authorization header"),
            new FilterEntry(1000, "RequestCacheAwareFilter",
                "Replays the saved request URL after successful authentication"),
            new FilterEntry(1100, "SecurityContextHolderAwareRequestFilter",
                "Wraps request with security-aware methods (isUserInRole etc.)"),
            new FilterEntry(1200, "AnonymousAuthenticationFilter",
                "Populates SecurityContext with anonymous token when no auth"),
            new FilterEntry(1300, "ExceptionTranslationFilter",
                "Converts AuthenticationException / AccessDeniedException to HTTP responses"),
            new FilterEntry(1400, "AuthorizationFilter",
                "Final gate: applies authorization rules (replaces FilterSecurityInterceptor)")
        );
    }

    /** Returns filter names in order. */
    public static List<String> filterNamesInOrder() {
        return standardFilterOrder().stream().map(FilterEntry::name).toList();
    }

    /** Finds a filter entry by name. */
    public static Optional<FilterEntry> findByName(String name) {
        return standardFilterOrder().stream()
                .filter(e -> e.name().equals(name))
                .findFirst();
    }

    /** Returns filters whose purpose contains the given keyword (case-insensitive). */
    public static List<FilterEntry> filtersByKeyword(String keyword) {
        String lower = keyword.toLowerCase();
        return standardFilterOrder().stream()
                .filter(e -> e.purpose().toLowerCase().contains(lower)
                          || e.name().toLowerCase().contains(lower))
                .toList();
    }

    /**
     * Guide for writing a custom filter.
     *
     * Steps:
     *  1. Extend OncePerRequestFilter
     *  2. Override doFilterInternal()
     *  3. Add to SecurityFilterChain via httpSecurity.addFilterBefore/After/At()
     *  4. Do NOT register as a @Bean (Spring Boot auto-adds it if registered as bean)
     */
    public static List<String> customFilterSteps() {
        return List.of(
            "1. extend OncePerRequestFilter",
            "2. override doFilterInternal(request, response, chain)",
            "3. call chain.doFilter(request, response) to pass to next filter",
            "4. add via httpSecurity.addFilterBefore(new MyFilter(), UsernamePasswordAuthenticationFilter.class)",
            "5. do NOT register as @Bean if you use addFilter* — avoid double registration"
        );
    }
}
