package com.techleadguru.phase7.day139;

import java.util.*;

/**
 * Day 139 — JWT validation at the API Gateway
 *
 * The gateway validates JWT tokens before forwarding requests to downstream services.
 * Downstream services receive pre-validated metadata via propagated headers.
 *
 * JWT structure: {header}.{payload}.{signature}
 * - Header: alg, typ
 * - Payload: sub, iss, aud, exp, iat, roles
 * - Signature: HMAC-SHA256 or RSA
 */
public class Day139JwtAtGateway {

    // ─────────────────────────────────────────────────────────────────────────
    // Validation result
    // ─────────────────────────────────────────────────────────────────────────

    public record TokenValidationResult(
            boolean  valid,
            String   subject,    // JWT sub claim (userId or email)
            List<String> roles,
            String   reason) {

        public static TokenValidationResult ok(String subject, List<String> roles) {
            return new TokenValidationResult(true, subject, roles, null);
        }

        public static TokenValidationResult fail(String reason) {
            return new TokenValidationResult(false, null, List.of(), reason);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Simplified JWT validator (no real crypto — demonstrates the steps)
    // ─────────────────────────────────────────────────────────────────────────

    public static class GatewayJwtValidator {

        private final String expectedIssuer;
        private final String expectedAudience;

        public GatewayJwtValidator(String expectedIssuer, String expectedAudience) {
            this.expectedIssuer   = expectedIssuer;
            this.expectedAudience = expectedAudience;
        }

        /**
         * Validates a Bearer token from an Authorization header value.
         * Token format (test-only, NOT real JWT):
         *   "sub=alice;iss=myapp;aud=api;exp={ms};roles=ROLE_USER,ROLE_ADMIN"
         */
        public TokenValidationResult validate(String authorizationHeader, long nowMs) {
            if (authorizationHeader == null || authorizationHeader.isBlank()) {
                return TokenValidationResult.fail("Missing Authorization header");
            }
            if (!authorizationHeader.startsWith("Bearer ")) {
                return TokenValidationResult.fail("Authorization header must be Bearer token");
            }
            String token = authorizationHeader.substring(7).trim();
            if (token.isEmpty()) {
                return TokenValidationResult.fail("Empty token");
            }

            // Parse test token: key=value;key=value
            Map<String, String> claims = new HashMap<>();
            for (String part : token.split(";")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) claims.put(kv[0].trim(), kv[1].trim());
            }

            String sub = claims.get("sub");
            if (sub == null || sub.isBlank()) {
                return TokenValidationResult.fail("Missing subject claim");
            }

            String iss = claims.get("iss");
            if (!expectedIssuer.equals(iss)) {
                return TokenValidationResult.fail("Invalid issuer: " + iss);
            }

            String aud = claims.get("aud");
            if (!expectedAudience.equals(aud)) {
                return TokenValidationResult.fail("Invalid audience: " + aud);
            }

            String expStr = claims.get("exp");
            if (expStr != null) {
                try {
                    long expMs = Long.parseLong(expStr);
                    if (nowMs > expMs) {
                        return TokenValidationResult.fail("Token expired");
                    }
                } catch (NumberFormatException e) {
                    return TokenValidationResult.fail("Invalid exp claim format");
                }
            }

            String rolesStr = claims.getOrDefault("roles", "");
            List<String> roles = rolesStr.isBlank()
                    ? List.of()
                    : Arrays.asList(rolesStr.split(","));

            return TokenValidationResult.ok(sub, roles);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Route auth policies
    // ─────────────────────────────────────────────────────────────────────────

    public enum AuthPolicy { PUBLIC, AUTHENTICATED, ROLE_REQUIRED }

    public record RouteAuthPolicy(
            String routeId,
            AuthPolicy policy,
            String requiredRole) {}

    public static Map<String, RouteAuthPolicy> sampleRoutePolicies() {
        Map<String, RouteAuthPolicy> map = new LinkedHashMap<>();
        map.put("health",    new RouteAuthPolicy("health",    AuthPolicy.PUBLIC,         null));
        map.put("login",     new RouteAuthPolicy("login",     AuthPolicy.PUBLIC,         null));
        map.put("orders",    new RouteAuthPolicy("orders",    AuthPolicy.AUTHENTICATED,  null));
        map.put("admin",     new RouteAuthPolicy("admin",     AuthPolicy.ROLE_REQUIRED,  "ROLE_ADMIN"));
        map.put("reports",   new RouteAuthPolicy("reports",   AuthPolicy.ROLE_REQUIRED,  "ROLE_ANALYST"));
        return map;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation step guide
    // ─────────────────────────────────────────────────────────────────────────

    public record JwtValidationStep(int order, String step, String detail) {}

    public static List<JwtValidationStep> validationSteps() {
        return List.of(
            new JwtValidationStep(1,  "Extract token",      "Read Authorization header, strip 'Bearer ' prefix"),
            new JwtValidationStep(2,  "Decode header",      "Base64url-decode header; check alg is not 'none'"),
            new JwtValidationStep(3,  "Decode payload",     "Base64url-decode payload claims"),
            new JwtValidationStep(4,  "Verify signature",   "Validate HMAC/RSA signature using public key"),
            new JwtValidationStep(5,  "Check expiry",       "Confirm current time < exp claim"),
            new JwtValidationStep(6,  "Check nbf",          "Confirm current time >= nbf (not-before) if present"),
            new JwtValidationStep(7,  "Check issuer",       "Confirm iss matches expected issuer"),
            new JwtValidationStep(8,  "Check audience",     "Confirm aud contains expected audience"),
            new JwtValidationStep(9,  "Extract roles",      "Read roles/authorities from claims"),
            new JwtValidationStep(10, "Propagate headers",  "Forward X-User-Id, X-User-Roles to downstream")
        );
    }
}
