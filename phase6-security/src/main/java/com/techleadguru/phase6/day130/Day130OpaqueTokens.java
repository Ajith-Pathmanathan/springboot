package com.techleadguru.phase6.day130;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 130 — Opaque tokens + introspection endpoint.
 *
 * An opaque token is a random string with no embedded claims.
 * Only the Authorization Server (or a database it controls) knows what it means.
 *
 * Validation requires an introspection call (RFC 7662):
 *   POST /oauth2/introspect
 *   { "token": "abc123..." }
 *   → { "active": true, "sub": "alice", "scope": "read", "exp": 1234567890 }
 *
 * Opaque tokens vs JWTs:
 *   Opaque: easy revocation, no risk of claim leakage, but requires a round-trip
 *           to the auth server on every request.
 *   JWT:    no auth server round-trip, but revocation is harder (blacklist needed).
 *
 * Spring Boot Resource Server can validate opaque tokens:
 *   spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=...
 *   spring.security.oauth2.resourceserver.opaquetoken.client-id=...
 *   spring.security.oauth2.resourceserver.opaquetoken.client-secret=...
 */
public class Day130OpaqueTokens {

    // ─────────────────────────────────────────────────────────────────────────
    // Token store
    // ─────────────────────────────────────────────────────────────────────────

    /** Metadata about one opaque token. */
    public record TokenInfo(
            String  token,
            String  subject,
            String  scope,
            Instant expiry,
            boolean active) {}

    /** In-memory opaque token manager (production: use DB or Redis). */
    public static class OpaqueTokenManager {

        private final Map<String, TokenInfo> store = new ConcurrentHashMap<>();

        /** Issues a new opaque token for the subject with the given TTL. */
        public String issue(String subject, String scope, Duration ttl) {
            String token = UUID.randomUUID().toString().replace("-", "");
            store.put(token, new TokenInfo(token, subject, scope, Instant.now().plus(ttl), true));
            return token;
        }

        /**
         * Introspects a token.
         * Returns the TokenInfo if active; the returned 'active' field is false
         * if the token is revoked or expired.
         */
        public Optional<TokenInfo> introspect(String token) {
            TokenInfo info = store.get(token);
            if (info == null) return Optional.empty();

            if (!info.active() || Instant.now().isAfter(info.expiry())) {
                return Optional.of(new TokenInfo(
                        info.token(), info.subject(), info.scope(), info.expiry(), false));
            }
            return Optional.of(info);
        }

        /** Revokes a token immediately. */
        public void revoke(String token) {
            store.computeIfPresent(token, (k, v) ->
                    new TokenInfo(v.token(), v.subject(), v.scope(), v.expiry(), false));
        }

        /** Returns true if the token is active and not expired. */
        public boolean isActive(String token) {
            return introspect(token).map(TokenInfo::active).orElse(false);
        }

        public int tokenCount() { return store.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Introspection response (RFC 7662)
    // ─────────────────────────────────────────────────────────────────────────

    public record IntrospectionResponse(
            boolean active,
            String  sub,
            String  scope,
            long    exp,
            String  iss) {}

    public static IntrospectionResponse buildResponse(TokenInfo info, String issuer) {
        return new IntrospectionResponse(
                info.active(),
                info.subject(),
                info.scope(),
                info.expiry().getEpochSecond(),
                issuer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Opaque vs JWT comparison
    // ─────────────────────────────────────────────────────────────────────────

    public record ComparisonAspect(String aspect, String opaqueToken, String jwt) {}

    public static List<ComparisonAspect> opaqueVsJwt() {
        return List.of(
            new ComparisonAspect("Revocation",
                "Instant — just delete from store",
                "Needs blacklist + short TTL"),
            new ComparisonAspect("Validation cost",
                "Network round-trip to /introspect on every request",
                "Local signature check + exp — no network"),
            new ComparisonAspect("Claim exposure",
                "No claims in token — auth server is the only source",
                "Claims visible to anyone with the token (Base64URL)"),
            new ComparisonAspect("Scalability",
                "Auth server must handle introspection load",
                "Resource servers validate independently — scales well"),
            new ComparisonAspect("Token size",
                "Short random string (~32 chars)",
                "1-2 KB depending on claims")
        );
    }
}
