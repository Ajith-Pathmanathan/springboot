package com.techleadguru.phase6.day124;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

class Day124RefreshTokenRotationTest {

    private Day124RefreshTokenRotation.RefreshTokenStore newStore() {
        return new Day124RefreshTokenRotation.RefreshTokenStore(
                Duration.ofMinutes(15), Duration.ofDays(30));
    }

    @Test
    void issue_returns_token_set_with_unique_tokens() {
        Day124RefreshTokenRotation.RefreshTokenStore store = newStore();
        Day124RefreshTokenRotation.TokenSet ts = store.issue("user-1");
        assertThat(ts.accessToken()).isNotBlank();
        assertThat(ts.refreshToken()).isNotBlank();
        assertThat(ts.accessToken()).isNotEqualTo(ts.refreshToken());
        assertThat(ts.userId()).isEqualTo("user-1");
    }

    @Test
    void issue_generates_unique_tokens_on_each_call() {
        Day124RefreshTokenRotation.RefreshTokenStore store = newStore();
        Day124RefreshTokenRotation.TokenSet ts1 = store.issue("user-2");
        Day124RefreshTokenRotation.TokenSet ts2 = store.issue("user-2");
        assertThat(ts1.refreshToken()).isNotEqualTo(ts2.refreshToken());
    }

    @Test
    void rotate_returns_new_token_set() {
        Day124RefreshTokenRotation.RefreshTokenStore store = newStore();
        Day124RefreshTokenRotation.TokenSet original = store.issue("user-3");

        Optional<Day124RefreshTokenRotation.TokenSet> rotated = store.rotate(original.refreshToken());
        assertThat(rotated).isPresent();
        assertThat(rotated.get().refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(rotated.get().userId()).isEqualTo("user-3");
    }

    @Test
    void old_refresh_token_rejected_after_rotation() {
        Day124RefreshTokenRotation.RefreshTokenStore store = newStore();
        Day124RefreshTokenRotation.TokenSet original = store.issue("user-4");
        store.rotate(original.refreshToken());

        // original token should no longer be valid
        assertThat(store.isValid(original.refreshToken())).isFalse();
    }

    @Test
    void reuse_detection_revokes_entire_family() {
        Day124RefreshTokenRotation.RefreshTokenStore store = newStore();
        Day124RefreshTokenRotation.TokenSet original = store.issue("user-5");
        Day124RefreshTokenRotation.TokenSet rotated  = store.rotate(original.refreshToken()).orElseThrow();

        // Attacker reuses original (already rotated) token → whole family revoked
        Optional<Day124RefreshTokenRotation.TokenSet> result = store.rotate(original.refreshToken());
        assertThat(result).isEmpty();
        // Legitimate (rotated) token should also be revoked
        assertThat(store.isValid(rotated.refreshToken())).isFalse();
    }

    @Test
    void revoke_removes_token() {
        Day124RefreshTokenRotation.RefreshTokenStore store = newStore();
        Day124RefreshTokenRotation.TokenSet ts = store.issue("user-6");
        assertThat(store.isValid(ts.refreshToken())).isTrue();

        store.revoke(ts.refreshToken());
        assertThat(store.isValid(ts.refreshToken())).isFalse();
    }

    @Test
    void isValid_false_for_unknown_token() {
        Day124RefreshTokenRotation.RefreshTokenStore store = newStore();
        assertThat(store.isValid("no-such-token")).isFalse();
    }

    @Test
    void tokenSet_isRefreshExpired_false_right_after_issue() {
        Day124RefreshTokenRotation.TokenSet ts = new Day124RefreshTokenRotation.TokenSet(
                "at", "rt", "u", "fam", java.time.Instant.now().plusSeconds(60));
        assertThat(ts.isRefreshExpired()).isFalse();
    }

    @Test
    void tokenSet_isRefreshExpired_true_when_past_expiry() {
        Day124RefreshTokenRotation.TokenSet ts = new Day124RefreshTokenRotation.TokenSet(
                "at", "rt", "u", "fam", java.time.Instant.now().minusSeconds(1));
        assertThat(ts.isRefreshExpired()).isTrue();
    }

    @Test
    void rotate_returns_empty_for_unknown_token() {
        Day124RefreshTokenRotation.RefreshTokenStore store = newStore();
        Optional<Day124RefreshTokenRotation.TokenSet> result = store.rotate("ghost-token");
        assertThat(result).isEmpty();
    }

    @Test
    void recommended_ttls_are_reasonable() {
        assertThat(Day124RefreshTokenRotation.recommendedAccessTokenTtl().toMinutes()).isEqualTo(15);
        assertThat(Day124RefreshTokenRotation.recommendedRefreshTokenTtl().toDays()).isEqualTo(30);
    }
}
