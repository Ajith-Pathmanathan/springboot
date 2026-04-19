package com.techleadguru.phase6.day122;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Day 122 — PKCE: code_verifier + code_challenge (SHA-256).
 *
 * PKCE (Proof Key for Code Exchange, RFC 7636) was originally designed for
 * mobile apps and SPAs that cannot safely hold a client_secret.
 *
 * How it works:
 *  1. Client generates a random code_verifier (43–128 URL-safe characters)
 *  2. Client derives code_challenge = BASE64URL(SHA256(ASCII(code_verifier)))
 *  3. Client sends code_challenge + method=S256 in the authorization request
 *  4. Auth server stores code_challenge with the issued code
 *  5. On code exchange, client sends the original code_verifier
 *  6. Auth server recomputes SHA256(code_verifier) and compares → attacker
 *     who stole the code cannot exchange it without the verifier
 *
 * Best practice: use PKCE for ALL public clients (SPAs, mobile apps, CLIs).
 * Spring Authorization Server requires PKCE for public clients by default.
 */
public class Day122PKCE {

    public enum ChallengeMethod { PLAIN, S256 }

    /** The PKCE parameters generated for one authorization request. */
    public record PkceParameters(
            String codeVerifier,
            String codeChallenge,
            ChallengeMethod method) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Generation
    // ─────────────────────────────────────────────────────────────────────────

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a cryptographically random code_verifier (48 random bytes,
     * Base64URL-encoded → 64 chars, within the 43–128 char requirement).
     */
    public static String generateCodeVerifier() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Derives the S256 code_challenge from the given code_verifier.
     * code_challenge = BASE64URL(SHA256(ASCII(code_verifier)))
     */
    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Generates a new {@link PkceParameters} instance with a fresh verifier. */
    public static PkceParameters generate() {
        String verifier   = generateCodeVerifier();
        String challenge  = generateCodeChallenge(verifier);
        return new PkceParameters(verifier, challenge, ChallengeMethod.S256);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verification (Authorization Server side)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies that the given code_verifier matches the stored code_challenge.
     * Returns true if they match (safe to exchange the code).
     */
    public static boolean verify(String codeVerifier, String storedCodeChallenge) {
        if (codeVerifier == null || storedCodeChallenge == null) return false;
        String computed = generateCodeChallenge(codeVerifier);
        // Constant-time comparison to avoid timing attacks
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                storedCodeChallenge.getBytes(StandardCharsets.UTF_8));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation rules
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns true if the verifier length is within RFC 7636 limits (43–128). */
    public static boolean isValidVerifierLength(String verifier) {
        return verifier != null && verifier.length() >= 43 && verifier.length() <= 128;
    }
}
