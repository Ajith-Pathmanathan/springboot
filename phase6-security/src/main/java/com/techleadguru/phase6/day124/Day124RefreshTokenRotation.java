package com.techleadguru.phase6.day124;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 124 — Refresh token rotation.
 *
 * Refresh token rotation is a security pattern where every time a refresh token
 * is used to obtain a new access token, it is ALSO replaced with a new refresh token.
 * The old refresh token is immediately invalidated.
 *
 * Why rotation?
 *  - If an attacker steals a refresh token and uses it, the legitimate user's
 *    next refresh attempt will FAIL (token was already rotated by attacker).
 *  - The server can detect this and revoke the entire session (token family).
 *
 * Token family / reuse detection:
 *  - Each token set belongs to a "family" (UUID issued at first login).
 *  - If a token from an already-rotated family is presented, revoke the WHOLE family.
 *
 * Recommended TTLs:
 *  - Access token: 15 minutes
 *  - Refresh token: 30 days (re-issue on each use)
 */
public class Day124RefreshTokenRotation {

    /** A set of tokens for one user session (one "token family"). */
    public record TokenSet(
            String accessToken,
            String refreshToken,
            String userId,
            String familyId,
            Instant refreshExpiry) {

        public boolean isRefreshExpired() { return Instant.now().isAfter(refreshExpiry); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RefreshTokenStore
    // ─────────────────────────────────────────────────────────────────────────

    public static class RefreshTokenStore {

        /** Maps refreshToken → TokenSet (only active tokens). */
        private final Map<String, TokenSet> active   = new ConcurrentHashMap<>();
        /** Tracks used-but-not-active tokens for reuse detection: token → familyId. */
        private final Map<String, String>   rotated  = new ConcurrentHashMap<>();

        private final Duration accessTokenTtl;
        private final Duration refreshTokenTtl;

        public RefreshTokenStore(Duration accessTokenTtl, Duration refreshTokenTtl) {
            this.accessTokenTtl  = accessTokenTtl;
            this.refreshTokenTtl = refreshTokenTtl;
        }

        /** Issues the initial token set for a user (new family). */
        public TokenSet issue(String userId) {
            return createAndStore(userId, UUID.randomUUID().toString());
        }

        /**
         * Rotates the refresh token.
         * On success → old token is invalidated, new TokenSet is returned.
         * On reuse detection → entire family is revoked, empty Optional returned.
         */
        public Optional<TokenSet> rotate(String refreshToken) {
            // 1. Reuse detection: was this token already rotated?
            if (rotated.containsKey(refreshToken)) {
                String familyId = rotated.get(refreshToken);
                revokeFamily(familyId);  // compromise detected
                return Optional.empty();
            }

            // 2. Normal case: look up active token
            TokenSet existing = active.get(refreshToken);
            if (existing == null || existing.isRefreshExpired()) {
                active.remove(refreshToken);
                return Optional.empty();
            }

            // 3. Move old token to rotated set, issue new token
            String familyId = existing.familyId();
            active.remove(refreshToken);
            rotated.put(refreshToken, familyId);

            return Optional.of(createAndStore(existing.userId(), familyId));
        }

        /** Explicitly revokes a refresh token (user logout). */
        public void revoke(String refreshToken) {
            active.remove(refreshToken);
        }

        /** Returns true if the refresh token is currently active and not expired. */
        public boolean isValid(String refreshToken) {
            TokenSet ts = active.get(refreshToken);
            return ts != null && !ts.isRefreshExpired();
        }

        public int activeTokenCount() { return active.size(); }

        // ── private helpers ─────────────────────────────────────────────────

        private TokenSet createAndStore(String userId, String familyId) {
            TokenSet ts = new TokenSet(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString(),
                    userId,
                    familyId,
                    Instant.now().plus(refreshTokenTtl)
            );
            active.put(ts.refreshToken(), ts);
            return ts;
        }

        private void revokeFamily(String familyId) {
            active.values().removeIf(ts -> ts.familyId().equals(familyId));
            rotated.entrySet().removeIf(e -> e.getValue().equals(familyId));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TTL recommendations
    // ─────────────────────────────────────────────────────────────────────────

    public static Duration recommendedAccessTokenTtl()  { return Duration.ofMinutes(15); }
    public static Duration recommendedRefreshTokenTtl() { return Duration.ofDays(30); }
}
