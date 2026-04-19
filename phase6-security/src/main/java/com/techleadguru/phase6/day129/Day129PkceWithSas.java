package com.techleadguru.phase6.day129;

import com.techleadguru.phase6.day122.Day122PKCE;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 129 — PKCE + SAS: full Authorization Code + PKCE flow.
 *
 * This day wires together the Authorization Code Flow (Day 121) with PKCE
 * (Day 122) and simulates what Spring Authorization Server does under the hood.
 *
 * Flow:
 *  1. Client generates code_verifier + code_challenge
 *  2. GET /authorize?...&code_challenge=...&code_challenge_method=S256
 *  3. User authenticates → server issues auth code + stores code_challenge
 *  4. POST /token?code=...&code_verifier=...
 *  5. Server verifies: SHA256(verifier) == stored challenge → issues tokens
 *
 * Key security property: attacker who intercepts the authorization code
 * CANNOT exchange it without the original code_verifier.
 */
public class Day129PkceWithSas {

    // ─────────────────────────────────────────────────────────────────────────
    // Request / Response records
    // ─────────────────────────────────────────────────────────────────────────

    public record AuthorizationRequest(
            String clientId,
            String redirectUri,
            String scope,
            String state,
            String codeChallenge,
            String challengeMethod) {}

    public record AuthorizationResponse(
            String code,
            String state) {}

    public record TokenRequest(
            String code,
            String codeVerifier,
            String clientId,
            String redirectUri) {}

    public record TokenResponse(
            String accessToken,
            String tokenType,
            long   expiresIn,
            String scope,
            String idToken) {}

    // ─────────────────────────────────────────────────────────────────────────
    // PkceFlowSimulator
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Simulates the PKCE-protected Authorization Code exchange.
     * No actual HTTP or crypto key management — purely models the state machine.
     */
    public static class PkceFlowSimulator {

        /** Maps authorization code → {code_challenge, clientId, redirectUri, scope}. */
        private record CodeEntry(String codeChallenge, String clientId, String redirectUri, String scope) {}
        private final Map<String, CodeEntry> pendingCodes = new ConcurrentHashMap<>();

        /**
         * Issues an authorization code after the user authenticates.
         * Stores the code_challenge for later verification.
         */
        public AuthorizationResponse issueAuthCode(AuthorizationRequest request) {
            if (request.codeChallenge() == null || request.codeChallenge().isBlank()) {
                throw new IllegalArgumentException("PKCE code_challenge is required");
            }
            String code = UUID.randomUUID().toString().replace("-", "");
            pendingCodes.put(code, new CodeEntry(
                    request.codeChallenge(),
                    request.clientId(),
                    request.redirectUri(),
                    request.scope()));
            return new AuthorizationResponse(code, request.state());
        }

        /**
         * Exchanges an authorization code for tokens, verifying the PKCE challenge.
         * Returns empty if the code is invalid, expired, or the verifier is wrong.
         */
        public Optional<TokenResponse> exchangeCode(TokenRequest request) {
            CodeEntry entry = pendingCodes.remove(request.code());
            if (entry == null) return Optional.empty();

            // Verify PKCE
            boolean valid = Day122PKCE.verify(request.codeVerifier(), entry.codeChallenge());
            if (!valid) return Optional.empty();

            // Validate redirect_uri and client_id match
            if (!entry.clientId().equals(request.clientId())) return Optional.empty();
            if (!entry.redirectUri().equals(request.redirectUri())) return Optional.empty();

            return Optional.of(new TokenResponse(
                    UUID.randomUUID().toString(),
                    "Bearer",
                    900,   // 15 minutes
                    entry.scope(),
                    null   // ID token omitted in this simulation
            ));
        }

        /** Returns the number of pending (not yet exchanged) authorization codes. */
        public int pendingCodeCount() { return pendingCodes.size(); }
    }
}
