package com.techleadguru.phase5.day99;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day99ActuatorDeepDiveTest {

    // Testing FeatureFlagsEndpoint directly (no Spring context needed)
    private final Day99ActuatorDeepDive.FeatureFlagsEndpoint endpoint =
            new Day99ActuatorDeepDive.FeatureFlagsEndpoint();

    @Test
    void featureFlagsEndpoint_getAll_returns_default_flags() {
        var flags = endpoint.getAll();
        assertThat(flags).containsKey("new-checkout-flow");
        assertThat(flags).containsKey("dark-mode-ui");
    }

    @Test
    void featureFlagsEndpoint_default_dark_mode_ui_is_true() {
        assertThat(endpoint.isFlagEnabled("dark-mode-ui")).isTrue();
    }

    @Test
    void featureFlagsEndpoint_default_new_checkout_flow_is_false() {
        assertThat(endpoint.isFlagEnabled("new-checkout-flow")).isFalse();
    }

    @Test
    void featureFlagsEndpoint_setFlag_updates_value() {
        endpoint.setFlagDirect("new-checkout-flow", true);
        assertThat(endpoint.isFlagEnabled("new-checkout-flow")).isTrue();
    }

    @Test
    void featureFlagsEndpoint_writeOperation_returns_updated_status() {
        var result = endpoint.setFlag("beta-analytics", true);
        assertThat(result.get("status")).isEqualTo("updated");
        assertThat(result.get("enabled")).isEqualTo(true);
    }

    @Test
    void featureFlagsEndpoint_removeFlag_removes_existing_flag() {
        endpoint.setFlagDirect("temp-flag", true);
        var result = endpoint.removeFlag("temp-flag");
        assertThat(result.get("status")).isEqualTo("removed");
        assertThat(endpoint.getFlag("temp-flag")).isEqualTo("NOT_FOUND");
    }

    @Test
    void featureFlagsEndpoint_removeFlag_non_existent_returns_NOT_FOUND() {
        var result = endpoint.removeFlag("does-not-exist");
        assertThat(result.get("status")).isEqualTo("NOT_FOUND");
    }

    @Test
    void featureFlagsEndpoint_getFlagCount_counts_boolean_flags() {
        int count = endpoint.getFlagCount();
        assertThat(count).isGreaterThanOrEqualTo(3); // new-checkout-flow, dark-mode-ui, beta-analytics
    }

    @Test
    void featureFlagsEndpoint_getFlag_existing_returns_value() {
        Object val = endpoint.getFlag("dark-mode-ui");
        assertThat(val).isEqualTo(true);
    }

    @Test
    void featureFlagsEndpoint_getFlag_missing_returns_NOT_FOUND() {
        Object val = endpoint.getFlag("unknown-flag-xyz");
        assertThat(val).isEqualTo("NOT_FOUND");
    }

    // AppDiagnosticsEndpoint tests
    @Test
    void appDiagnosticsEndpoint_getDiagnostics_returns_data() {
        var diagEndpoint = new Day99ActuatorDeepDive.AppDiagnosticsEndpoint();
        var data = diagEndpoint.getDiagnostics();
        assertThat(data).containsKey("startedAt");
    }

    // ActuatorSecurityGuide tests
    @Test
    void actuatorSecurityGuide_securityConfigSnippet_contains_actuator_path() {
        String snippet = Day99ActuatorDeepDive.ActuatorSecurityGuide.securityConfigSnippet();
        assertThat(snippet).containsIgnoringCase("actuator");
    }

    @Test
    void actuatorSecurityGuide_exposureRecommendations_is_not_empty() {
        var recs = Day99ActuatorDeepDive.ActuatorSecurityGuide.exposureRecommendations();
        assertThat(recs).isNotEmpty();
    }
}
