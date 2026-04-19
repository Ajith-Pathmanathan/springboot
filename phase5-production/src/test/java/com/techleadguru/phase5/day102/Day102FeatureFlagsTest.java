package com.techleadguru.phase5.day102;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day102FeatureFlagsTest {

    private Day102FeatureFlags flags;

    @BeforeEach
    void setUp() {
        flags = new Day102FeatureFlags();
    }

    @Test
    void isEnabled_returns_false_for_unknown_flag() {
        assertThat(flags.isEnabled("non-existent-flag")).isFalse();
    }

    @Test
    void isEnabled_returns_custom_default_for_unknown_flag() {
        assertThat(flags.isEnabled("non-existent-flag", true)).isTrue();
    }

    @Test
    void default_flags_are_registered_on_construction() {
        assertThat(flags.getFlagCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void enable_makes_flag_true() {
        flags.disable("email-async-processing"); // ensure it's false first
        flags.enable("email-async-processing");
        assertThat(flags.isEnabled("email-async-processing")).isTrue();
    }

    @Test
    void disable_makes_flag_false() {
        flags.enable("new-checkout-flow"); // set true first
        flags.disable("new-checkout-flow");
        assertThat(flags.isEnabled("new-checkout-flow")).isFalse();
    }

    @Test
    void toggle_flips_false_to_true() {
        flags.disable("beta-dashboard");
        boolean result = flags.toggle("beta-dashboard");
        assertThat(result).isTrue();
        assertThat(flags.isEnabled("beta-dashboard")).isTrue();
    }

    @Test
    void toggle_flips_true_to_false() {
        flags.enable("email-async-processing");
        boolean result = flags.toggle("email-async-processing");
        assertThat(result).isFalse();
        assertThat(flags.isEnabled("email-async-processing")).isFalse();
    }

    @Test
    void register_adds_a_new_flag() {
        flags.register("dark-mode", true, "Enable dark mode UI");
        assertThat(flags.isEnabled("dark-mode")).isTrue();
        assertThat(flags.getFlagCount()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void getAll_returns_snapshot_of_all_flags() {
        var all = flags.getAll();
        assertThat(all).isNotEmpty();
        assertThat(all).containsKey("new-checkout-flow");
    }

    @Test
    void getAllWithDescriptions_contains_description() {
        var list = flags.getAllWithDescriptions();
        assertThat(list).isNotEmpty();
        var firstWithDesc = list.stream()
                .filter(f -> !f.description().isBlank())
                .findFirst();
        assertThat(firstWithDesc).isPresent();
    }

    @Test
    void remove_deletes_flag() {
        flags.register("temp-flag", false, "Temp");
        boolean removed = flags.remove("temp-flag");
        assertThat(removed).isTrue();
        assertThat(flags.isEnabled("temp-flag")).isFalse();
    }

    @Test
    void remove_returns_false_for_non_existent_flag() {
        boolean removed = flags.remove("does-not-exist");
        assertThat(removed).isFalse();
    }
}
