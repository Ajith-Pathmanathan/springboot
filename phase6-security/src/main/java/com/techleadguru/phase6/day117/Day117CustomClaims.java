package com.techleadguru.phase6.day117;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Day 117 — Custom claims: tenantId, roles in @PreAuthorize.
 *
 * In a multi-tenant or RBAC system, the JWT carries application-specific claims:
 *   "tenantId": "acme-corp"
 *   "roles"   : ["ROLE_ADMIN", "ROLE_USER"]
 *   "tier"    : "premium"
 *
 * In Spring Security Resource Server, a JwtAuthenticationConverter can extract
 * these claims and populate the Authentication's authorities.
 *
 * In @PreAuthorize expressions you can then use:
 *   @PreAuthorize("@tenantBean.isMember(#id, authentication)")
 *   @PreAuthorize("hasAuthority('ROLE_ADMIN')")
 */
public class Day117CustomClaims {

    /** Holds per-tenant, per-user claims extracted from a JWT. */
    public record TenantClaims(
            String tenantId,
            String userId,
            List<String> roles,
            Map<String, Object> custom) {

        public boolean hasRole(String role) {
            return roles != null && roles.contains(role);
        }

        public boolean isSameTenant(String otherTenantId) {
            return tenantId != null && tenantId.equals(otherTenantId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Extractor
    // ─────────────────────────────────────────────────────────────────────────

    public static class TenantClaimsExtractor {

        /**
         * Extracts {@link TenantClaims} from a raw JWT claims map
         * (as received from Spring Security's Jwt.getClaims()).
         */
        @SuppressWarnings("unchecked")
        public TenantClaims extract(Map<String, Object> jwtClaims) {
            String tenantId = (String) jwtClaims.get("tenantId");
            String userId   = (String) jwtClaims.getOrDefault("sub", "");

            // Roles may be stored as List<String> or as a space-separated scope string
            List<String> roles = new ArrayList<>();
            Object rolesObj = jwtClaims.get("roles");
            if (rolesObj instanceof List<?> list) {
                list.forEach(r -> roles.add(Objects.toString(r)));
            }
            // Also pull from "scope" (space-separated)
            Object scopeObj = jwtClaims.get("scope");
            if (scopeObj instanceof String scope) {
                for (String s : scope.split(" ")) {
                    if (!s.isBlank()) roles.add(s);
                }
            }

            // Custom = everything except standard claims
            Map<String, Object> custom = new java.util.HashMap<>(jwtClaims);
            for (String std : List.of("sub","iss","iat","exp","aud","jti","nbf","roles","scope","tenantId")) {
                custom.remove(std);
            }

            return new TenantClaims(tenantId, userId, roles, custom);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Expression builders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a @PreAuthorize expression that checks a custom claim from the JWT
     * principal matches a method parameter.
     *
     * Example: claimMatchesParam("tenantId", "tid")
     *   → "authentication.principal.claims['tenantId'] == #tid"
     */
    public static String claimMatchesParam(String claim, String paramName) {
        return "authentication.principal.claims['" + claim + "'] == #" + paramName;
    }

    /**
     * Builds a @PreAuthorize expression that delegates to a bean.
     *
     * Example: beanCheck("tenantGuard", "isMember", "tenantId")
     *   → "@tenantGuard.isMember(#tenantId, authentication)"
     */
    public static String beanCheck(String beanName, String method, String paramName) {
        return "@" + beanName + "." + method + "(#" + paramName + ", authentication)";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Common multi-tenant claim layout reference
    // ─────────────────────────────────────────────────────────────────────────

    public record ClaimDefinition(String claimName, String type, String purpose) {}

    public static List<ClaimDefinition> recommendedCustomClaims() {
        return List.of(
            new ClaimDefinition("tenantId",    "String",       "Tenant identifier for row-level security"),
            new ClaimDefinition("roles",       "List<String>", "Application roles (ROLE_ADMIN, ROLE_USER)"),
            new ClaimDefinition("permissions", "List<String>", "Fine-grained scopes (read:orders)"),
            new ClaimDefinition("tier",        "String",       "Subscription tier (free, premium, enterprise)"),
            new ClaimDefinition("orgId",       "String",       "Organisation identifier for B2B apps")
        );
    }
}
