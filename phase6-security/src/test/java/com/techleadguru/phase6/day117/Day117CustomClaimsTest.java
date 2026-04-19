package com.techleadguru.phase6.day117;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class Day117CustomClaimsTest {

    private final Day117CustomClaims.TenantClaimsExtractor extractor =
            new Day117CustomClaims.TenantClaimsExtractor();

    @Test
    void extract_returns_tenant_id_from_claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub",      "user-123");
        claims.put("tenantId", "acme");
        claims.put("roles",    List.of("ROLE_USER"));

        Day117CustomClaims.TenantClaims tc = extractor.extract(claims);
        assertThat(tc.tenantId()).isEqualTo("acme");
        assertThat(tc.userId()).isEqualTo("user-123");
    }

    @Test
    void extract_returns_roles_from_roles_claim() {
        Map<String, Object> claims = Map.of(
                "sub",   "alice",
                "roles", List.of("ROLE_ADMIN", "ROLE_USER"));

        Day117CustomClaims.TenantClaims tc = extractor.extract(claims);
        assertThat(tc.roles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void extract_returns_roles_from_scope_string() {
        Map<String, Object> claims = Map.of(
                "sub",   "bob",
                "scope", "openid profile read:orders");

        Day117CustomClaims.TenantClaims tc = extractor.extract(claims);
        assertThat(tc.roles()).contains("openid", "profile", "read:orders");
    }

    @Test
    void tenantClaims_hasRole_true_when_present() {
        var tc = new Day117CustomClaims.TenantClaims(
                "tenant1", "user1", List.of("ROLE_ADMIN"), Map.of());
        assertThat(tc.hasRole("ROLE_ADMIN")).isTrue();
        assertThat(tc.hasRole("ROLE_USER")).isFalse();
    }

    @Test
    void tenantClaims_isSameTenant_true_when_ids_match() {
        var tc = new Day117CustomClaims.TenantClaims(
                "acme", "user1", List.of(), Map.of());
        assertThat(tc.isSameTenant("acme")).isTrue();
        assertThat(tc.isSameTenant("other")).isFalse();
    }

    @Test
    void claimMatchesParam_builds_expression() {
        assertThat(Day117CustomClaims.claimMatchesParam("tenantId", "tid"))
                .isEqualTo("authentication.principal.claims['tenantId'] == #tid");
    }

    @Test
    void beanCheck_builds_expression() {
        assertThat(Day117CustomClaims.beanCheck("tenantGuard", "isMember", "tenantId"))
                .isEqualTo("@tenantGuard.isMember(#tenantId, authentication)");
    }

    @Test
    void recommendedCustomClaims_has_5_entries() {
        assertThat(Day117CustomClaims.recommendedCustomClaims()).hasSize(5);
    }

    @Test
    void recommendedCustomClaims_includes_roles_and_tenantId() {
        var names = Day117CustomClaims.recommendedCustomClaims().stream()
                .map(Day117CustomClaims.ClaimDefinition::claimName)
                .toList();
        assertThat(names).contains("roles", "tenantId");
    }

    @Test
    void extract_custom_claims_excludes_standard_claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub",      "alice");
        claims.put("iss",      "https://auth.example.com");
        claims.put("exp",      9999L);
        claims.put("tenantId", "acme");
        claims.put("tier",     "premium");

        Day117CustomClaims.TenantClaims tc = extractor.extract(claims);
        assertThat(tc.custom()).containsKey("tier");
        assertThat(tc.custom()).doesNotContainKey("sub");
        assertThat(tc.custom()).doesNotContainKey("iss");
        assertThat(tc.custom()).doesNotContainKey("exp");
    }
}
