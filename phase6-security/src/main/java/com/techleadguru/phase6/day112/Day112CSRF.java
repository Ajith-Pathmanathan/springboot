package com.techleadguru.phase6.day112;

import java.util.List;

/**
 * Day 112 — CSRF: when to enable vs disable.
 *
 * Cross-Site Request Forgery (CSRF) tricks a victim's browser into making
 * an authenticated request to your server from a malicious site.
 *
 * Spring Security enables CSRF protection by default.
 *
 * ENABLE CSRF when:
 *   - You use browser-based session cookies (stateful, server-rendered HTML)
 *   - You have a state-changing endpoint that a browser can reach
 *
 * DISABLE CSRF when:
 *   - Your API is stateless (uses JWT Bearer tokens)
 *   - You only serve non-browser clients (mobile apps, other services)
 *   - You have no session cookie (tokens sent via Authorization header)
 *
 * Why JWT APIs don't need CSRF: browsers can't automatically attach
 *   Authorization headers — only cookies are auto-sent. If your API
 *   relies solely on Bearer tokens, CSRF attack is not possible.
 */
public class Day112CSRF {

    public enum CsrfPolicy { ENABLE, DISABLE }

    /** Describes a scenario and its recommended CSRF policy. */
    public record CsrfScenario(
            String apiType,
            CsrfPolicy recommendedPolicy,
            String reason) {}

    /** Returns the CSRF policy guide for common API patterns. */
    public static List<CsrfScenario> csrfPolicyGuide() {
        return List.of(
            new CsrfScenario(
                "Traditional MVC / Thymeleaf web app with session cookies",
                CsrfPolicy.ENABLE,
                "Browser auto-sends session cookie → CSRF attack possible"),
            new CsrfScenario(
                "Stateless REST API using JWT Bearer tokens",
                CsrfPolicy.DISABLE,
                "Authorization header cannot be forged by another origin's JS"),
            new CsrfScenario(
                "Stateless REST API using HttpOnly cookie for JWT",
                CsrfPolicy.ENABLE,
                "Cookie is auto-sent by browser → CSRF protection needed"),
            new CsrfScenario(
                "Service-to-service (no browser, no cookie)",
                CsrfPolicy.DISABLE,
                "No browser involved → no CSRF risk"),
            new CsrfScenario(
                "Mobile-only API (tokens in Authorization header)",
                CsrfPolicy.DISABLE,
                "Mobile clients don't auto-send cookies → no CSRF risk"),
            new CsrfScenario(
                "SPA (React/Vue) with session cookie authentication",
                CsrfPolicy.ENABLE,
                "Browser session cookie is auto-sent across origins")
        );
    }

    /** Returns the recommended CSRF policy for a given API type description. */
    public static CsrfPolicy recommendedPolicyFor(String apiType) {
        return csrfPolicyGuide().stream()
                .filter(s -> s.apiType().equalsIgnoreCase(apiType))
                .map(CsrfScenario::recommendedPolicy)
                .findFirst()
                .orElse(CsrfPolicy.ENABLE); // safe default
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Configuration snippets (as Strings for documentation)
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns the Spring Security config snippet to DISABLE CSRF for a REST API. */
    public static String disableCsrfSnippet() {
        return """
            @Bean
            SecurityFilterChain api(HttpSecurity http) throws Exception {
                http
                    .csrf(csrf -> csrf.disable())  // stateless JWT API
                    .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
                return http.build();
            }
            """;
    }

    /** Returns the Spring Security config snippet to ENABLE CSRF with cookie token repo. */
    public static String enableCsrfWithCookieSnippet() {
        return """
            @Bean
            SecurityFilterChain web(HttpSecurity http) throws Exception {
                http.csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
                return http.build();
            }
            """;
    }

    /** Common CSRF attack vectors. */
    public static List<String> attackVectors() {
        return List.of(
            "Malicious HTML form auto-submitted on page load",
            "Image tag with state-changing GET endpoint as src",
            "JavaScript XMLHttpRequest from another origin (blocked by CORS, but cookie still sent)",
            "Third-party script injection that triggers state-changing POST"
        );
    }
}
