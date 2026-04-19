package com.techleadguru.phase6.day127;

import java.util.List;
import java.util.Map;

/**
 * Day 127 — Spring Authorization Server: issue access tokens.
 *
 * Spring Authorization Server (SAS) is the official Spring project for building
 * OAuth 2.0 Authorization Servers. It implements OAuth 2.0 + OIDC.
 *
 * Minimum setup:
 *  1. Add spring-security-oauth2-authorization-server dependency
 *  2. Define a RegisteredClient (client ID, secret, grant types, scopes, redirect URIs)
 *  3. Define a JWKSource (RSA key for signing tokens)
 *  4. Apply OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)
 *
 * Key endpoints (all under /oauth2/* by default):
 *   /oauth2/authorize      — authorization endpoint
 *   /oauth2/token          — token endpoint
 *   /oauth2/jwks           — JWKS (public keys)
 *   /oauth2/introspect     — token introspection
 *   /oauth2/revoke         — token revocation
 *   /.well-known/openid-configuration — OIDC discovery
 */
public class Day127AuthorizationServer {

    // ─────────────────────────────────────────────────────────────────────────
    // Registered client configuration
    // ─────────────────────────────────────────────────────────────────────────

    /** A simplified view of a RegisteredClient configuration. */
    public record ClientConfig(
            String  clientId,
            String  clientSecret,
            List<String> grantTypes,
            List<String> scopes,
            List<String> redirectUris,
            boolean requirePkce) {}

    /** Returns a default local client suitable for development/testing. */
    public static ClientConfig defaultLocalClient() {
        return new ClientConfig(
                "demo-client",
                "{noop}secret",
                List.of("authorization_code", "refresh_token", "client_credentials"),
                List.of("openid", "profile", "email", "api.read", "api.write"),
                List.of("http://localhost:8080/login/oauth2/code/demo"),
                true
        );
    }

    /** Returns a service-to-service client (Client Credentials only). */
    public static ClientConfig serviceClient(String clientId) {
        return new ClientConfig(
                clientId,
                "{noop}service-secret",
                List.of("client_credentials"),
                List.of("api.read", "api.write"),
                List.of(),
                false
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Standard endpoints
    // ─────────────────────────────────────────────────────────────────────────

    public record EndpointInfo(String name, String path, String description) {}

    public static List<EndpointInfo> authServerEndpoints() {
        return List.of(
            new EndpointInfo("Authorization",  "/oauth2/authorize",
                "Starts browser-based auth flow; issues authorization code"),
            new EndpointInfo("Token",          "/oauth2/token",
                "Exchanges code / credentials for access/refresh/ID tokens"),
            new EndpointInfo("JWKS",           "/oauth2/jwks",
                "Public keys used to verify JWT signatures (RS256 public key)"),
            new EndpointInfo("Introspection",  "/oauth2/introspect",
                "Resource servers can verify opaque tokens or get token metadata"),
            new EndpointInfo("Revocation",     "/oauth2/revoke",
                "Invalidates access or refresh tokens"),
            new EndpointInfo("OIDC Discovery", "/.well-known/openid-configuration",
                "Discovery document: all endpoints + supported features")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup guide steps
    // ─────────────────────────────────────────────────────────────────────────

    public record SetupStep(int order, String title, String description) {}

    public static List<SetupStep> setupGuide() {
        return List.of(
            new SetupStep(1, "Add dependency",
                "spring-security-oauth2-authorization-server"),
            new SetupStep(2, "Configure SecurityFilterChain for the authorization server",
                "OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http)"),
            new SetupStep(3, "Register clients",
                "Define RegisteredClient beans (in-memory or via RegisteredClientRepository)"),
            new SetupStep(4, "Configure JWKSource",
                "Generate RSA key pair; expose JWKSet via JWKSource<SecurityContext> bean"),
            new SetupStep(5, "Configure ProviderSettings",
                "Set issuer URI: AuthorizationServerSettings.builder().issuer(url).build()"),
            new SetupStep(6, "Configure default security for the app",
                "Second SecurityFilterChain with form login for user authentication"),
            new SetupStep(7, "Test",
                "GET /.well-known/openid-configuration should return discovery document")
        );
    }

    /** Returns required application properties for the SAS issuer. */
    public static Map<String, String> issuerProperties(String issuerUri) {
        return Map.of("spring.security.oauth2.authorizationserver.issuer", issuerUri);
    }
}
