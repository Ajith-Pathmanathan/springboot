package com.techleadguru.phase6.day121;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Day 121 — Authorization Code Flow end-to-end.
 *
 * The Authorization Code Flow is the most secure OAuth 2.0 grant for
 * confidential clients (web apps with a server back-end).
 *
 * Flow:
 *  1. Client builds an authorization URL and redirects the browser
 *  2. User authenticates on the Authorization Server and grants consent
 *  3. Authorization Server redirects back with a short-lived code
 *  4. Client exchanges the code for tokens (server-to-server)
 *  5. Tokens are returned to the client's back-end (never exposed in URL)
 *  6. Client uses access token to call the Resource Server
 *
 * Key security properties:
 *  - The code is single-use and short-lived (~10 minutes)
 *  - Tokens never appear in browser history (exchange happens server→server)
 *  - The 'state' parameter prevents CSRF during the redirect
 */
public class Day121AuthorizationCodeFlow {

    // ─────────────────────────────────────────────────────────────────────────
    // Flow steps
    // ─────────────────────────────────────────────────────────────────────────

    public enum Step {
        REDIRECT_TO_AUTH_SERVER,
        USER_AUTHENTICATES,
        AUTH_SERVER_ISSUES_CODE,
        CLIENT_RECEIVES_CODE,
        CLIENT_EXCHANGES_CODE,
        AUTH_SERVER_ISSUES_TOKENS,
        CLIENT_ACCESSES_RESOURCE
    }

    /** Detail for one flow step. */
    public record FlowStepDetail(
            Step step,
            String actor,
            String httpMethod,
            String endpoint,
            String keyParameters) {}

    public static List<FlowStepDetail> authorizationCodeFlow() {
        return List.of(
            new FlowStepDetail(Step.REDIRECT_TO_AUTH_SERVER,
                "Client (browser redirect)",
                "GET",
                "/authorize",
                "response_type=code, client_id, redirect_uri, scope, state"),
            new FlowStepDetail(Step.USER_AUTHENTICATES,
                "Authorization Server → User",
                "POST",
                "/login",
                "username, password (or SSO, MFA)"),
            new FlowStepDetail(Step.AUTH_SERVER_ISSUES_CODE,
                "Authorization Server",
                "302 Redirect",
                "{redirect_uri}",
                "code, state (must match original state)"),
            new FlowStepDetail(Step.CLIENT_RECEIVES_CODE,
                "Client back-end",
                "GET",
                "{redirect_uri}?code=...&state=...",
                "Extract code; validate state vs session"),
            new FlowStepDetail(Step.CLIENT_EXCHANGES_CODE,
                "Client back-end → Authorization Server",
                "POST",
                "/token",
                "grant_type=authorization_code, code, redirect_uri, client_id, client_secret"),
            new FlowStepDetail(Step.AUTH_SERVER_ISSUES_TOKENS,
                "Authorization Server",
                "200 JSON",
                "/token response",
                "access_token, token_type, expires_in, refresh_token, id_token (OIDC)"),
            new FlowStepDetail(Step.CLIENT_ACCESSES_RESOURCE,
                "Client back-end → Resource Server",
                "GET/POST",
                "/api/resource",
                "Authorization: Bearer {access_token}")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State parameter helpers (CSRF protection during redirect)
    // ─────────────────────────────────────────────────────────────────────────

    /** Generates a cryptographically random state value. */
    public static String generateState() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Validates the state returned by the Authorization Server.
     * The returned state MUST exactly match the one sent in the authorization request.
     */
    public static boolean validateState(String originalState, String returnedState) {
        return originalState != null
            && returnedState != null
            && originalState.equals(returnedState);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Authorization URL builder
    // ─────────────────────────────────────────────────────────────────────────

    /** Builds the authorization URL query parameters (for documentation / tests). */
    public static Map<String, String> authorizationParams(
            String clientId, String redirectUri, String scope, String state) {
        return Map.of(
            "response_type", "code",
            "client_id",     clientId,
            "redirect_uri",  redirectUri,
            "scope",         scope,
            "state",         state
        );
    }
}
