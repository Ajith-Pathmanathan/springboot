package com.techleadguru.phase1.day12;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 12 — Test: Optional<T> injection for optional dependencies.
 */
class Day12OptionalInjectionTest {

    // -----------------------------------------------------------------------
    // Test 1: When bean is present, Optional contains it
    // -----------------------------------------------------------------------
    @Test
    void monitoring_service_uses_alert_channel_when_present() {
        Day12OptionalInjection.AlertChannel slack = new Day12OptionalInjection.SlackAlertChannel();
        var service = new Day12OptionalInjection.MonitoringService(Optional.of(slack));

        assertThat(service.hasAlertChannel()).isTrue();
        assertThat(service.describeChannel()).isEqualTo("SlackAlertChannel");

        // No exception — alert is sent
        service.checkSystem("CPU", 95.0);
        System.out.println("[DAY 12] Alert sent via Slack (bean present)");
    }

    // -----------------------------------------------------------------------
    // Test 2: When no bean, Optional.empty() — no exception, no NPE
    // -----------------------------------------------------------------------
    @Test
    void monitoring_service_works_safely_when_alert_channel_absent() {
        var service = new Day12OptionalInjection.MonitoringService(Optional.empty());

        assertThat(service.hasAlertChannel()).isFalse();
        assertThat(service.describeChannel()).isEqualTo("NONE");

        // Should NOT throw — handles absent bean safely
        service.checkSystem("CPU", 95.0);
        System.out.println("[DAY 12] No alert channel — logged warning, no exception");
    }

    // -----------------------------------------------------------------------
    // Test 3: No alert sent when metric is below threshold
    // -----------------------------------------------------------------------
    @Test
    void no_alert_sent_when_metric_below_threshold() {
        var sent = new boolean[]{false};
        Day12OptionalInjection.AlertChannel trackingChannel = alert -> sent[0] = true;
        var service = new Day12OptionalInjection.MonitoringService(Optional.of(trackingChannel));

        service.checkSystem("CPU", 50.0); // below 90% threshold

        assertThat(sent[0]).isFalse();
        System.out.println("[DAY 12] No alert at 50% — threshold not exceeded");
    }

    // -----------------------------------------------------------------------
    // Test 4: Document comparison of injection patterns
    // -----------------------------------------------------------------------
    @Test
    void document_optional_vs_required_false_vs_object_provider() {
        System.out.println("[DAY 12] OPTIONAL INJECTION COMPARISON:");
        System.out.println("  @Autowired(required=false) field → null if missing → NPE risk");
        System.out.println("  Optional<T>                      → empty() if missing → null-safe");
        System.out.println("  ObjectProvider<T>                → lazy, stream-capable, no NPE");
        System.out.println();
        System.out.println("  RULE: Prefer Optional<T> for simple optional dependencies.");
        System.out.println("        Use ObjectProvider<T> when you need lazy init or prototype bean per call.");
        assertThat(true).isTrue();
    }
}
