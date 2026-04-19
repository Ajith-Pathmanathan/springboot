package com.techleadguru.phase6.day125;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class Day125OpenIDConnectTest {

    @Test
    void extractClaims_reads_sub_and_email() {
        Map<String, Object> claims = Map.of(
                "sub", "user-42",
                "email", "alice@example.com",
                "email_verified", true,
                "name", "Alice"
        );
        Day125OpenIDConnect.OidcClaims result = Day125OpenIDConnect.extractClaims(claims);
        assertThat(result.sub()).isEqualTo("user-42");
        assertThat(result.email()).isEqualTo("alice@example.com");
        assertThat(result.emailVerified()).isTrue();
        assertThat(result.name()).isEqualTo("Alice");
    }

    @Test
    void extractClaims_handles_missing_optional_fields() {
        Map<String, Object> claims = Map.of("sub", "user-99");
        Day125OpenIDConnect.OidcClaims result = Day125OpenIDConnect.extractClaims(claims);
        assertThat(result.sub()).isEqualTo("user-99");
        // Missing optional fields should return null or empty string
        assertThat(result.email()).isNullOrEmpty();
    }

    @Test
    void decodeIdToken_extracts_subject() {
        // Build a real base64url-encoded JWT-shaped string
        String header  = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
        long   futureExp = java.time.Instant.now().plusSeconds(3600).getEpochSecond();
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"sub\":\"oidc-user\",\"exp\":" + futureExp + ",\"iss\":\"https://issuer\"}").getBytes());
        String token = header + "." + payload + ".signature";

        Day125OpenIDConnect.OidcClaims claims = Day125OpenIDConnect.decodeIdToken(token);
        assertThat(claims.sub()).isEqualTo("oidc-user");
    }

    @Test
    void standardScopes_contains_openid() {
        List<String> scopes = Day125OpenIDConnect.standardScopes();
        assertThat(scopes).contains("openid");
    }

    @Test
    void standardScopes_includes_profile_and_email() {
        List<String> scopes = Day125OpenIDConnect.standardScopes();
        assertThat(scopes).contains("profile", "email");
    }

    @Test
    void isOidcRequest_true_when_scope_contains_openid() {
        assertThat(Day125OpenIDConnect.isOidcRequest(java.util.List.of("openid", "profile"))).isTrue();
        assertThat(Day125OpenIDConnect.isOidcRequest(java.util.List.of("openid"))).isTrue();
    }

    @Test
    void isOidcRequest_false_when_scope_lacks_openid() {
        assertThat(Day125OpenIDConnect.isOidcRequest(java.util.List.of("profile", "email"))).isFalse();
        assertThat(Day125OpenIDConnect.isOidcRequest(java.util.List.of())).isFalse();
    }

    @Test
    void standardEndpoints_has_six_entries() {
        List<Day125OpenIDConnect.OidcEndpoint> endpoints = Day125OpenIDConnect.standardEndpoints();
        assertThat(endpoints).hasSize(6);
        endpoints.forEach(e -> {
            assertThat(e.name()).isNotBlank();
            assertThat(e.path()).isNotBlank();
            assertThat(e.purpose()).isNotBlank();
        });
    }

    @Test
    void oidcVsOauth2_has_six_comparisons() {
        List<Day125OpenIDConnect.Comparison> comparisons = Day125OpenIDConnect.oidcVsOauth2();
        assertThat(comparisons).hasSize(6);
        comparisons.forEach(c -> {
            assertThat(c.aspect()).isNotBlank();
            assertThat(c.oauth2()).isNotBlank();
            assertThat(c.oidc()).isNotBlank();
        });
    }
}
