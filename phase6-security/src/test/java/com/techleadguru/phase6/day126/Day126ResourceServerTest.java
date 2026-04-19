package com.techleadguru.phase6.day126;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class Day126ResourceServerTest {

    @Test
    void validationSteps_has_eight_steps() {
        List<Day126ResourceServer.ValidationStep> steps = Day126ResourceServer.validationSteps();
        assertThat(steps).hasSize(8);
    }

    @Test
    void validationSteps_are_ordered_one_to_eight() {
        List<Day126ResourceServer.ValidationStep> steps = Day126ResourceServer.validationSteps();
        for (int i = 0; i < steps.size(); i++) {
            assertThat(steps.get(i).order()).isEqualTo(i + 1);
        }
    }

    @Test
    void validationSteps_fields_are_non_blank() {
        Day126ResourceServer.validationSteps().forEach(s -> {
            assertThat(s.check()).isNotBlank();
            assertThat(s.passCondition()).isNotBlank();
        });
    }

    @Test
    void jwtClaimsExtractor_extractSubject() {
        Day126ResourceServer.JwtClaimsExtractor extractor = new Day126ResourceServer.JwtClaimsExtractor();
        Map<String, Object> claims = Map.of("sub", "service-a");
        assertThat(extractor.extractSubject(claims))
                .isEqualTo("service-a");
    }

    @Test
    void jwtClaimsExtractor_extractRoles_from_roles_claim() {
        Day126ResourceServer.JwtClaimsExtractor extractor = new Day126ResourceServer.JwtClaimsExtractor();
        Map<String, Object> claims = Map.of(
                "sub", "user-1",
                "roles", List.of("ROLE_ADMIN", "ROLE_USER")
        );
        List<String> roles = extractor.extractRoles(claims);
        assertThat(roles).contains("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void jwtClaimsExtractor_hasScope_true_for_contained_scope() {
        Day126ResourceServer.JwtClaimsExtractor extractor = new Day126ResourceServer.JwtClaimsExtractor();
        Map<String, Object> claims = Map.of("scope", "read write admin");
        assertThat(extractor.hasScope(claims, "write")).isTrue();
        assertThat(extractor.hasScope(claims, "delete")).isFalse();
    }

    @Test
    void jwtClaimsExtractor_extractTenantId() {
        Day126ResourceServer.JwtClaimsExtractor extractor = new Day126ResourceServer.JwtClaimsExtractor();
        Map<String, Object> claims = Map.of("tenantId", "tenant-abc");
        assertThat(extractor.extractTenantId(claims))
                .isEqualTo("tenant-abc");
    }

    @Test
    void jwtClaimsExtractor_extractKeycloakRealmRoles() {
        Day126ResourceServer.JwtClaimsExtractor extractor = new Day126ResourceServer.JwtClaimsExtractor();
        Map<String, Object> realmAccess = Map.of("roles", List.of("admin", "user"));
        Map<String, Object> claims = Map.of("realm_access", realmAccess);
        List<String> roles = extractor.extractKeycloakRealmRoles(claims);
        assertThat(roles).contains("admin", "user");
    }

    @Test
    void minimalProperties_contains_issuer_uri_key() {
        Map<String, String> props = Day126ResourceServer.minimalProperties("https://auth.example.com");
        String allKeys = String.join(" ", props.keySet());
        assertThat(allKeys.toLowerCase()).contains("issuer");
        assertThat(props.values()).contains("https://auth.example.com");
    }

    @Test
    void deriveJwksUri_appends_openid_configuration_path() {
        String jwksUri = Day126ResourceServer.deriveJwksUri("https://auth.example.com/realms/myrealm");
        assertThat(jwksUri).startsWith("https://auth.example.com/realms/myrealm");
        assertThat(jwksUri.toLowerCase()).contains("openid");
    }
}
