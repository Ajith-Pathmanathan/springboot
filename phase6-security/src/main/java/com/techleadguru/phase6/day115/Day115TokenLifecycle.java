package com.techleadguru.phase6.day115;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 115 — Access token + refresh token lifecycle.
 *
 * Access token:  short-lived (15 min–1 h). Sent with every API request.
 *   Stateless JWT — server validates without DB lookup.
 *
 * Refresh token:  long-lived (days–months). Sent ONLY to /token endpoint.
 *   Must be stored server-side (or as opaque token) so it can be revoked.
 *
 * Flow:
 *   1. Login → server issues TokenPair (access + refresh)
 *   2. Client uses access token for API calls
 *   3. Access token expires → client sends refresh token to GET new TokenPair
 *   4. If refresh token expires → user must log in again
 *   5. Logout → revoke refresh token
 */
public class Day115TokenLifecycle {

    /** A pair of access and refresh tokens issued to a user. */
    public record TokenPair(
            String accessToken,
            String refreshToken,
            String userId,
            Instant accessTokenExpiry,
            Instant refreshTokenExpiry) {

        public boolean isAccessTokenExpired()  { return Instant.now().isAfter(accessTokenExpiry); }
        public boolean isRefreshTokenExpired() { return Instant.now().isAfter(refreshTokenExpiry); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TokenLifecycleManager
    // ─────────────────────────────────────────────────────────────────────────

    /** Manages token issuance, refresh and revocation (in-memory for demos). */
    public static class TokenLifecycleManager {

        private final Duration accessTokenTtl;
        private final Duration refreshTokenTtl;

        /** Maps userId → active TokenPair. */
        private final Map<String, TokenPair> byUserId        = new ConcurrentHashMap<>();
        /** Maps refreshToken → userId (for fast lookup during refresh). */
        private final Map<String, String>    refreshIndex    = new ConcurrentHashMap<>();

        public TokenLifecycleManager(Duration accessTokenTtl, Duration refreshTokenTtl) {
            this.accessTokenTtl  = accessTokenTtl;
            this.refreshTokenTtl = refreshTokenTtl;
        }

        /** Issues a new TokenPair for the user; invalidates any previous pair. */
        public TokenPair issue(String userId) {
            Instant now = Instant.now();
            TokenPair pair = new TokenPair(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    userId,
                    now.plus(accessTokenTtl),
                    now.plus(refreshTokenTtl)
            );
            // Remove old refresh token from index if present
            TokenPair old = byUserId.get(userId);
            if (old != null) refreshIndex.remove(old.refreshToken());

            byUserId.put(userId, pair);
            refreshIndex.put(pair.refreshToken(), userId);
            return pair;
        }

        /**
         * Exchanges a valid refresh token for a new TokenPair.
         * Invalidates the old refresh token (rotation).
         */
        public Optional<TokenPair> refresh(String refreshToken) {
            String userId = refreshIndex.get(refreshToken);
            if (userId == null) return Optional.empty();

            TokenPair existing = byUserId.get(userId);
            if (existing == null || existing.isRefreshTokenExpired()) {
                revoke(userId);
                return Optional.empty();
            }

            // Rotate: issue new pair  (old refresh token invalidated inside issue())
            return Optional.of(issue(userId));
        }

        /** Revokes all tokens for the user. */
        public void revoke(String userId) {
            TokenPair pair = byUserId.remove(userId);
            if (pair != null) refreshIndex.remove(pair.refreshToken());
        }

        /** Returns the active TokenPair (if any) for the given userId. */
        public Optional<TokenPair> getActiveTokens(String userId) {
            return Optional.ofNullable(byUserId.get(userId));
        }

        public int activeSessionCount() { return byUserId.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Default TTL recommendations
    // ─────────────────────────────────────────────────────────────────────────

    public static Duration recommendedAccessTokenTtl()  { return Duration.ofMinutes(15); }
    public static Duration recommendedRefreshTokenTtl() { return Duration.ofDays(30); }
}
