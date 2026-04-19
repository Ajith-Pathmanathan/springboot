package com.techleadguru.phase6.day128;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class Day128TokenCustomizerTest {

    @Test
    void recommendedCustomClaims_has_five_entries() {
        List<Day128TokenCustomizer.CustomClaim> claims = Day128TokenCustomizer.recommendedCustomClaims();
        assertThat(claims).hasSize(5);
    }

    @Test
    void recommendedCustomClaims_includes_roles_and_tenantId() {
        List<Day128TokenCustomizer.CustomClaim> claims = Day128TokenCustomizer.recommendedCustomClaims();
        List<String> names = claims.stream().map(Day128TokenCustomizer.CustomClaim::name).toList();
        assertThat(names).contains("roles", "tenantId");
    }

    @Test
    void recommendedCustomClaims_fields_are_non_blank() {
        Day128TokenCustomizer.recommendedCustomClaims().forEach(c -> {
            assertThat(c.name()).isNotBlank();
            assertThat(c.type()).isNotBlank();
            assertThat(c.purpose()).isNotBlank();
            assertThat(c.sampleValue()).isNotNull();
        });
    }

    @Test
    void rolesClaimCustomizer_adds_roles_under_roles_key() {
        Day128TokenCustomizer.RolesClaimCustomizer customizer =
                new Day128TokenCustomizer.RolesClaimCustomizer(
                        userId -> List.of("ROLE_ADMIN", "ROLE_USER"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-1");

        Map<String, Object> result = customizer.addRolesToClaims("user-1", claims);

        assertThat(result).containsKey("roles");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) result.get("roles");
        assertThat(roles).contains("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void rolesClaimCustomizer_does_not_remove_existing_claims() {
        Day128TokenCustomizer.RolesClaimCustomizer customizer =
                new Day128TokenCustomizer.RolesClaimCustomizer(
                        userId -> List.of("ROLE_USER"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-2");
        claims.put("email", "user2@example.com");

        Map<String, Object> result = customizer.addRolesToClaims("user-2", claims);

        assertThat(result).containsKey("email");
        assertThat(result.get("email")).isEqualTo("user2@example.com");
    }

    @Test
    void multiClaimCustomizer_adds_profile_claims() {
        Day128TokenCustomizer.MultiClaimCustomizer customizer = new Day128TokenCustomizer.MultiClaimCustomizer();

        Day128TokenCustomizer.MultiClaimCustomizer.UserProfile profile =
                new Day128TokenCustomizer.MultiClaimCustomizer.UserProfile(
                        "user-3", List.of("read", "write"), "tenant-x", "premium");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user-3");

        Map<String, Object> result = customizer.addProfileClaims(profile, claims);

        assertThat(result).containsKey("tenantId");
        assertThat(result).containsKey("tier");
        assertThat(result).containsKey("roles");
        assertThat(result.get("tenantId")).isEqualTo("tenant-x");
        assertThat(result.get("tier")).isEqualTo("premium");
    }

    @Test
    void tokenTypeGuidance_has_three_entries() {
        List<Day128TokenCustomizer.TokenTypeGuidance> guidance = Day128TokenCustomizer.tokenTypeGuidance();
        assertThat(guidance).hasSize(3);
        guidance.forEach(g -> {
            assertThat(g.tokenType()).isNotBlank();
            assertThat(g.claimsToAdd()).isNotBlank();
            assertThat(g.claimsToAvoid()).isNotBlank();
        });
    }
}
