package com.techleadguru.phase6.day118;

import java.util.List;

/**
 * Day 118 — Stateless vs stateful: horizontal scaling impact.
 *
 * Stateful authentication:
 *   Server keeps a session (in memory or Redis).
 *   Client sends a session cookie; server looks up session on every request.
 *   Problem: sticky sessions or shared session store needed for horizontal scaling.
 *
 * Stateless authentication (JWT):
 *   All state is inside the signed token.
 *   Server validates signature only — no DB/session lookup.
 *   Benefits: horizontal scaling trivial, no shared session store.
 *   Challenges: token revocation, longer token payload.
 *
 * In practice: start stateless, add Redis-backed token blacklist (Day 119)
 *   for revocation rather than reverting to stateful sessions.
 */
public class Day118StatelessVsStateful {

    public enum SessionStrategy { STATEFUL, STATELESS }

    /** Compares one aspect of stateful vs stateless authentication. */
    public record SessionTrade(
            String consideration,
            String stateful,
            String stateless) {}

    /** Returns a comparison table of the two strategies. */
    public static List<SessionTrade> comparisonTable() {
        return List.of(
            new SessionTrade("Session storage",
                "Server-side (JVM memory or Redis)",
                "Client-side (JWT payload)"),
            new SessionTrade("Horizontal scaling",
                "Needs sticky sessions or shared session store",
                "Trivially scalable — any node validates the token"),
            new SessionTrade("Token revocation",
                "Simple: delete session from store",
                "Harder: need token blacklist or short expiry"),
            new SessionTrade("Server memory per user",
                "High — each active session occupies heap",
                "Zero — token lives on client side"),
            new SessionTrade("Token payload size",
                "Small cookie (~20 bytes session ID)",
                "Larger (1–2 KB depending on claims)"),
            new SessionTrade("CSRF risk",
                "Yes — session cookie is auto-sent",
                "No — Authorization header is not auto-sent"),
            new SessionTrade("Visibility into active sessions",
                "High — session store has complete list",
                "Low — no central list; must track via refresh tokens"),
            new SessionTrade("Audit / forced logout",
                "Easy — revoke session server-side",
                "Requires blacklist entry or short-lived tokens")
        );
    }

    public static SessionStrategy recommendedForHorizontalScaling() {
        return SessionStrategy.STATELESS;
    }

    public static List<String> statelessBenefits() {
        return List.of(
            "No shared session store needed",
            "Any server instance can validate the token independently",
            "Reduced per-request DB/Redis round trips",
            "Works well for microservices and API gateways",
            "No CSRF risk when using Authorization header"
        );
    }

    public static List<String> statelessChallenges() {
        return List.of(
            "Immediate token revocation requires a blacklist (adds statefulness back)",
            "Larger token on every request (bandwidth)",
            "Claims in token are immutable until it expires (role changes not instant)",
            "Storing refresh tokens server-side adds partial statefulness"
        );
    }

    /** Returns the Spring Security session policy name for stateless APIs. */
    public static String statelessSessionPolicy() {
        return "SessionCreationPolicy.STATELESS";
    }
}
