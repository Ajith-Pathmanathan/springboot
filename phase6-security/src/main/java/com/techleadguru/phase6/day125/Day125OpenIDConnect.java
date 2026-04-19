package com.techleadguru.phase6.day125;

import com.techleadguru.phase6.day113.Day113JwtStructure;

import java.util.List;
import java.util.Map;

/**
 * Day 125 — OpenID Connect: ID token, userinfo endpoint.
 *
 * OpenID Connect (OIDC) is a thin identity layer on top of OAuth 2.0.
 * OAuth 2.0 provides AUTHORIZATION (can this app access this resource?).
 * OIDC adds AUTHENTICATION (who is this user?).
 *
 * Key additions OIDC makes to OAuth 2.0:
 *  - ID Token   — a JWT containing identity claims (who the user is)
 *  - UserInfo   — /userinfo endpoint to fetch additional claims
 *  - Discovery  — /.well-known/openid-configuration auto-discovery
 *  - Standard scopes: openid, profile, email, address, phone
 *
 * The ID token MUST be validated (signature, iss, aud, exp, nonce).
 * Use it to establish identity ONCE at login, then store locally.
 */
public class Day125OpenIDConnect {

    // ─────────────────────────────────────────────────────────────────────────
    // Standard OIDC claims
    // ─────────────────────────────────────────────────────────────────────────

    /** Strongly-typed view of standard OIDC user claims. */
    public record OidcClaims(
            String sub,        // Subject — unique user identifier
            String name,       // Full name
            String givenName,  // First name
            String familyName, // Last name
            String email,
            boolean emailVerified,
            String picture,    // Profile picture URL
            String locale,
            List<String> audience) {}

    /** Extracts standard OIDC claims from a decoded JWT payload map. */
    @SuppressWarnings("unchecked")
    public static OidcClaims extractClaims(Map<String, Object> payload) {
        return new OidcClaims(
                (String) payload.getOrDefault("sub",          ""),
                (String) payload.getOrDefault("name",         ""),
                (String) payload.getOrDefault("given_name",   ""),
                (String) payload.getOrDefault("family_name",  ""),
                (String) payload.getOrDefault("email",        ""),
                Boolean.TRUE.equals(payload.get("email_verified")),
                (String) payload.getOrDefault("picture",      ""),
                (String) payload.getOrDefault("locale",       ""),
                payload.get("aud") instanceof List<?> l
                        ? l.stream().map(Object::toString).toList()
                        : List.of()
        );
    }

    /** Decodes the ID token JWT and extracts OIDC claims (no signature check — education only). */
    public static OidcClaims decodeIdToken(String idToken) {
        Day113JwtStructure.JwtPayload payload = Day113JwtStructure.decodePayload(idToken);
        Map<String, Object> allClaims = new java.util.HashMap<>(payload.claims());
        allClaims.put("sub", payload.sub());
        allClaims.put("iss", payload.iss());
        allClaims.put("exp", payload.exp());
        allClaims.put("iat", payload.iat());
        return extractClaims(allClaims);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Standard scopes
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> standardScopes() {
        return List.of("openid", "profile", "email", "address", "phone");
    }

    /** Returns true if the required 'openid' scope is present. */
    public static boolean isOidcRequest(List<String> requestedScopes) {
        return requestedScopes != null && requestedScopes.contains("openid");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Standard OIDC endpoints
    // ─────────────────────────────────────────────────────────────────────────

    public record OidcEndpoint(String name, String path, String purpose) {}

    public static List<OidcEndpoint> standardEndpoints() {
        return List.of(
            new OidcEndpoint("Discovery",       "/.well-known/openid-configuration",
                "JSON document with all provider endpoints and supported features"),
            new OidcEndpoint("Authorization",   "/oauth2/authorize",
                "Starts the auth flow; redirects user for login + consent"),
            new OidcEndpoint("Token",           "/oauth2/token",
                "Exchanges auth code or refresh token for access/id tokens"),
            new OidcEndpoint("UserInfo",        "/userinfo",
                "Returns additional user claims using a valid access token"),
            new OidcEndpoint("JWKS",            "/oauth2/jwks",
                "Public keys for verifying JWT signatures"),
            new OidcEndpoint("End Session",     "/logout",
                "Single logout — ends session at the provider")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OIDC vs OAuth 2.0 comparison
    // ─────────────────────────────────────────────────────────────────────────

    public record Comparison(String aspect, String oauth2, String oidc) {}

    public static List<Comparison> oidcVsOauth2() {
        return List.of(
            new Comparison("Purpose",         "Delegated authorization",            "Authentication + authorization"),
            new Comparison("Token returned",  "Access token only",                  "Access token + ID token"),
            new Comparison("User identity",   "No standard way",                    "sub claim in ID token"),
            new Comparison("Discovery",       "No standard URI",                    "/.well-known/openid-configuration"),
            new Comparison("Logout",          "No standard",                        "/logout with post_logout_redirect_uri"),
            new Comparison("Required scope",  "application-specific",               "'openid' scope required")
        );
    }
}
