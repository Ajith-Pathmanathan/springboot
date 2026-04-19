package com.techleadguru.phase6.day106;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class Day106SecurityFilterChainTest {

    @Test
    void standardFilterOrder_returns_15_filters() {
        assertThat(Day106SecurityFilterChain.standardFilterOrder()).hasSize(15);
    }

    @Test
    void standardFilterOrder_is_ascending() {
        var filters = Day106SecurityFilterChain.standardFilterOrder();
        for (int i = 1; i < filters.size(); i++) {
            assertThat(filters.get(i).order())
                    .as("Filter at index %d should have higher order than previous", i)
                    .isGreaterThan(filters.get(i - 1).order());
        }
    }

    @Test
    void filterNamesInOrder_matches_standard_order_names() {
        var names     = Day106SecurityFilterChain.filterNamesInOrder();
        var entries   = Day106SecurityFilterChain.standardFilterOrder();
        assertThat(names).hasSize(entries.size());
        for (int i = 0; i < names.size(); i++) {
            assertThat(names.get(i)).isEqualTo(entries.get(i).name());
        }
    }

    @Test
    void filterNamesInOrder_contains_authorization_filter_last() {
        var names = Day106SecurityFilterChain.filterNamesInOrder();
        assertThat(names.getLast()).isEqualTo("AuthorizationFilter");
    }

    @Test
    void filterNamesInOrder_first_is_disable_encode_url() {
        var names = Day106SecurityFilterChain.filterNamesInOrder();
        assertThat(names.getFirst()).isEqualTo("DisableEncodeUrlFilter");
    }

    @Test
    void findByName_returns_present_for_known_filter() {
        var entry = Day106SecurityFilterChain.findByName("CrsrfFilter");
        // This filter is not in the list — should be empty
        assertThat(entry).isEmpty();

        var csrfEntry = Day106SecurityFilterChain.findByName("CsrfFilter");
        assertThat(csrfEntry).isPresent();
        assertThat(csrfEntry.get().purpose()).contains("CSRF");
    }

    @Test
    void findByName_returns_empty_for_unknown_filter() {
        assertThat(Day106SecurityFilterChain.findByName("NonExistentFilter")).isEmpty();
    }

    @Test
    void filtersByKeyword_finds_csrf_related() {
        var results = Day106SecurityFilterChain.filtersByKeyword("CSRF");
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(e -> e.name().equals("CsrfFilter"));
    }

    @Test
    void filtersByKeyword_is_case_insensitive() {
        var lower = Day106SecurityFilterChain.filtersByKeyword("cors");
        var upper = Day106SecurityFilterChain.filtersByKeyword("CORS");
        assertThat(lower).isEqualTo(upper);
    }

    @Test
    void customFilterSteps_has_5_steps() {
        assertThat(Day106SecurityFilterChain.customFilterSteps()).hasSize(5);
    }

    @Test
    void each_filter_has_non_blank_name_and_purpose() {
        Day106SecurityFilterChain.standardFilterOrder().forEach(e -> {
            assertThat(e.name()).isNotBlank();
            assertThat(e.purpose()).isNotBlank();
            assertThat(e.order()).isPositive();
        });
    }
}
