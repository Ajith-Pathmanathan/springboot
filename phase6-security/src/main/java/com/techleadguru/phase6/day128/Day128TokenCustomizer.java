package com.techleadguru.phase6.day128;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Day 128 — OAuth2TokenCustomizer: add roles to token.
 *
 * Spring Authorization Server allows you to customize the claims in access tokens
 * and ID tokens via OAuth2TokenCustomizer<T>.
 *
 * Common customizations:
 *  - Add user roles as a "roles" claim
 *  - Add tenantId for multi-tenant systems
 *  - Add user tier (free/premium) for rate limiting
 *
 * Wiring (for JWT access tokens):
 *   @Bean
 *   OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
 *       return context -> {
 *           if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
 *               Set<String> roles = userRoleService.getRolesFor(context.getPrincipal().getName());
 *               context.getClaims().claim("roles", roles);
 *           }
 *       };
 *   }
 */
public class Day128TokenCustomizer {

    /** Describes a custom claim added to the JWT. */
    public record CustomClaim(String name, String type, String purpose, Object sampleValue) {}

    /** Returns a catalogue of recommended custom claims. */
    public static List<CustomClaim> recommendedCustomClaims() {
        return List.of(
            new CustomClaim("roles",       "List<String>",
                "Application roles for authorization",
                List.of("ROLE_USER", "ROLE_ADMIN")),
            new CustomClaim("tenantId",    "String",
                "Multi-tenant identifier for row-level security",
                "acme-corp"),
            new CustomClaim("tier",        "String",
                "Subscription tier for feature gating",
                "premium"),
            new CustomClaim("permissions", "List<String>",
                "Fine-grained operation permissions",
                List.of("read:orders", "write:orders")),
            new CustomClaim("orgId",       "String",
                "Organisation identifier for B2B apps",
                "org-123")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RolesClaimCustomizer — pure Java, no Spring context needed
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds a "roles" claim to a claims map.
     * In production, inject this logic into OAuth2TokenCustomizer<JwtEncodingContext>.
     */
    public static class RolesClaimCustomizer {

        private final Function<String, List<String>> roleProvider;

        public RolesClaimCustomizer(Function<String, List<String>> roleProvider) {
            this.roleProvider = roleProvider;
        }

        /**
         * Returns a new claims map with the "roles" claim added for the given subject.
         * Leaves all other claims unchanged.
         */
        public Map<String, Object> addRolesToClaims(String subject, Map<String, Object> existingClaims) {
            Map<String, Object> result = new HashMap<>(existingClaims);
            result.put("roles", roleProvider.apply(subject));
            return result;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MultiClaimCustomizer — adds multiple custom claims at once
    // ─────────────────────────────────────────────────────────────────────────

    public static class MultiClaimCustomizer {

        public record UserProfile(String userId, List<String> roles, String tenantId, String tier) {}

        /**
         * Merges a user profile into an existing claims map.
         * In practice this runs inside OAuth2TokenCustomizer.customize().
         */
        public Map<String, Object> addProfileClaims(
                UserProfile profile, Map<String, Object> existingClaims) {
            Map<String, Object> result = new HashMap<>(existingClaims);
            result.put("roles",    profile.roles());
            result.put("tenantId", profile.tenantId());
            result.put("tier",     profile.tier());
            return result;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token type guidance
    // ─────────────────────────────────────────────────────────────────────────

    public record TokenTypeGuidance(String tokenType, String claimsToAdd, String claimsToAvoid) {}

    public static List<TokenTypeGuidance> tokenTypeGuidance() {
        return List.of(
            new TokenTypeGuidance("ACCESS_TOKEN",
                "roles, tenantId, scope, tier",
                "password, ssn, credit card — sensitive PII"),
            new TokenTypeGuidance("ID_TOKEN",
                "name, email, given_name, family_name, picture",
                "roles, internal IDs — keep ID token focused on identity"),
            new TokenTypeGuidance("REFRESH_TOKEN",
                "(no additional claims — refresh tokens are opaque or minimal)",
                "all application claims — these belong in the access token")
        );
    }
}
