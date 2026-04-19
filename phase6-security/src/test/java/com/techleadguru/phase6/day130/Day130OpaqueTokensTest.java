package com.techleadguru.phase6.day130;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class Day130OpaqueTokensTest {

    @Test
    void issue_returns_non_blank_token() {
        Day130OpaqueTokens.OpaqueTokenManager mgr = new Day130OpaqueTokens.OpaqueTokenManager();
        String token = mgr.issue("subject-1", "read write", Duration.ofHours(1));
        assertThat(token).isNotBlank();
    }

    @Test
    void introspect_returns_active_for_valid_token() {
        Day130OpaqueTokens.OpaqueTokenManager mgr = new Day130OpaqueTokens.OpaqueTokenManager();
        String token = mgr.issue("subject-2", "api", Duration.ofHours(1));

        Day130OpaqueTokens.TokenInfo info = mgr.introspect(token).orElseThrow();
        assertThat(info.active()).isTrue();
        assertThat(info.subject()).isEqualTo("subject-2");
        assertThat(info.scope()).isEqualTo("api");
    }

    @Test
    void introspect_returns_empty_for_unknown_token() {
        Day130OpaqueTokens.OpaqueTokenManager mgr = new Day130OpaqueTokens.OpaqueTokenManager();
        assertThat(mgr.introspect("ghost-token")).isEmpty();
    }

    @Test
    void revoke_makes_token_inactive() {
        Day130OpaqueTokens.OpaqueTokenManager mgr = new Day130OpaqueTokens.OpaqueTokenManager();
        String token = mgr.issue("subject-3", "read", Duration.ofHours(1));
        assertThat(mgr.isActive(token)).isTrue();

        mgr.revoke(token);
        assertThat(mgr.isActive(token)).isFalse();
    }

    @Test
    void isActive_false_for_expired_token() {
        Day130OpaqueTokens.OpaqueTokenManager mgr = new Day130OpaqueTokens.OpaqueTokenManager();
        String token = mgr.issue("subject-4", "read", Duration.ofSeconds(-1));
        assertThat(mgr.isActive(token)).isFalse();
    }

    @Test
    void tokenCount_increases_with_each_issue() {
        Day130OpaqueTokens.OpaqueTokenManager mgr = new Day130OpaqueTokens.OpaqueTokenManager();
        mgr.issue("u1", "r", Duration.ofMinutes(10));
        mgr.issue("u2", "r", Duration.ofMinutes(10));
        assertThat(mgr.tokenCount()).isEqualTo(2);
    }

    @Test
    void buildResponse_reflects_token_info() {
        Day130OpaqueTokens.OpaqueTokenManager mgr = new Day130OpaqueTokens.OpaqueTokenManager();
        String token = mgr.issue("subject-5", "write", Duration.ofHours(2));

        Day130OpaqueTokens.TokenInfo info = mgr.introspect(token).orElseThrow();
        Day130OpaqueTokens.IntrospectionResponse resp =
                Day130OpaqueTokens.buildResponse(info, "https://issuer.example.com");
        assertThat(resp.active()).isTrue();
        assertThat(resp.sub()).isEqualTo("subject-5");
        assertThat(resp.scope()).isEqualTo("write");
        assertThat(resp.iss()).isEqualTo("https://issuer.example.com");
    }

    @Test
    void opaqueVsJwt_has_five_aspects() {
        List<Day130OpaqueTokens.ComparisonAspect> aspects = Day130OpaqueTokens.opaqueVsJwt();
        assertThat(aspects).hasSize(5);
        aspects.forEach(a -> {
            assertThat(a.aspect()).isNotBlank();
            assertThat(a.opaqueToken()).isNotBlank();
            assertThat(a.jwt()).isNotBlank();
        });
    }
}
