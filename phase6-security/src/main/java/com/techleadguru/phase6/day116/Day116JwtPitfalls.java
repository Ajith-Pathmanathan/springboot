package com.techleadguru.phase6.day116;

import com.techleadguru.phase6.day113.Day113JwtStructure;

import java.util.List;
import java.util.Set;

/**
 * Day 116 — JWT security pitfalls: alg:none attack ⚠️
 *
 * VULNERABILITY: Some early JWT libraries accepted a token whose header
 * declared "alg":"none" and had no signature.  An attacker could
 * forge any payload (e.g., change userId or role) and the server would
 * accept it as valid.
 *
 * ATTACK steps:
 *  1. Obtain a valid JWT
 *  2. Decode the header+payload (Base64URL)
 *  3. Modify the payload (e.g., set "admin":true)
 *  4. Replace the header's "alg" with "none"
 *  5. Remove the signature (or leave empty after final dot)
 *  6. Send the crafted token — vulnerable server accepts it
 *
 * DEFENSE:
 *  - Whitelist allowed algorithms explicitly
 *  - Reject any token that declares alg=none
 *  - Never accept a signature-less token unless the algorithm is known secure
 */
public class Day116JwtPitfalls {

    /** Describes a known JWT vulnerability. */
    public record JwtVulnerability(
            String name,
            String cve,
            String description,
            String attackScenario,
            String defense) {}

    /** Returns a catalogue of known JWT vulnerabilities. */
    public static List<JwtVulnerability> knownVulnerabilities() {
        return List.of(
            new JwtVulnerability(
                "alg:none attack",
                "CVE-2015-9235",
                "Library accepts 'none' algorithm and skips signature verification",
                "Attacker modifies payload, sets alg=none, removes signature",
                "Whitelist allowed algorithms; explicitly reject 'none'"),
            new JwtVulnerability(
                "RS256 → HS256 confusion",
                "CVE-2016-5431",
                "Server uses RSA public key as HMAC secret when alg is switched to HS256",
                "Attacker changes header to HS256, signs with the public key",
                "Validate algorithm before choosing verification method; never switch algorithms"),
            new JwtVulnerability(
                "Weak HS256 secret",
                "N/A",
                "Short or guessable secrets can be brute-forced",
                "Offline dictionary attack to recover the signing secret",
                "Use ≥256-bit random secrets; prefer RS256 for public APIs"),
            new JwtVulnerability(
                "Missing exp claim validation",
                "N/A",
                "Server never checks the expiry time, so stolen tokens work forever",
                "Steal access token and replay it indefinitely",
                "Always validate exp; keep access tokens short-lived (15 min)")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vulnerable validator — educational purpose only
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ⚠️ VULNERABLE — accepts any algorithm including 'none'.
     * Demonstrates the WRONG approach for educational contrast.
     */
    public static boolean vulnerableValidate(String jwt) {
        try {
            // Just checks the token has 3 dot-separated parts — no signature check!
            String[] parts = jwt.split("\\.");
            return parts.length == 3 || (parts.length == 2 && jwt.endsWith("."));
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Secure validator
    // ─────────────────────────────────────────────────────────────────────────

    public static class SecureJwtValidator {

        private static final Set<String> ALLOWED_ALGORITHMS = Set.of("RS256", "HS256", "ES256");

        /** Returns true when the token's algorithm is in the allowed set. */
        public static boolean isAlgorithmAllowed(String algorithm) {
            return ALLOWED_ALGORITHMS.contains(algorithm);
        }

        /**
         * Returns true if the token is attempting an alg:none attack.
         * (i.e., the header declares "none" as the algorithm)
         */
        public static boolean detectsAlgNoneAttack(String jwt) {
            try {
                Day113JwtStructure.JwtHeader header = Day113JwtStructure.decodeHeader(jwt);
                return "none".equalsIgnoreCase(header.alg())
                        || header.alg() == null
                        || header.alg().isBlank();
            } catch (Exception e) {
                return true; // treat malformed tokens as attacks
            }
        }

        /**
         * Validates the token's algorithm before any signature check.
         * If the algorithm is not on the whitelist, throws an exception.
         */
        public static void assertAlgorithmAllowed(String jwt) {
            Day113JwtStructure.JwtHeader header = Day113JwtStructure.decodeHeader(jwt);
            String alg = header.alg();
            if (!isAlgorithmAllowed(alg)) {
                throw new SecurityException(
                        "Rejected JWT with disallowed algorithm: '" + alg + "'");
            }
        }

        public static Set<String> allowedAlgorithms() { return ALLOWED_ALGORITHMS; }
    }
}
