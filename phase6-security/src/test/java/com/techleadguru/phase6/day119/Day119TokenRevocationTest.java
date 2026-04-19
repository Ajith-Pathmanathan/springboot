package com.techleadguru.phase6.day119;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class Day119TokenRevocationTest {

    private Day119TokenRevocation.InMemoryTokenBlacklist blacklist;
    private Day119TokenRevocation.TokenRevocationService  service;

    @BeforeEach
    void setUp() {
        blacklist = new Day119TokenRevocation.InMemoryTokenBlacklist();
        service   = new Day119TokenRevocation.TokenRevocationService(blacklist);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // InMemoryTokenBlacklist
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isRevoked_returns_false_for_unknown_token() {
        assertThat(blacklist.isRevoked("unknown-jti")).isFalse();
    }

    @Test
    void revoke_then_isRevoked_returns_true() {
        blacklist.revoke("jti-001", Duration.ofHours(1));
        assertThat(blacklist.isRevoked("jti-001")).isTrue();
    }

    @Test
    void revoke_adds_to_size() {
        blacklist.revoke("jti-001", Duration.ofHours(1));
        blacklist.revoke("jti-002", Duration.ofHours(1));
        assertThat(blacklist.size()).isEqualTo(2);
    }

    @Test
    void clear_removes_all_entries() {
        blacklist.revoke("jti-001", Duration.ofHours(1));
        blacklist.revoke("jti-002", Duration.ofHours(1));
        blacklist.clear();
        assertThat(blacklist.size()).isZero();
    }

    @Test
    void expired_entry_is_no_longer_revoked() {
        // Revoke with very short TTL (already past)
        blacklist.revoke("jti-old", Duration.ofMillis(1));
        // Wait for expiry
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        // Should not be revoked after TTL
        assertThat(blacklist.isRevoked("jti-old")).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TokenRevocationService
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isValid_returns_true_for_non_revoked_token() {
        assertThat(service.isValid("clean-jti")).isTrue();
    }

    @Test
    void logout_revokes_token_with_positive_ttl() {
        service.logout("jti-logout", Duration.ofHours(1));
        assertThat(service.isValid("jti-logout")).isFalse();
    }

    @Test
    void logout_does_not_revoke_when_ttl_is_zero() {
        service.logout("jti-zero", Duration.ZERO);
        // Zero/negative TTL — not added to blacklist
        assertThat(service.isValid("jti-zero")).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RedisTokenBlacklist (in-memory simulation)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void redisBlacklist_revoke_and_isRevoked_work_with_functional_ops() {
        Map<String, Instant> store = new ConcurrentHashMap<>();
        var redis = new Day119TokenRevocation.RedisTokenBlacklist(
                (key, ttl) -> store.put(key, Instant.now().plus(ttl)),
                (key)      -> store.containsKey(key) && Instant.now().isBefore(store.get(key))
        );

        redis.revoke("jti-redis", Duration.ofHours(1));
        assertThat(redis.isRevoked("jti-redis")).isTrue();
        assertThat(redis.isRevoked("unknown")).isFalse();
    }
}
