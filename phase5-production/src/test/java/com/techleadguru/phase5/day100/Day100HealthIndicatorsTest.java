package com.techleadguru.phase5.day100;

import org.junit.jupiter.api.*;
import org.springframework.boot.actuate.health.Health;
import static org.assertj.core.api.Assertions.*;

class Day100HealthIndicatorsTest {

    // ---- LivenessStateMachine (pure Java — no Spring context) ----

    @Test
    void livenessStateMachine_starts_correct() {
        var sm = new Day100HealthIndicators.LivenessStateMachine();
        assertThat(sm.isBroken()).isFalse();
        assertThat(sm.getReason()).isEqualTo("OK");
    }

    @Test
    void livenessStateMachine_toHealth_returns_up_when_not_broken() {
        var sm = new Day100HealthIndicators.LivenessStateMachine();
        Health health = sm.toHealth();
        assertThat(health.getStatus().getCode()).isEqualTo("UP");
    }

    @Test
    void livenessStateMachine_markBroken_fails_health() {
        var sm = new Day100HealthIndicators.LivenessStateMachine();
        sm.markBroken("deadlock detected");
        assertThat(sm.isBroken()).isTrue();
        assertThat(sm.getReason()).isEqualTo("deadlock detected");
        Health health = sm.toHealth();
        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
    }

    @Test
    void livenessStateMachine_recover_restores_health() {
        var sm = new Day100HealthIndicators.LivenessStateMachine();
        sm.markBroken("test failure");
        sm.recover();
        assertThat(sm.isBroken()).isFalse();
        assertThat(sm.toHealth().getStatus().getCode()).isEqualTo("UP");
    }

    // ---- DatabaseSanityHealthIndicator (pure Java) ----

    @Test
    void databaseSanityIndicator_up_when_count_meets_minimum() {
        var indicator = new Day100HealthIndicators.DatabaseSanityHealthIndicator(100);
        indicator.setCurrentRecordCount(150);
        Health health = indicator.health();
        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails()).containsKey("recordCount");
    }

    @Test
    void databaseSanityIndicator_down_when_count_below_minimum() {
        var indicator = new Day100HealthIndicators.DatabaseSanityHealthIndicator(100);
        indicator.setCurrentRecordCount(50);
        Health health = indicator.health();
        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails()).containsKey("reason");
    }

    @Test
    void databaseSanityIndicator_up_when_exactly_at_minimum() {
        var indicator = new Day100HealthIndicators.DatabaseSanityHealthIndicator(100);
        indicator.setCurrentRecordCount(100);
        assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");
    }

    @Test
    void databaseSanityIndicator_up_with_zero_minimum() {
        var indicator = new Day100HealthIndicators.DatabaseSanityHealthIndicator(0);
        indicator.setCurrentRecordCount(0);
        assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");
    }
}
