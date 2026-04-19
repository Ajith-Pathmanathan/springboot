package com.techleadguru.phase6.day115;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

class Day115TokenLifecycleTest {

    private static final Duration SHORT_ACCESS  = Duration.ofSeconds(5);
    private static final Duration LONG_REFRESH  = Duration.ofDays(30);

    // ─────────────────────────────────────────────────────────────────────────
    // TokenPair
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void tokenPair_isAccessTokenExpired_false_when_not_expired() {
        var pair = new Day115TokenLifecycle.TokenPair(
                "acc", "ref", "u1",
                Instant.now().plusSeconds(900),
                Instant.now().plusSeconds(86400));
        assertThat(pair.isAccessTokenExpired()).isFalse();
        assertThat(pair.isRefreshTokenExpired()).isFalse();
    }

    @Test
    void tokenPair_isAccessTokenExpired_true_when_expired() {
        var pair = new Day115TokenLifecycle.TokenPair(
                "acc", "ref", "u1",
                Instant.now().minusSeconds(1),
                Instant.now().plusSeconds(86400));
        assertThat(pair.isAccessTokenExpired()).isTrue();
        assertThat(pair.isRefreshTokenExpired()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TokenLifecycleManager
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void issue_returns_token_pair_with_unique_tokens() {
        var mgr  = new Day115TokenLifecycle.TokenLifecycleManager(SHORT_ACCESS, LONG_REFRESH);
        var pair = mgr.issue("alice");
        assertThat(pair.accessToken()).isNotBlank();
        assertThat(pair.refreshToken()).isNotBlank();
        assertThat(pair.userId()).isEqualTo("alice");
        assertThat(pair.accessToken()).isNotEqualTo(pair.refreshToken());
    }

    @Test
    void issue_stores_pair_as_active_session() {
        var mgr = new Day115TokenLifecycle.TokenLifecycleManager(SHORT_ACCESS, LONG_REFRESH);
        mgr.issue("alice");
        assertThat(mgr.activeSessionCount()).isEqualTo(1);
    }

    @Test
    void issue_for_same_user_replaces_old_pair() {
        var mgr   = new Day115TokenLifecycle.TokenLifecycleManager(SHORT_ACCESS, LONG_REFRESH);
        var first = mgr.issue("alice");
        var second = mgr.issue("alice");
        assertThat(mgr.activeSessionCount()).isEqualTo(1);
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
    }

    @Test
    void refresh_returns_new_pair_for_valid_refresh_token() {
        var mgr  = new Day115TokenLifecycle.TokenLifecycleManager(SHORT_ACCESS, LONG_REFRESH);
        var pair = mgr.issue("alice");
        Optional<Day115TokenLifecycle.TokenPair> refreshed = mgr.refresh(pair.refreshToken());
        assertThat(refreshed).isPresent();
        assertThat(refreshed.get().userId()).isEqualTo("alice");
        assertThat(refreshed.get().refreshToken()).isNotEqualTo(pair.refreshToken());
    }

    @Test
    void refresh_returns_empty_for_unknown_token() {
        var mgr = new Day115TokenLifecycle.TokenLifecycleManager(SHORT_ACCESS, LONG_REFRESH);
        assertThat(mgr.refresh("unknown-token")).isEmpty();
    }

    @Test
    void revoke_removes_session() {
        var mgr  = new Day115TokenLifecycle.TokenLifecycleManager(SHORT_ACCESS, LONG_REFRESH);
        mgr.issue("alice");
        mgr.revoke("alice");
        assertThat(mgr.activeSessionCount()).isZero();
    }

    @Test
    void getActiveTokens_returns_empty_after_revoke() {
        var mgr  = new Day115TokenLifecycle.TokenLifecycleManager(SHORT_ACCESS, LONG_REFRESH);
        mgr.issue("alice");
        mgr.revoke("alice");
        assertThat(mgr.getActiveTokens("alice")).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recommendations
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void recommendedAccessTokenTtl_is_15_minutes() {
        assertThat(Day115TokenLifecycle.recommendedAccessTokenTtl())
                .isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void recommendedRefreshTokenTtl_is_30_days() {
        assertThat(Day115TokenLifecycle.recommendedRefreshTokenTtl())
                .isEqualTo(Duration.ofDays(30));
    }
}
