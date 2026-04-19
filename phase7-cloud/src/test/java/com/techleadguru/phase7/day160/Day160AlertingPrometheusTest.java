package com.techleadguru.phase7.day160;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day160AlertingPrometheusTest {

    @Test
    void testAlertConditionGreaterThan() {
        Day160AlertingPrometheus.AlertCondition cond =
                new Day160AlertingPrometheus.AlertCondition(
                        "error_rate", ">", 5.0, "5m");
        assertTrue(cond.evaluate(6.0));
        assertFalse(cond.evaluate(4.0));
        assertFalse(cond.evaluate(5.0));
    }

    @Test
    void testAlertConditionLessThan() {
        Day160AlertingPrometheus.AlertCondition cond =
                new Day160AlertingPrometheus.AlertCondition(
                        "throughput", "<", 1.0, "10m");
        assertTrue(cond.evaluate(0.5));
        assertFalse(cond.evaluate(1.5));
    }

    @Test
    void testAlertSimulatorFiresAlert() {
        Day160AlertingPrometheus.AlertSimulator sim =
                new Day160AlertingPrometheus.AlertSimulator();
        Day160AlertingPrometheus.AlertRule rule =
                new Day160AlertingPrometheus.AlertRule(
                        "HighError",
                        new Day160AlertingPrometheus.AlertCondition(
                                "error_rate_percent", ">", 5.0, "5m"),
                        Day160AlertingPrometheus.AlertSeverity.CRITICAL,
                        "High errors",
                        "Error rate exceeded 5%");
        sim.addRule(rule);
        sim.evaluate("error_rate_percent", 10.0);
        assertEquals(1, sim.firingAlerts().size());
    }

    @Test
    void testAlertSimulatorResolvesAlert() {
        Day160AlertingPrometheus.AlertSimulator sim =
                new Day160AlertingPrometheus.AlertSimulator();
        Day160AlertingPrometheus.AlertRule rule =
                new Day160AlertingPrometheus.AlertRule(
                        "HighError",
                        new Day160AlertingPrometheus.AlertCondition(
                                "error_rate_percent", ">", 5.0, "5m"),
                        Day160AlertingPrometheus.AlertSeverity.WARNING,
                        "High errors",
                        "desc");
        sim.addRule(rule);
        sim.evaluate("error_rate_percent", 10.0); // fires
        sim.evaluate("error_rate_percent", 2.0);  // resolves
        assertEquals(0, sim.firingAlerts().size());
        assertEquals(1, sim.resolvedAlerts().size());
    }

    @Test
    void testAlertSimulatorDoesNotFireForDifferentMetric() {
        Day160AlertingPrometheus.AlertSimulator sim =
                new Day160AlertingPrometheus.AlertSimulator();
        Day160AlertingPrometheus.AlertRule rule =
                new Day160AlertingPrometheus.AlertRule(
                        "Rule",
                        new Day160AlertingPrometheus.AlertCondition(
                                "metric_a", ">", 1.0, "1m"),
                        Day160AlertingPrometheus.AlertSeverity.INFO,
                        "s", "d");
        sim.addRule(rule);
        sim.evaluate("metric_b", 999.0); // different metric
        assertEquals(0, sim.firingAlerts().size());
    }

    @Test
    void testSampleAlertRules() {
        List<Day160AlertingPrometheus.AlertRule> rules =
                Day160AlertingPrometheus.sampleAlertRules();
        assertEquals(5, rules.size());
        assertTrue(rules.stream().anyMatch(
                r -> r.severity() == Day160AlertingPrometheus.AlertSeverity.CRITICAL));
    }
}
