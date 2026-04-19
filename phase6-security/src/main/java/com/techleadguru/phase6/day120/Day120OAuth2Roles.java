package com.techleadguru.phase6.day120;

import java.util.List;

/**
 * Day 120 — OAuth 2.0 roles: draw flow diagram from memory.
 *
 * The OAuth 2.0 spec defines four roles that interact in every grant flow:
 *
 *  ┌─────────────────────────────────────────────────────────────────────┐
 *  │  RESOURCE OWNER  →  AUTHORIZATION SERVER  →  CLIENT                │
 *  │                                       ↓                             │
 *  │                                  RESOURCE SERVER                    │
 *  └─────────────────────────────────────────────────────────────────────┘
 *
 *  Resource Owner      — the end user who owns the data
 *  Client              — your application requesting access on behalf of the user
 *  Authorization Server— issues tokens after the user authenticates and consents
 *  Resource Server     — hosts the protected data; validates tokens
 */
public class Day120OAuth2Roles {

    // ─────────────────────────────────────────────────────────────────────────
    // Roles
    // ─────────────────────────────────────────────────────────────────────────

    public enum OAuth2Role {
        RESOURCE_OWNER,
        CLIENT,
        AUTHORIZATION_SERVER,
        RESOURCE_SERVER
    }

    /** Describes one OAuth 2.0 role. */
    public record RoleDescription(
            OAuth2Role role,
            String description,
            String realWorldExample) {}

    public static List<RoleDescription> roleDescriptions() {
        return List.of(
            new RoleDescription(OAuth2Role.RESOURCE_OWNER,
                "The entity that can grant access to the protected resource (usually the end user)",
                "Alice on her phone wanting to share Google Calendar with a scheduling app"),
            new RoleDescription(OAuth2Role.CLIENT,
                "The application requesting access to a resource on behalf of the resource owner",
                "The scheduling app (e.g., Calendly) requesting calendar read access"),
            new RoleDescription(OAuth2Role.AUTHORIZATION_SERVER,
                "Issues access tokens after authenticating the resource owner and obtaining consent",
                "Google's OAuth 2.0 server (accounts.google.com)"),
            new RoleDescription(OAuth2Role.RESOURCE_SERVER,
                "Hosts the protected resources; validates the access token on each request",
                "Google Calendar API (calendar.google.com)")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Grant types
    // ─────────────────────────────────────────────────────────────────────────

    public enum GrantType {
        AUTHORIZATION_CODE,
        AUTHORIZATION_CODE_PKCE,
        CLIENT_CREDENTIALS,
        REFRESH_TOKEN,
        DEVICE_CODE
    }

    /** Describes one OAuth 2.0 grant type. */
    public record GrantTypeInfo(
            GrantType type,
            String useCase,
            boolean requiresPKCE,
            boolean requiresUserInteraction) {}

    public static List<GrantTypeInfo> grantTypeGuide() {
        return List.of(
            new GrantTypeInfo(GrantType.AUTHORIZATION_CODE,
                "Web apps with a back-end server (confidential client)",
                false, true),
            new GrantTypeInfo(GrantType.AUTHORIZATION_CODE_PKCE,
                "SPAs and mobile apps (public clients without a secure server secret)",
                true, true),
            new GrantTypeInfo(GrantType.CLIENT_CREDENTIALS,
                "Machine-to-machine (service accounts, batch jobs, microservices)",
                false, false),
            new GrantTypeInfo(GrantType.REFRESH_TOKEN,
                "Obtain a new access token silently using a long-lived refresh token",
                false, false),
            new GrantTypeInfo(GrantType.DEVICE_CODE,
                "Input-constrained devices (smart TVs, CLI tools) that can't open browsers",
                false, true)
        );
    }

    /** Returns grant types that require user interaction. */
    public static List<GrantType> userFacingGrantTypes() {
        return grantTypeGuide().stream()
                .filter(GrantTypeInfo::requiresUserInteraction)
                .map(GrantTypeInfo::type)
                .toList();
    }

    /** Returns grant types that require PKCE. */
    public static List<GrantType> pkceRequiredGrantTypes() {
        return grantTypeGuide().stream()
                .filter(GrantTypeInfo::requiresPKCE)
                .map(GrantTypeInfo::type)
                .toList();
    }
}
