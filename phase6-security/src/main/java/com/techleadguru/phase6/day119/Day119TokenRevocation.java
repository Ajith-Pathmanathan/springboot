package com.techleadguru.phase6.day119;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 119 — Token revocation: Redis blacklist with TTL ⚠️
 *
 * Problem: JWTs are self-contained and stateless — once issued, they are valid
 *   until they expire. You cannot "delete" them from the client.
 *
 * Solution: maintain a server-side blacklist of revoked JWT IDs (jti claim).
 *   When a token is presented, check the blacklist before accepting.
 *   Blacklist entries expire after the token's remaining TTL (no need to keep forever).
 *
 * In production use Redis:
 *   SET blacklist:{jti} "revoked" EX {remaining_seconds}
 *   EXISTS blacklist:{jti}  → 1 = revoked, 0 = valid
 *
 * This file provides:
 *   - TokenBlacklist interface
 *   - InMemoryTokenBlacklist for tests
 *   - RedisTokenBlacklist sketch pointing at Spring Data Redis
 */
public class Day119TokenRevocation {

    // ─────────────────────────────────────────────────────────────────────────
    // Interface
    // ─────────────────────────────────────────────────────────────────────────

    public interface TokenBlacklist {
        /** Adds the token ID to the blacklist for the given TTL. */
        void revoke(String jti, Duration ttl);

        /** Returns true if the token ID is currently blacklisted. */
        boolean isRevoked(String jti);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // In-memory implementation (for tests and development)
    // ─────────────────────────────────────────────────────────────────────────

    public static class InMemoryTokenBlacklist implements TokenBlacklist {

        /** Maps jti → expiry timestamp of the blacklist entry. */
        private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

        @Override
        public void revoke(String jti, Duration ttl) {
            blacklist.put(jti, Instant.now().plus(ttl));
        }

        @Override
        public boolean isRevoked(String jti) {
            Instant expiry = blacklist.get(jti);
            if (expiry == null) return false;
            if (Instant.now().isAfter(expiry)) {
                blacklist.remove(jti);  // lazy cleanup
                return false;
            }
            return true;
        }

        /** Returns the current number of blacklisted entries. */
        public int size() { return blacklist.size(); }

        /** Removes all entries (useful in tests). */
        public void clear() { blacklist.clear(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Redis implementation (requires StringRedisTemplate; sketch only)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Production implementation using Spring Data Redis.
     *
     * Register as a @Bean and inject StringRedisTemplate.
     * The key expires automatically after the TTL — no cleanup job needed.
     *
     * This class is a sketch; Redis-based methods compile but are not tested
     * (no Redis server in CI). Use InMemoryTokenBlacklist in tests.
     */
    public static class RedisTokenBlacklist implements TokenBlacklist {

        private static final String PREFIX = "blacklist:";

        // Lazy functional interface to avoid requiring spring-data-redis on classpath
        // In real code: inject StringRedisTemplate
        private final java.util.function.BiConsumer<String, Duration> writeOp;
        private final java.util.function.Predicate<String>            existsOp;

        public RedisTokenBlacklist(
                java.util.function.BiConsumer<String, Duration> writeOp,
                java.util.function.Predicate<String>            existsOp) {
            this.writeOp = writeOp;
            this.existsOp = existsOp;
        }

        @Override
        public void revoke(String jti, Duration ttl) {
            writeOp.accept(PREFIX + jti, ttl);
        }

        @Override
        public boolean isRevoked(String jti) {
            return existsOp.test(PREFIX + jti);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Revocation service
    // ─────────────────────────────────────────────────────────────────────────

    public static class TokenRevocationService {

        private final TokenBlacklist blacklist;

        public TokenRevocationService(TokenBlacklist blacklist) {
            this.blacklist = blacklist;
        }

        /** Revokes a token given its jti and remaining lifetime. */
        public void logout(String jti, Duration remainingTtl) {
            if (remainingTtl.isPositive()) {
                blacklist.revoke(jti, remainingTtl);
            }
        }

        /** Checks whether the given jti is valid (not blacklisted). */
        public boolean isValid(String jti) {
            return !blacklist.isRevoked(jti);
        }
    }
}
