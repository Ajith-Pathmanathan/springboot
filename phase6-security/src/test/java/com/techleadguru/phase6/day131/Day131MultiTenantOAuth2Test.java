package com.techleadguru.phase6.day131;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class Day131MultiTenantOAuth2Test {

    private Day131MultiTenantOAuth2.TenantConfig tenantA() {
        return new Day131MultiTenantOAuth2.TenantConfig(
                "tenant-a",
                "https://auth.example.com/realms/tenant-a",
                "https://auth.example.com/realms/tenant-a/protocol/openid-connect/certs",
                "my-app"
        );
    }

    @Test
    void registry_register_and_findByTenantId() {
        Day131MultiTenantOAuth2.TenantRegistry registry = new Day131MultiTenantOAuth2.TenantRegistry();
        registry.register(tenantA());
        Optional<Day131MultiTenantOAuth2.TenantConfig> found = registry.findByTenantId("tenant-a");
        assertThat(found).isPresent();
        assertThat(found.get().audience()).isEqualTo("my-app");
    }

    @Test
    void registry_findByIssuer_works() {
        Day131MultiTenantOAuth2.TenantRegistry registry = new Day131MultiTenantOAuth2.TenantRegistry();
        registry.register(tenantA());
        Optional<Day131MultiTenantOAuth2.TenantConfig> found =
                registry.findByIssuer("https://auth.example.com/realms/tenant-a");
        assertThat(found).isPresent();
        assertThat(found.get().tenantId()).isEqualTo("tenant-a");
    }

    @Test
    void registry_findByTenantId_empty_for_unknown() {
        Day131MultiTenantOAuth2.TenantRegistry registry = new Day131MultiTenantOAuth2.TenantRegistry();
        assertThat(registry.findByTenantId("ghost")).isEmpty();
    }

    @Test
    void registry_tenantCount_increments() {
        Day131MultiTenantOAuth2.TenantRegistry registry = new Day131MultiTenantOAuth2.TenantRegistry();
        assertThat(registry.tenantCount()).isEqualTo(0);
        registry.register(tenantA());
        assertThat(registry.tenantCount()).isEqualTo(1);
    }

    @Test
    void registry_registeredTenants_returns_set() {
        Day131MultiTenantOAuth2.TenantRegistry registry = new Day131MultiTenantOAuth2.TenantRegistry();
        registry.register(tenantA());
        Set<String> tenants = registry.registeredTenants();
        assertThat(tenants).contains("tenant-a");
    }

    @Test
    void registry_clear_removes_all_tenants() {
        Day131MultiTenantOAuth2.TenantRegistry registry = new Day131MultiTenantOAuth2.TenantRegistry();
        registry.register(tenantA());
        registry.clear();
        assertThat(registry.tenantCount()).isEqualTo(0);
    }

    @Test
    void validator_extractTenantId_from_custom_claim() {
        Day131MultiTenantOAuth2.TenantRegistry registry = new Day131MultiTenantOAuth2.TenantRegistry();
        registry.register(tenantA());
        Day131MultiTenantOAuth2.MultiTenantTokenValidator validator =
                new Day131MultiTenantOAuth2.MultiTenantTokenValidator(registry);

        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", "tenant-a");
        claims.put("iss", "https://auth.example.com/realms/tenant-a");

        String tenantId = validator.extractTenantId(claims);
        assertThat(tenantId).isEqualTo("tenant-a");
    }

    @Test
    void validator_extractTenantId_from_iss_when_no_custom_claim() {
        Day131MultiTenantOAuth2.TenantRegistry registry = new Day131MultiTenantOAuth2.TenantRegistry();
        registry.register(tenantA());
        Day131MultiTenantOAuth2.MultiTenantTokenValidator validator =
                new Day131MultiTenantOAuth2.MultiTenantTokenValidator(registry);

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://auth.example.com/realms/tenant-a");

        String tenantId = validator.extractTenantId(claims);
        assertThat(tenantId).isEqualTo("tenant-a");
    }

    @Test
    void validator_isValidTenant_true_for_registered_tenant() {
        Day131MultiTenantOAuth2.TenantRegistry registry = new Day131MultiTenantOAuth2.TenantRegistry();
        registry.register(tenantA());
        Day131MultiTenantOAuth2.MultiTenantTokenValidator validator =
                new Day131MultiTenantOAuth2.MultiTenantTokenValidator(registry);

        Map<String, Object> validClaims = new HashMap<>();
        validClaims.put("tenantId", "tenant-a");
        assertThat(validator.isValidTenant(validClaims)).isTrue();

        Map<String, Object> unknownClaims = new HashMap<>();
        unknownClaims.put("tenantId", "unknown-tenant");
        assertThat(validator.isValidTenant(unknownClaims)).isFalse();
    }

    @Test
    void validator_resolveJwksUri_returns_jwks_uri() {
        Day131MultiTenantOAuth2.TenantRegistry registry = new Day131MultiTenantOAuth2.TenantRegistry();
        registry.register(tenantA());
        Day131MultiTenantOAuth2.MultiTenantTokenValidator validator =
                new Day131MultiTenantOAuth2.MultiTenantTokenValidator(registry);

        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", "tenant-a");

        Optional<String> jwksUri = validator.resolveJwksUri(claims);
        assertThat(jwksUri).isPresent();
        assertThat(jwksUri.get()).contains("certs");
    }
}
