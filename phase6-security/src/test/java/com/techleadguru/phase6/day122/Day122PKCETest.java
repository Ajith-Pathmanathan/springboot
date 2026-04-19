package com.techleadguru.phase6.day122;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class Day122PKCETest {

    @Test
    void generateCodeVerifier_has_valid_length() {
        String verifier = Day122PKCE.generateCodeVerifier();
        assertThat(verifier.length()).isBetween(43, 128);
    }

    @Test
    void generateCodeVerifier_each_call_produces_unique_value() {
        String v1 = Day122PKCE.generateCodeVerifier();
        String v2 = Day122PKCE.generateCodeVerifier();
        assertThat(v1).isNotEqualTo(v2);
    }

    @Test
    void generateCodeChallenge_returns_non_blank_base64url_string() {
        String verifier   = Day122PKCE.generateCodeVerifier();
        String challenge  = Day122PKCE.generateCodeChallenge(verifier);
        assertThat(challenge).isNotBlank();
        // Base64URL must not contain standard Base64 padding '='
        assertThat(challenge).doesNotContain("=");
        assertThat(challenge).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void generateCodeChallenge_is_deterministic() {
        String verifier  = "demoVerifier12345678901234567890123456789012345";
        String ch1       = Day122PKCE.generateCodeChallenge(verifier);
        String ch2       = Day122PKCE.generateCodeChallenge(verifier);
        assertThat(ch1).isEqualTo(ch2);
    }

    @Test
    void generate_produces_pkce_parameters() {
        Day122PKCE.PkceParameters params = Day122PKCE.generate();
        assertThat(params.codeVerifier()).isNotBlank();
        assertThat(params.codeChallenge()).isNotBlank();
        assertThat(params.method()).isEqualTo(Day122PKCE.ChallengeMethod.S256);
        assertThat(params.codeVerifier()).isNotEqualTo(params.codeChallenge());
    }

    @Test
    void verify_returns_true_when_verifier_matches_challenge() {
        Day122PKCE.PkceParameters params = Day122PKCE.generate();
        assertThat(Day122PKCE.verify(params.codeVerifier(), params.codeChallenge()))
                .isTrue();
    }

    @Test
    void verify_returns_false_when_verifier_does_not_match() {
        Day122PKCE.PkceParameters params = Day122PKCE.generate();
        assertThat(Day122PKCE.verify("wrong-verifier-that-wont-match-the-challenge", params.codeChallenge()))
                .isFalse();
    }

    @Test
    void verify_returns_false_for_null_inputs() {
        assertThat(Day122PKCE.verify(null, "challenge")).isFalse();
        assertThat(Day122PKCE.verify("verifier", null)).isFalse();
    }

    @Test
    void isValidVerifierLength_true_within_bounds() {
        String v = Day122PKCE.generateCodeVerifier();
        assertThat(Day122PKCE.isValidVerifierLength(v)).isTrue();
    }

    @Test
    void isValidVerifierLength_false_too_short() {
        assertThat(Day122PKCE.isValidVerifierLength("short")).isFalse();
    }

    @Test
    void isValidVerifierLength_false_for_null() {
        assertThat(Day122PKCE.isValidVerifierLength(null)).isFalse();
    }

    @Test
    void pkce_end_to_end_works_correctly() {
        // Simulate full PKCE exchange
        Day122PKCE.PkceParameters params = Day122PKCE.generate();

        // 1. Client sends code_challenge in authorization request
        String storedChallenge = params.codeChallenge();

        // 2. Client sends code_verifier in token request
        // 3. Server verifies
        boolean verified = Day122PKCE.verify(params.codeVerifier(), storedChallenge);
        assertThat(verified).isTrue();

        // 4. Attacker tries with different verifier — must fail
        String attackerVerifier = Day122PKCE.generateCodeVerifier();
        boolean attackerSucceeds = Day122PKCE.verify(attackerVerifier, storedChallenge);
        assertThat(attackerSucceeds).isFalse();
    }
}
