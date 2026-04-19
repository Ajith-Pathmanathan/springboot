package com.techleadguru.phase6.day114;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.List;

/**
 * Day 114 — RS256 vs HS256: sign with RSA key pair.
 *
 * HS256 (HMAC-SHA256)  — symmetric: same secret signs AND verifies.
 *   ✔ Simple; ✘ every service that needs to verify must hold the secret.
 *
 * RS256 (RSA-SHA256)   — asymmetric: private key signs, public key verifies.
 *   ✔ Auth server holds private key; resource servers only need the public key.
 *   ✔ Public key can be distributed via JWKS endpoint.
 *   ✘ Slightly more complex setup.
 *
 * Recommendation: use RS256 for microservices — resource servers never see
 *   the private key, so a compromised resource server cannot forge tokens.
 */
public class Day114JwtAlgorithms {

    public enum JwtAlgorithm { HS256, RS256, ES256 }

    /** Metadata about a JWT signing algorithm. */
    public record AlgorithmInfo(
            JwtAlgorithm algorithm,
            String keyType,
            String keyMaterial,
            String useCase,
            boolean supportsDistributedValidation) {}

    /** Returns metadata for each algorithm. */
    public static List<AlgorithmInfo> algorithmCatalog() {
        return List.of(
            new AlgorithmInfo(JwtAlgorithm.HS256, "Symmetric",
                "One shared secret (byte array of ≥256 bits)",
                "Internal monolith or single-service where all verifiers are trusted",
                false),
            new AlgorithmInfo(JwtAlgorithm.RS256, "Asymmetric RSA",
                "2048-bit RSA key pair (private to sign, public to verify)",
                "Microservices where resource servers should not hold the signing secret",
                true),
            new AlgorithmInfo(JwtAlgorithm.ES256, "Asymmetric ECDSA",
                "256-bit EC key pair (P-256 curve)",
                "Same as RS256 but smaller keys; preferred in mobile/IoT contexts",
                true)
        );
    }

    /** Returns the recommended algorithm for microservice architectures. */
    public static JwtAlgorithm recommendedForMicroservices() { return JwtAlgorithm.RS256; }

    // ─────────────────────────────────────────────────────────────────────────
    // HS256 helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Signs a JWT payload string with HS256.
     * Input is the raw header.payload string; output is the Base64URL signature.
     */
    public static String signHs256(String headerDotPayload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(headerDotPayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HS256 signing failed", e);
        }
    }

    /** Verifies an HS256 JWT by recomputing the signature and comparing. */
    public static boolean verifyHs256(String jwt, String secret) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) return false;
        String expectedSig = signHs256(parts[0] + "." + parts[1], secret);
        return MessageDigest.isEqual(
                expectedSig.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RS256 helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Generates an RSA 2048-bit key pair (for testing and educational demos). */
    public static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048, new SecureRandom());
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA key generation failed", e);
        }
    }

    /** Signs a header.payload string with an RSA private key (RS256). */
    public static String signRs256(String headerDotPayload, PrivateKey privateKey) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(headerDotPayload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());
        } catch (Exception e) {
            throw new RuntimeException("RS256 signing failed", e);
        }
    }

    /** Verifies an RS256 JWT using the RSA public key. */
    public static boolean verifyRs256(String jwt, PublicKey publicKey) {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) return false;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
            // Decode the signature
            int pad = parts[2].length() % 4;
            String paddedSig = parts[2] + (pad == 2 ? "==" : pad == 3 ? "=" : "");
            return sig.verify(Base64.getUrlDecoder().decode(paddedSig));
        } catch (Exception e) {
            return false;
        }
    }
}
