package com.techleadguru.phase6.day126;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Day 126 — Resource Server: validate JWT from Keycloak (or any OIDC provider).
 *
 * A Resource Server sits behind an Authorization Server. It:
 *  1. Accepts Bearer tokens in the Authorization header
 *  2. Validates the JWT signature using the provider's public key (from JWKS endpoint)
 *  3. Checks expiry, issuer, and audience claims
 *  4. Maps JWT claims to Spring Security authorities
 *
 * Spring Boot auto-configuration (just one property needed):
 *   spring.security.oauth2.resourceserver.jwt.issuer-uri=https://your-auth-server
 *
 * Spring Boot fetches the JWKS from {issuer-uri}/.well-known/openid-configuration
 * automatically and uses it to validate signatures.
 *
 * Custom claim extraction:  configure JwtAuthenticationConverter to read
 *   "roles" or "realm_access.roles" (Keycloak) from the JWT.
 */
public class Day126ResourceServer {

    // ─────────────────────────────────────────────────────────────────────────
    // JWT validation steps
    // ─────────────────────────────────────────────────────────────────────────

    public record ValidationStep(int order, String check, String passCondition) {}

    public static List<ValidationStep> validationSteps() {
        return List.of(
            new ValidationStep(1, "JWT format",        "Header.Payload.Signature (3 parts)"),
            new ValidationStep(2, "Algorithm",         "Header alg is in allowed set (RS256, etc.)"),
            new ValidationStep(3, "Signature",         "Verified against public key from JWKS endpoint"),
            new ValidationStep(4, "Expiry (exp)",      "Current time < exp claim"),
            new ValidationStep(5, "Not-before (nbf)",  "Current time >= nbf claim (if present)"),
            new ValidationStep(6, "Issuer (iss)",      "Matches configured issuer-uri"),
            new ValidationStep(7, "Audience (aud)",    "Token audience includes this resource server"),
            new ValidationStep(8, "Scope/Roles",       "Authorization checks via @PreAuthorize / filterChain")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Claims extractor
    // ─────────────────────────────────────────────────────────────────────────

    public static class JwtClaimsExtractor {

        /** Extracts the subject (user ID) from JWT claims. */
        public String extractSubject(Map<String, Object> claims) {
            return Objects.toString(claims.getOrDefault("sub", ""), "");
        }

        /** Extracts roles from the 'roles' claim (custom) or 'scope' (standard). */
        @SuppressWarnings("unchecked")
        public List<String> extractRoles(Map<String, Object> claims) {
            Object roles = claims.get("roles");
            if (roles instanceof List<?> list) {
                return list.stream().map(Objects::toString).toList();
            }
            // Fallback: parse scope as space-separated authorities
            Object scope = claims.get("scope");
            if (scope instanceof String s) {
                return List.of(s.split(" "));
            }
            return List.of();
        }

        /** Extracts the tenantId custom claim. */
        public String extractTenantId(Map<String, Object> claims) {
            return Objects.toString(claims.getOrDefault("tenantId", ""), "");
        }

        /** Returns true if the JWT has the given scope. */
        public boolean hasScope(Map<String, Object> claims, String requiredScope) {
            Object scope = claims.get("scope");
            if (scope instanceof String s) {
                return List.of(s.split(" ")).contains(requiredScope);
            }
            return false;
        }

        /**
         * Extracts Keycloak-style realm roles from:
         *   {"realm_access": {"roles": ["offline_access","ROLE_ADMIN"]}}
         */
        @SuppressWarnings("unchecked")
        public List<String> extractKeycloakRealmRoles(Map<String, Object> claims) {
            Object realmAccess = claims.get("realm_access");
            if (realmAccess instanceof Map<?, ?> map) {
                Object roles = map.get("roles");
                if (roles instanceof List<?> list) {
                    return list.stream().map(Objects::toString).toList();
                }
            }
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spring Boot property reference
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns the minimal Spring Boot Resource Server properties for a given issuer. */
    public static Map<String, String> minimalProperties(String issuerUri) {
        return Map.of(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri", issuerUri
        );
    }

    /** Returns the JWKS URI for an issuer following OIDC discovery convention. */
    public static String deriveJwksUri(String issuerUri) {
        String base = issuerUri.endsWith("/") ? issuerUri.substring(0, issuerUri.length() - 1) : issuerUri;
        return base + "/.well-known/openid-configuration";
    }
}
