package com.techleadguru.phase6.day123;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 123 — Client Credentials: service-to-service auth.
 *
 * The Client Credentials grant is used when a service needs to authenticate
 * as ITSELF (not on behalf of a user). No browser redirect. No user interaction.
 *
 * Use cases:
 *  - Microservice A calls Microservice B's API
 *  - Background jobs calling internal services
 *  - CI/CD pipelines calling APIs
 *
 * Flow:
 *  1. Client sends clientId + clientSecret to /oauth2/token
 *  2. Auth server validates credentials
 *  3. Returns an access token (no refresh token — just re-request)
 *  4. Client uses access token in Authorization: Bearer header
 *
 * Spring Boot config:
 *   spring.security.oauth2.client.registration.my-service:
 *     authorization-grant-type: client_credentials
 *     client-id: my-service
 *     client-secret: ${CLIENT_SECRET}
 *     scope: api.read, api.write
 */
public class Day123ClientCredentials {

    /** A service-to-service access token issued via Client Credentials. */
    public record ClientToken(
            String accessToken,
            String tokenType,
            long   expiresIn,
            String scope) {

        public boolean isExpired(Instant issuedAt) {
            return Instant.now().isAfter(issuedAt.plusSeconds(expiresIn));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ClientCredentialsFlowSimulator (for tests, no network needed)
    // ─────────────────────────────────────────────────────────────────────────

    public static class ClientCredentialsFlowSimulator {

        /** clientId → client_secret */
        private final Map<String, String> clients = new ConcurrentHashMap<>();
        /** clientId → issued ClientToken + issuedAt */
        private final Map<String, Instant> issuedAt = new ConcurrentHashMap<>();

        private final Duration tokenTtl;

        public ClientCredentialsFlowSimulator(Duration tokenTtl) {
            this.tokenTtl = tokenTtl;
        }

        /** Registers a client. */
        public void registerClient(String clientId, String clientSecret) {
            clients.put(clientId, clientSecret);
        }

        /**
         * Authenticates the client and issues a token if credentials are valid.
         * Returns empty if the clientId is unknown or the secret is wrong.
         */
        public Optional<ClientToken> authenticate(String clientId, String clientSecret, String scope) {
            String stored = clients.get(clientId);
            if (stored == null || !stored.equals(clientSecret)) {
                return Optional.empty();
            }
            ClientToken token = new ClientToken(
                    UUID.randomUUID().toString(),
                    "Bearer",
                    tokenTtl.getSeconds(),
                    scope);
            issuedAt.put(token.accessToken(), Instant.now());
            return Optional.of(token);
        }

        /** Checks whether a token is still valid (not expired). */
        public boolean isTokenValid(String accessToken) {
            Instant issued = issuedAt.get(accessToken);
            if (issued == null) return false;
            return Instant.now().isBefore(issued.plusSeconds(tokenTtl.getSeconds()));
        }

        public int registeredClientCount() { return clients.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spring Boot configuration reference
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns the spring.security.oauth2.client.* property keys for this grant. */
    public static Map<String, String> springProperties(
            String registrationId, String clientId, String tokenUri, String scope) {
        String prefix = "spring.security.oauth2.client";
        return Map.of(
            prefix + ".registration." + registrationId + ".authorization-grant-type", "client_credentials",
            prefix + ".registration." + registrationId + ".client-id",                clientId,
            prefix + ".registration." + registrationId + ".scope",                    scope,
            prefix + ".provider."    + registrationId + ".token-uri",                 tokenUri
        );
    }
}
