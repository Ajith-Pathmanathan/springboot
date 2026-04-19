package com.techleadguru.phase6.day113;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Day 113 — JWT structure: decode manually, no library.
 *
 * A JWT is three Base64URL-encoded JSON parts separated by dots:
 *   base64url(header) . base64url(payload) . base64url(signature)
 *
 * The header declares the algorithm and token type.
 * The payload contains claims (sub, iss, iat, exp, custom).
 * The signature is computed from header+payload using the algorithm.
 *
 * By decoding manually you understand exactly what a JWT contains —
 * critical for debugging and security reviews.
 *
 * ⚠️  Never skip signature validation in production code!
 *     This class is for education only — use spring-security-oauth2-resource-server
 *     or nimbus-jose-jwt for production JWT validation.
 */
public class Day113JwtStructure {

    // ─────────────────────────────────────────────────────────────────────────
    // Records
    // ─────────────────────────────────────────────────────────────────────────

    /** The three raw Base64URL parts of a JWT string. */
    public record JwtParts(String headerB64, String payloadB64, String signatureB64) {}

    /** Decoded JWT header. */
    public record JwtHeader(String alg, String typ) {}

    /** Decoded JWT payload with standard claims. */
    public record JwtPayload(
            String sub,
            String iss,
            long   iat,
            long   exp,
            Map<String, Object> claims) {

        public boolean isExpired() {
            return Instant.now().getEpochSecond() > exp;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Manual decoding
    // ─────────────────────────────────────────────────────────────────────────

    /** Splits a JWT string into its three Base64URL parts. */
    public static JwtParts split(String jwt) {
        String[] parts = jwt.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Not a valid JWT: expected 3 parts, got " + parts.length);
        }
        return new JwtParts(parts[0], parts[1], parts[2]);
    }

    /** Decodes the header part of a JWT. */
    public static JwtHeader decodeHeader(String jwt) {
        JwtParts parts = split(jwt);
        String json = b64Decode(parts.headerB64());
        return new JwtHeader(
                extractString(json, "alg"),
                extractString(json, "typ"));
    }

    /** Decodes the payload part of a JWT. */
    @SuppressWarnings("unchecked")
    public static JwtPayload decodePayload(String jwt) {
        JwtParts parts = split(jwt);
        String json = b64Decode(parts.payloadB64());

        Map<String, Object> allClaims = simpleJsonParse(json);
        long iat = toLong(allClaims.get("iat"));
        long exp = toLong(allClaims.get("exp"));
        String sub = (String) allClaims.getOrDefault("sub", "");
        String iss = (String) allClaims.getOrDefault("iss", "");

        // Custom claims = everything not in standard set
        Map<String, Object> custom = new HashMap<>(allClaims);
        custom.remove("sub"); custom.remove("iss"); custom.remove("iat");
        custom.remove("exp"); custom.remove("aud"); custom.remove("jti");
        custom.remove("nbf");

        return new JwtPayload(sub, iss, iat, exp, custom);
    }

    /** Returns true if the token is expired (based on the 'exp' claim). */
    public static boolean isExpired(String jwt) {
        return decodePayload(jwt).isExpired();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token building (unsigned — education only)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds an unsigned JWT with alg=none.
     * ⚠️  This must NEVER be accepted by a secure validator (see Day 116).
     */
    public static String buildUnsignedJwt(String subject, String issuer, long expEpochSec) {
        String header  = b64Encode("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = b64Encode(String.format(
                "{\"sub\":\"%s\",\"iss\":\"%s\",\"iat\":%d,\"exp\":%d}",
                subject, issuer, Instant.now().getEpochSecond(), expEpochSec));
        return header + "." + payload + ".";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers  (minimal JSON parsing — no external library)
    // ─────────────────────────────────────────────────────────────────────────

    private static String b64Decode(String base64Url) {
        // Add padding if needed
        int pad = base64Url.length() % 4;
        if (pad == 2) base64Url += "==";
        else if (pad == 3) base64Url += "=";
        byte[] bytes = Base64.getUrlDecoder().decode(base64Url);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static String b64Encode(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Rudimentary JSON string-field extractor (for strings only). */
    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    /** Very minimal JSON-to-Map for flat objects (supports string and numeric values). */
    static Map<String, Object> simpleJsonParse(String json) {
        Map<String, Object> result = new HashMap<>();
        // Remove outer braces
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        // Split on commas that are not inside strings (best-effort for flat objects)
        // This is intentionally simple — do not use for nested JSON
        int depth = 0; int bracketDepth = 0; boolean inStr = false;
        StringBuilder current = new StringBuilder();
        for (char c : json.toCharArray()) {
            if (c == '"' && depth == 0) inStr = !inStr;
            else if (!inStr) {
                if (c == '[') bracketDepth++;
                else if (c == ']') bracketDepth--;
            }
            if (c == ',' && !inStr && depth == 0 && bracketDepth == 0) {
                parseEntry(current.toString().trim(), result);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) parseEntry(current.toString().trim(), result);
        return result;
    }

    private static void parseEntry(String entry, Map<String, Object> result) {
        int colon = entry.indexOf(':');
        if (colon == -1) return;
        String key   = entry.substring(0, colon).trim().replace("\"", "");
        String value = entry.substring(colon + 1).trim();
        if (value.startsWith("\"")) {
            result.put(key, value.substring(1, value.length() - 1));
        } else if (value.equals("true"))  { result.put(key, Boolean.TRUE);
        } else if (value.equals("false")) { result.put(key, Boolean.FALSE);
        } else {
            try { result.put(key, Long.parseLong(value)); }
            catch (NumberFormatException e) { result.put(key, value); }
        }
    }

    private static long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0L; }
    }
}
