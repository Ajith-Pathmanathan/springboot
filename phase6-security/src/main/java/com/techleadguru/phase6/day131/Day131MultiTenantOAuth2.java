package com.techleadguru.phase6.day131;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 131 — Multi-tenant OAuth2: tenantId in token + per-tenant JWKS.
 *
 * In a multi-tenant SaaS platform, each tenant may use a different Identity
 * Provider (Keycloak realm, Auth0 tenant, Azure AD tenant).
 *
 * Strategy: include the tenantId in the JWT (via a custom claim or the 'iss' URI).
 * On each request:
 *  1. Extract tenantId from the token (e.g., from 'iss' or custom 'tenantId' claim)
 *  2. Look up the tenant's JWKS URI and issuer from a registry
 *  3. Validate the JWT using the tenant-specific public key
 *
 * Spring Resource Server supports multi-tenancy via a custom
 * AuthenticationManagerResolver<HttpServletRequest> that picks the right
 * JwtDecoder based on the tenantId in the request.
 */
public class Day131MultiTenantOAuth2 {

    // ─────────────────────────────────────────────────────────────────────────
    // TenantConfig
    // ─────────────────────────────────────────────────────────────────────────

    /** Configuration for one tenant's identity provider. */
    public record TenantConfig(
            String tenantId,
            String issuerUri,
            String jwksUri,
            String audience) {}

    // ─────────────────────────────────────────────────────────────────────────
    // TenantRegistry
    // ─────────────────────────────────────────────────────────────────────────

    public static class TenantRegistry {

        private final Map<String, TenantConfig> tenants = new ConcurrentHashMap<>();

        /** Registers a new tenant configuration. */
        public void register(TenantConfig config) {
            tenants.put(config.tenantId(), config);
        }

        /** Finds the tenant config by tenantId. */
        public Optional<TenantConfig> findByTenantId(String tenantId) {
            return Optional.ofNullable(tenants.get(tenantId));
        }

        /** Finds the tenant config whose issuerUri matches. */
        public Optional<TenantConfig> findByIssuer(String issuerUri) {
            return tenants.values().stream()
                    .filter(t -> t.issuerUri().equals(issuerUri))
                    .findFirst();
        }

        public Set<String> registeredTenants() { return Set.copyOf(tenants.keySet()); }
        public int tenantCount() { return tenants.size(); }
        public void clear() { tenants.clear(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MultiTenantTokenValidator
    // ─────────────────────────────────────────────────────────────────────────

    public static class MultiTenantTokenValidator {

        private final TenantRegistry registry;

        public MultiTenantTokenValidator(TenantRegistry registry) {
            this.registry = registry;
        }

        /** Extracts the tenantId from JWT claims (custom claim or from 'iss'). */
        public String extractTenantId(Map<String, Object> claims) {
            // Try explicit 'tenantId' custom claim first
            Object tid = claims.get("tenantId");
            if (tid instanceof String s && !s.isBlank()) return s;
            // Fall back: derive from issuer URI
            Object iss = claims.get("iss");
            if (iss instanceof String issuer) {
                return registry.findByIssuer(issuer)
                        .map(TenantConfig::tenantId)
                        .orElse(null);
            }
            return null;
        }

        /**
         * Returns true if the token's tenantId is registered in the registry.
         * This is a preliminary check before full JWT validation.
         */
        public boolean isValidTenant(Map<String, Object> claims) {
            String tenantId = extractTenantId(claims);
            return tenantId != null && registry.findByTenantId(tenantId).isPresent();
        }

        /**
         * Returns the JWKS URI to use for validating a token.
         * In production, each JwtDecoder is created with the per-tenant JWKS URI.
         */
        public Optional<String> resolveJwksUri(Map<String, Object> claims) {
            String tenantId = extractTenantId(claims);
            if (tenantId == null) return Optional.empty();
            return registry.findByTenantId(tenantId).map(TenantConfig::jwksUri);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JWKS URI derivation
    // ─────────────────────────────────────────────────────────────────────────

    /** Derives a standard JWKS URI from an issuer URI (OIDC convention). */
    public static String deriveJwksUri(String issuerUri) {
        String base = issuerUri.endsWith("/") ? issuerUri : issuerUri + "/";
        return base + "protocol/openid-connect/certs"; // Keycloak convention
    }

    /** Builds a Keycloak realm issuer URI. */
    public static String keycloakIssuerUri(String serverUrl, String realm) {
        return serverUrl + "/realms/" + realm;
    }
}
