package com.techleadguru.phase6.day116;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class Day116JwtPitfallsTest {

    private static final String UNSIGNED_JWT =
            "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0" + // {"alg":"none","typ":"JWT"}
            ".eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJhZG1pbiJ9" +  // {"sub":"admin","role":"admin"}
            ".";  // empty signature

    private static final String VALID_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
            ".eyJzdWIiOiJhbGljZSIsImlzcyI6InRlc3QiLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6OTk5OTk5OTk5OX0" +
            ".anything";

    @Test
    void knownVulnerabilities_has_4_entries() {
        assertThat(Day116JwtPitfalls.knownVulnerabilities()).hasSize(4);
    }

    @Test
    void each_vulnerability_has_non_blank_fields() {
        Day116JwtPitfalls.knownVulnerabilities().forEach(v -> {
            assertThat(v.name()).isNotBlank();
            assertThat(v.description()).isNotBlank();
            assertThat(v.attackScenario()).isNotBlank();
            assertThat(v.defense()).isNotBlank();
        });
    }

    @Test
    void knownVulnerabilities_includes_alg_none() {
        boolean hasAlgNone = Day116JwtPitfalls.knownVulnerabilities().stream()
                .anyMatch(v -> v.name().contains("alg:none"));
        assertThat(hasAlgNone).isTrue();
    }

    @Test
    void vulnerableValidate_accepts_unsigned_jwt() {
        // The vulnerable validator should return true (demonstrating the bug)
        assertThat(Day116JwtPitfalls.vulnerableValidate(UNSIGNED_JWT)).isTrue();
    }

    @Test
    void secureValidator_isAlgorithmAllowed_true_for_rs256() {
        assertThat(Day116JwtPitfalls.SecureJwtValidator.isAlgorithmAllowed("RS256")).isTrue();
        assertThat(Day116JwtPitfalls.SecureJwtValidator.isAlgorithmAllowed("HS256")).isTrue();
        assertThat(Day116JwtPitfalls.SecureJwtValidator.isAlgorithmAllowed("ES256")).isTrue();
    }

    @Test
    void secureValidator_isAlgorithmAllowed_false_for_none() {
        assertThat(Day116JwtPitfalls.SecureJwtValidator.isAlgorithmAllowed("none")).isFalse();
        assertThat(Day116JwtPitfalls.SecureJwtValidator.isAlgorithmAllowed("NONE")).isFalse();
        assertThat(Day116JwtPitfalls.SecureJwtValidator.isAlgorithmAllowed("RS512")).isFalse();
    }

    @Test
    void secureValidator_detectsAlgNoneAttack_true_for_unsigned_jwt() {
        assertThat(Day116JwtPitfalls.SecureJwtValidator.detectsAlgNoneAttack(UNSIGNED_JWT)).isTrue();
    }

    @Test
    void secureValidator_detectsAlgNoneAttack_false_for_hs256_jwt() {
        assertThat(Day116JwtPitfalls.SecureJwtValidator.detectsAlgNoneAttack(VALID_JWT)).isFalse();
    }

    @Test
    void secureValidator_assertAlgorithmAllowed_throws_for_none() {
        assertThatThrownBy(() ->
                Day116JwtPitfalls.SecureJwtValidator.assertAlgorithmAllowed(UNSIGNED_JWT))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("none");
    }

    @Test
    void secureValidator_assertAlgorithmAllowed_does_not_throw_for_hs256() {
        assertThatNoException().isThrownBy(() ->
                Day116JwtPitfalls.SecureJwtValidator.assertAlgorithmAllowed(VALID_JWT));
    }

    @Test
    void allowedAlgorithms_does_not_contain_none() {
        assertThat(Day116JwtPitfalls.SecureJwtValidator.allowedAlgorithms())
                .doesNotContain("none");
    }
}
