package com.techleadguru.phase6.day110;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * Day 110 — BCrypt cost factor: brute force vs latency trade-off.
 *
 * BCrypt is the recommended password hashing algorithm for Spring Security.
 * The cost factor (strength) controls the number of hashing rounds: 2^strength.
 *
 * Trade-off:
 *   Higher cost → harder to brute force, but higher latency per login.
 *   Target: ~100–300ms per hash on your production hardware.
 *   OWASP recommends cost factor ≥ 10 (aim for 12 in 2024).
 *
 * Key rule: always re-evaluate as hardware gets faster. BCrypt upgrades
 *   gracefully — verify with current factor, re-hash if cost is too low.
 */
public class Day110BCryptCostFactor {

    /**
     * Benchmark entry for a given BCrypt strength.
     *
     * @param strength          BCrypt cost factor (4–31)
     * @param rounds            actual rounds = 2^strength
     * @param approximateMs     approximate hash time on modern server hardware (ms)
     * @param recommendation    usage recommendation
     */
    public record CostBenchmark(int strength, long rounds, String approximateMs, String recommendation) {}

    /** Returns a reference guide for BCrypt cost factors. */
    public static List<CostBenchmark> costFactorGuide() {
        return List.of(
            new CostBenchmark(4,   16L,       "< 1 ms",   "Tests only — far too fast"),
            new CostBenchmark(6,   64L,       "~1 ms",    "Legacy systems — upgrade"),
            new CostBenchmark(8,   256L,      "~5 ms",    "Minimum acceptable"),
            new CostBenchmark(10,  1024L,     "~30 ms",   "OWASP minimum recommended"),
            new CostBenchmark(12,  4096L,     "~100 ms",  "Recommended for 2024+"),
            new CostBenchmark(14,  16384L,    "~500 ms",  "High security, user notices"),
            new CostBenchmark(16,  65536L,    "~2 000 ms","Too slow for interactive use")
        );
    }

    /** Returns the OWASP-recommended production strength. */
    public static int recommendedProductionStrength() { return 12; }

    /** Returns the minimum strength considered acceptable. */
    public static int minimumAcceptableStrength() { return 10; }

    /** Creates a BCryptPasswordEncoder with the given strength. */
    public static PasswordEncoder createEncoder(int strength) {
        return new BCryptPasswordEncoder(strength);
    }

    /** Creates a BCryptPasswordEncoder with the recommended production strength. */
    public static PasswordEncoder productionEncoder() {
        return createEncoder(recommendedProductionStrength());
    }

    /**
     * Demonstrates upgrade behaviour: if the stored hash uses a lower factor,
     * re-hash after successful verification so the next login uses the new factor.
     */
    public static class PasswordUpgradeService {

        private final PasswordEncoder currentEncoder;
        private final int             currentStrength;

        public PasswordUpgradeService(PasswordEncoder encoder, int strength) {
            this.currentEncoder  = encoder;
            this.currentStrength = strength;
        }

        /** Returns true if the stored hash uses a lower cost factor than current. */
        public boolean needsUpgrade(String storedHash) {
            // BCrypt hash format: $2a$<strength>$<salt+hash>
            if (storedHash == null || !storedHash.startsWith("$2")) return false;
            String[] parts = storedHash.split("\\$");
            if (parts.length < 3) return false;
            try {
                int storedStrength = Integer.parseInt(parts[2]);
                return storedStrength < currentStrength;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        /** Encodes a raw password using the current encoder. */
        public String encode(String raw) { return currentEncoder.encode(raw); }

        /** Verifies raw against stored; returns true if they match. */
        public boolean verify(String raw, String stored) {
            return currentEncoder.matches(raw, stored);
        }
    }
}
