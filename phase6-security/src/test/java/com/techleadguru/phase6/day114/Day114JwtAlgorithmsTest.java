package com.techleadguru.phase6.day114;

import org.junit.jupiter.api.Test;
import java.security.KeyPair;
import static org.assertj.core.api.Assertions.*;

class Day114JwtAlgorithmsTest {

    @Test
    void algorithmCatalog_has_3_algorithms() {
        assertThat(Day114JwtAlgorithms.algorithmCatalog()).hasSize(3);
    }

    @Test
    void algorithmCatalog_each_entry_has_non_blank_fields() {
        Day114JwtAlgorithms.algorithmCatalog().forEach(info -> {
            assertThat(info.algorithm()).isNotNull();
            assertThat(info.keyType()).isNotBlank();
            assertThat(info.keyMaterial()).isNotBlank();
            assertThat(info.useCase()).isNotBlank();
        });
    }

    @Test
    void rs256_supports_distributed_validation() {
        var rs256 = Day114JwtAlgorithms.algorithmCatalog().stream()
                .filter(i -> i.algorithm() == Day114JwtAlgorithms.JwtAlgorithm.RS256)
                .findFirst();
        assertThat(rs256).isPresent();
        assertThat(rs256.get().supportsDistributedValidation()).isTrue();
    }

    @Test
    void hs256_does_not_support_distributed_validation() {
        var hs256 = Day114JwtAlgorithms.algorithmCatalog().stream()
                .filter(i -> i.algorithm() == Day114JwtAlgorithms.JwtAlgorithm.HS256)
                .findFirst();
        assertThat(hs256).isPresent();
        assertThat(hs256.get().supportsDistributedValidation()).isFalse();
    }

    @Test
    void recommendedForMicroservices_is_rs256() {
        assertThat(Day114JwtAlgorithms.recommendedForMicroservices())
                .isEqualTo(Day114JwtAlgorithms.JwtAlgorithm.RS256);
    }

    @Test
    void signHs256_produces_non_blank_signature() {
        String sig = Day114JwtAlgorithms.signHs256("header.payload", "my-secret");
        assertThat(sig).isNotBlank();
    }

    @Test
    void verifyHs256_returns_true_for_correct_signature() {
        String payload = "header.payload";
        String secret  = "super-secret-key";
        String sig     = Day114JwtAlgorithms.signHs256(payload, secret);
        String jwt     = payload + "." + sig;
        assertThat(Day114JwtAlgorithms.verifyHs256(jwt, secret)).isTrue();
    }

    @Test
    void verifyHs256_returns_false_for_wrong_secret() {
        String payload = "header.payload";
        String sig     = Day114JwtAlgorithms.signHs256(payload, "correct-secret");
        String jwt     = payload + "." + sig;
        assertThat(Day114JwtAlgorithms.verifyHs256(jwt, "wrong-secret")).isFalse();
    }

    @Test
    void verifyHs256_returns_false_for_tampered_payload() {
        String original  = "header.payload";
        String sig       = Day114JwtAlgorithms.signHs256(original, "my-secret");
        String tampered  = "header.tampered-payload." + sig;
        assertThat(Day114JwtAlgorithms.verifyHs256(tampered, "my-secret")).isFalse();
    }

    @Test
    void generateRsaKeyPair_returns_rsa_keys() {
        KeyPair kp = Day114JwtAlgorithms.generateRsaKeyPair();
        assertThat(kp.getPublic().getAlgorithm()).isEqualTo("RSA");
        assertThat(kp.getPrivate().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void signRs256_and_verifyRs256_round_trip() {
        KeyPair kp      = Day114JwtAlgorithms.generateRsaKeyPair();
        String  payload = "header.mypayload";
        String  sig     = Day114JwtAlgorithms.signRs256(payload, kp.getPrivate());
        String  jwt     = payload + "." + sig;
        assertThat(Day114JwtAlgorithms.verifyRs256(jwt, kp.getPublic())).isTrue();
    }

    @Test
    void verifyRs256_returns_false_for_tampered_payload() {
        KeyPair kp      = Day114JwtAlgorithms.generateRsaKeyPair();
        String  payload = "header.original";
        String  sig     = Day114JwtAlgorithms.signRs256(payload, kp.getPrivate());
        String  jwt     = "header.tampered." + sig;
        assertThat(Day114JwtAlgorithms.verifyRs256(jwt, kp.getPublic())).isFalse();
    }

    @Test
    void verifyRs256_returns_false_with_wrong_key() {
        KeyPair kp1     = Day114JwtAlgorithms.generateRsaKeyPair();
        KeyPair kp2     = Day114JwtAlgorithms.generateRsaKeyPair();
        String  payload = "header.payload";
        String  sig     = Day114JwtAlgorithms.signRs256(payload, kp1.getPrivate());
        String  jwt     = payload + "." + sig;
        assertThat(Day114JwtAlgorithms.verifyRs256(jwt, kp2.getPublic())).isFalse();
    }
}
