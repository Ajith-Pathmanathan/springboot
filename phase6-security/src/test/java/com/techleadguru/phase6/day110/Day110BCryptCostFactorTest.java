package com.techleadguru.phase6.day110;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;

class Day110BCryptCostFactorTest {

    @Test
    void costFactorGuide_has_7_entries() {
        assertThat(Day110BCryptCostFactor.costFactorGuide()).hasSize(7);
    }

    @Test
    void costFactorGuide_each_entry_has_non_blank_fields() {
        Day110BCryptCostFactor.costFactorGuide().forEach(entry -> {
            assertThat(entry.strength()).isBetween(4, 16);
            assertThat(entry.rounds()).isPositive();
            assertThat(entry.approximateMs()).isNotBlank();
            assertThat(entry.recommendation()).isNotBlank();
        });
    }

    @Test
    void costFactorGuide_strength_12_is_recommended() {
        boolean has12 = Day110BCryptCostFactor.costFactorGuide().stream()
                .anyMatch(e -> e.strength() == 12 && e.recommendation().contains("Recommended"));
        assertThat(has12).isTrue();
    }

    @Test
    void recommendedProductionStrength_is_12() {
        assertThat(Day110BCryptCostFactor.recommendedProductionStrength()).isEqualTo(12);
    }

    @Test
    void minimumAcceptableStrength_is_10() {
        assertThat(Day110BCryptCostFactor.minimumAcceptableStrength()).isEqualTo(10);
    }

    @Test
    void createEncoder_with_strength_4_encodes_and_matches() {
        // Use strength=4 to keep tests fast
        PasswordEncoder encoder = Day110BCryptCostFactor.createEncoder(4);
        String encoded = encoder.encode("mypassword");
        assertThat(encoded).startsWith("$2a$04$");
        assertThat(encoder.matches("mypassword", encoded)).isTrue();
        assertThat(encoder.matches("wrongpassword", encoded)).isFalse();
    }

    @Test
    void productionEncoder_creates_bcrypt_strength_12() {
        PasswordEncoder encoder = Day110BCryptCostFactor.productionEncoder();
        String encoded = encoder.encode("test");
        // The BCrypt identifier includes the cost factor
        assertThat(encoded).startsWith("$2a$12$");
    }

    @Test
    void passwordUpgradeService_needsUpgrade_true_when_lower_cost() {
        PasswordEncoder encoder = Day110BCryptCostFactor.createEncoder(12);
        var svc = new Day110BCryptCostFactor.PasswordUpgradeService(encoder, 12);

        // Hash with cost 4
        String lowCostHash = Day110BCryptCostFactor.createEncoder(4).encode("secret");
        assertThat(svc.needsUpgrade(lowCostHash)).isTrue();
    }

    @Test
    void passwordUpgradeService_needsUpgrade_false_when_same_cost() {
        PasswordEncoder encoder = Day110BCryptCostFactor.createEncoder(12);
        var svc = new Day110BCryptCostFactor.PasswordUpgradeService(encoder, 12);

        String hash = encoder.encode("secret");
        assertThat(svc.needsUpgrade(hash)).isFalse();
    }

    @Test
    void passwordUpgradeService_verify_returns_true_for_correct_password() {
        PasswordEncoder enc = Day110BCryptCostFactor.createEncoder(4);
        var svc = new Day110BCryptCostFactor.PasswordUpgradeService(enc, 4);
        String hash = svc.encode("mypass");
        assertThat(svc.verify("mypass", hash)).isTrue();
        assertThat(svc.verify("wrong",  hash)).isFalse();
    }
}
