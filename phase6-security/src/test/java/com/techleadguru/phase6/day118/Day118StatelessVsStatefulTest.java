package com.techleadguru.phase6.day118;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class Day118StatelessVsStatefulTest {

    @Test
    void comparisonTable_has_8_entries() {
        assertThat(Day118StatelessVsStateful.comparisonTable()).hasSize(8);
    }

    @Test
    void each_entry_has_non_blank_fields() {
        Day118StatelessVsStateful.comparisonTable().forEach(t -> {
            assertThat(t.consideration()).isNotBlank();
            assertThat(t.stateful()).isNotBlank();
            assertThat(t.stateless()).isNotBlank();
        });
    }

    @Test
    void comparisonTable_covers_horizontal_scaling() {
        boolean hasScaling = Day118StatelessVsStateful.comparisonTable().stream()
                .anyMatch(t -> t.consideration().toLowerCase().contains("scaling"));
        assertThat(hasScaling).isTrue();
    }

    @Test
    void comparisonTable_covers_csrf_risk() {
        boolean hasCsrf = Day118StatelessVsStateful.comparisonTable().stream()
                .anyMatch(t -> t.consideration().toLowerCase().contains("csrf"));
        assertThat(hasCsrf).isTrue();
    }

    @Test
    void recommendedForHorizontalScaling_is_stateless() {
        assertThat(Day118StatelessVsStateful.recommendedForHorizontalScaling())
                .isEqualTo(Day118StatelessVsStateful.SessionStrategy.STATELESS);
    }

    @Test
    void statelessBenefits_has_at_least_4_entries() {
        assertThat(Day118StatelessVsStateful.statelessBenefits())
                .hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void statelessBenefits_mentions_no_shared_session_store() {
        assertThat(Day118StatelessVsStateful.statelessBenefits())
                .anyMatch(b -> b.toLowerCase().contains("session store"));
    }

    @Test
    void statelessChallenges_has_at_least_3_entries() {
        assertThat(Day118StatelessVsStateful.statelessChallenges())
                .hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void statelessChallenges_mentions_revocation() {
        assertThat(Day118StatelessVsStateful.statelessChallenges())
                .anyMatch(c -> c.toLowerCase().contains("revocation") || c.toLowerCase().contains("revoke"));
    }

    @Test
    void statelessSessionPolicy_returns_correct_policy_name() {
        assertThat(Day118StatelessVsStateful.statelessSessionPolicy())
                .contains("STATELESS");
    }
}
