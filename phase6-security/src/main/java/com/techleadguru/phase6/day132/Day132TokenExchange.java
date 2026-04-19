package com.techleadguru.phase6.day132;

import java.util.List;
import java.util.UUID;

/**
 * Day 132 — Token exchange: impersonation + delegation (RFC 8693).
 *
 * Token Exchange allows one service to obtain a token on behalf of another
 * principal. Two patterns:
 *
 * DELEGATION:
 *   ServiceA acts on behalf of User.
 *   The token's principal is still User, but actor is ServiceA.
 *   Use case: ServiceA calls ServiceB but the downstream operation
 *   should be audited under the user's identity, not the service's.
 *
 * IMPERSONATION:
 *   ServiceA completely replaces the principal.
 *   The resulting token is as-if the actor IS the subject.
 *   Use case: admin tools performing actions as another user (with audit).
 *
 * RFC 8693 token exchange parameters:
 *   grant_type     = urn:ietf:params:oauth:grant-type:token-exchange
 *   subject_token  = original token (the user's token)
 *   actor_token    = token of the requesting service (optional)
 *   requested_token_type = urn:ietf:params:oauth:token-type:access_token
 *   scope          = scopes for the new token
 */
public class Day132TokenExchange {

    // ─────────────────────────────────────────────────────────────────────────
    // Exchange types
    // ─────────────────────────────────────────────────────────────────────────

    public enum ExchangeType { DELEGATION, IMPERSONATION }

    // ─────────────────────────────────────────────────────────────────────────
    // Request / Response
    // ─────────────────────────────────────────────────────────────────────────

    /** RFC 8693 token-exchange request. */
    public record TokenExchangeRequest(
            String       subjectToken,  // original identity
            String       actorToken,    // requesting service token (nullable for impersonation)
            ExchangeType type,
            String       scope) {}

    /** Result of a token exchange. */
    public record ExchangedToken(
            String       token,
            String       subject,   // original user (for delegation: same as input)
            String       actor,     // the service that got the delegated token (nullable)
            ExchangeType type) {}

    // ─────────────────────────────────────────────────────────────────────────
    // TokenExchangeService (simplified simulator)
    // ─────────────────────────────────────────────────────────────────────────

    public static class TokenExchangeService {

        /**
         * Performs a token exchange.
         *
         * DELEGATION: subject remains the user, actor is set to the requesting service.
         * IMPERSONATION: subject becomes the actor (actor identity replaces subject).
         */
        public ExchangedToken exchange(TokenExchangeRequest request) {
            // Simulate subject extraction from the subject token (just use the token itself as ID)
            String subject = "user-" + request.subjectToken().substring(0, 6);
            String actor   = request.actorToken() != null
                    ? "service-" + request.actorToken().substring(0, 6)
                    : null;

            String exchangedSubject = switch (request.type()) {
                case DELEGATION    -> subject;        // user is still the principal
                case IMPERSONATION -> actor != null
                        ? actor
                        : subject;                    // actor becomes the principal
            };

            return new ExchangedToken(
                    UUID.randomUUID().toString(),
                    exchangedSubject,
                    request.type() == ExchangeType.DELEGATION ? actor : null,
                    request.type()
            );
        }

        /** Returns true if the given token value is non-null and non-blank (basic validity check). */
        public boolean isValidForExchange(String token) {
            return token != null && !token.isBlank() && token.length() >= 6;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RFC 8693 parameter reference
    // ─────────────────────────────────────────────────────────────────────────

    public record TokenExchangeParameter(String name, String description, boolean required) {}

    public static List<TokenExchangeParameter> rfc8693Parameters() {
        return List.of(
            new TokenExchangeParameter("grant_type",
                "urn:ietf:params:oauth:grant-type:token-exchange", true),
            new TokenExchangeParameter("subject_token",
                "Security token representing the identity to be exchanged", true),
            new TokenExchangeParameter("subject_token_type",
                "urn:ietf:params:oauth:token-type:access_token (or jwt, saml2)", true),
            new TokenExchangeParameter("actor_token",
                "Security token representing the requesting party (for delegation)", false),
            new TokenExchangeParameter("actor_token_type",
                "Type of the actor_token", false),
            new TokenExchangeParameter("requested_token_type",
                "Type of the token to issue; defaults to access_token", false),
            new TokenExchangeParameter("scope",
                "Scopes for the issued token; may be subset of subject_token's scopes", false),
            new TokenExchangeParameter("audience",
                "Target service / resource the issued token is intended for", false)
        );
    }

    /** Returns the number of required RFC 8693 parameters. */
    public static long requiredParameterCount() {
        return rfc8693Parameters().stream().filter(TokenExchangeParameter::required).count();
    }
}
