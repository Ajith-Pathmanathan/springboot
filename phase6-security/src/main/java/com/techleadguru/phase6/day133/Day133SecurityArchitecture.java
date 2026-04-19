package com.techleadguru.phase6.day133;

import java.util.List;

/**
 * Day 133 — Security architecture design: web + mobile + service-to-service.
 *
 * This capstone day synthesises all Phase 6 learnings into a security
 * architecture guide covering three client types:
 *
 *  Web browser (SPA + server-rendered):
 *   → Authorization Code + PKCE; HttpOnly cookie or in-memory storage for tokens
 *
 *  Mobile app:
 *   → Authorization Code + PKCE; OS secure storage (Keychain / Keystore)
 *
 *  Service-to-service:
 *   → Client Credentials; short-lived access tokens; no refresh tokens
 *
 * Common principles:
 *  - Never put client_secret in a public client
 *  - Always use PKCE for public clients
 *  - Validate tokens at the gateway — not just in each service
 *  - Short access token TTL (15 min) + refresh token rotation
 *  - Revoke tokens on logout via Redis blacklist or opaque tokens
 */
public class Day133SecurityArchitecture {

    // ─────────────────────────────────────────────────────────────────────────
    // Architecture decisions per client type
    // ─────────────────────────────────────────────────────────────────────────

    public enum ClientType { WEB_BROWSER_SPA, WEB_BROWSER_SERVER_RENDERED, MOBILE_APP, SERVICE_TO_SERVICE, USER_CLI }

    /** Key architecture decision for one client type. */
    public record ArchitectureDecision(
            ClientType clientType,
            String recommendedFlow,
            String tokenStorage,
            String refreshStrategy,
            String csrfStrategy) {}

    public static List<ArchitectureDecision> architectureGuide() {
        return List.of(
            new ArchitectureDecision(
                ClientType.WEB_BROWSER_SPA,
                "Authorization Code + PKCE (public client, no client_secret)",
                "In-memory (JS variables) or HttpOnly cookie for access token",
                "Silent refresh via iframe or refresh token in HttpOnly cookie",
                "Not needed if tokens stored in memory; needed if cookie-based"),
            new ArchitectureDecision(
                ClientType.WEB_BROWSER_SERVER_RENDERED,
                "Authorization Code (confidential client, client_secret on server)",
                "Server-side session; send session cookie to browser (HttpOnly, Secure, SameSite=Lax)",
                "Server silently refreshes using refresh token; user sees no interruption",
                "Enable CSRF — session cookie is auto-sent by browser"),
            new ArchitectureDecision(
                ClientType.MOBILE_APP,
                "Authorization Code + PKCE (public client)",
                "OS secure storage: iOS Keychain / Android Keystore",
                "Rotate refresh tokens; re-prompt biometric on refresh if TTL exceeded",
                "Not applicable — no browser cookies"),
            new ArchitectureDecision(
                ClientType.SERVICE_TO_SERVICE,
                "Client Credentials (no user, no PKCE)",
                "In-memory cache; re-fetch when expired (no persistent storage needed)",
                "No refresh tokens — just re-request with client credentials",
                "Not applicable — no browser, no cookies"),
            new ArchitectureDecision(
                ClientType.USER_CLI,
                "Authorization Code + PKCE via system browser",
                "OS secret store or encrypted config file",
                "Automatic token refresh; prompt re-authentication if refresh expired",
                "Not applicable")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Production security checklist
    // ─────────────────────────────────────────────────────────────────────────

    public record SecurityChecklistItem(
            String   category,
            String   item,
            boolean  critical) {}

    public static List<SecurityChecklistItem> productionChecklist() {
        return List.of(
            // Token security
            new SecurityChecklistItem("Token Security", "Access token TTL ≤ 15 minutes", true),
            new SecurityChecklistItem("Token Security", "Refresh token rotation enabled", true),
            new SecurityChecklistItem("Token Security", "Token revocation implemented (blacklist or opaque)", true),
            new SecurityChecklistItem("Token Security", "alg:none rejected by all validators", true),
            new SecurityChecklistItem("Token Security", "JWT signature verified before trusting claims", true),
            // PKCE
            new SecurityChecklistItem("PKCE", "PKCE enforced for all public clients", true),
            new SecurityChecklistItem("PKCE", "S256 method required (PLAIN rejected)", true),
            // Password
            new SecurityChecklistItem("Password", "BCrypt with cost factor ≥ 12", true),
            new SecurityChecklistItem("Password", "Password upgrade on login when factor is low", false),
            // CSRF
            new SecurityChecklistItem("CSRF", "CSRF disabled for stateless JWT APIs", false),
            new SecurityChecklistItem("CSRF", "CSRF enabled for session-cookie-based apps", true),
            // Headers
            new SecurityChecklistItem("HTTP Headers", "HSTS enabled (Strict-Transport-Security)", true),
            new SecurityChecklistItem("HTTP Headers", "X-Frame-Options: DENY or SAMEORIGIN", true),
            new SecurityChecklistItem("HTTP Headers", "Content-Security-Policy defined", false),
            // OAuth2
            new SecurityChecklistItem("OAuth2", "Redirect URIs exactly matched (no wildcard)", true),
            new SecurityChecklistItem("OAuth2", "State parameter validated on callback", true),
            new SecurityChecklistItem("OAuth2", "Auth codes are single-use and short-lived (< 10 min)", true),
            // Async
            new SecurityChecklistItem("Spring Security", "SecurityContext propagated in @Async methods", false),
            // Method security
            new SecurityChecklistItem("Authorization", "@EnableMethodSecurity on @Configuration class", false),
            new SecurityChecklistItem("Authorization", "@PreAuthorize on all sensitive methods", false)
        );
    }

    public static long countCriticalItems() {
        return productionChecklist().stream().filter(SecurityChecklistItem::critical).count();
    }

    public static List<String> categories() {
        return productionChecklist().stream()
                .map(SecurityChecklistItem::category)
                .distinct()
                .toList();
    }
}
