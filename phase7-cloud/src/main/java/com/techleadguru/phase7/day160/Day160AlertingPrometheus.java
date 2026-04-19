package com.techleadguru.phase7.day160;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Day 160 — Alerting with Prometheus Alertmanager
 *
 * Alert pipeline:
 *   Prometheus evaluates alert rules (YAML) → fires when condition true for 'for' duration
 *   → Alertmanager receives → groups, deduplicates, routes → notifies (Slack, email, PD)
 */
public class Day160AlertingPrometheus {

    // ─────────────────────────────────────────────────────────────────────────
    // Alert model
    // ─────────────────────────────────────────────────────────────────────────

    public enum AlertSeverity { CRITICAL, WARNING, INFO }

    public record AlertCondition(
            String metric,        // PromQL metric name
            String operator,      // >, <, ==, !=
            double threshold,
            String forDuration) { // e.g. "5m" — must be true for this long before firing

        public boolean evaluate(double currentValue) {
            return switch (operator) {
                case ">"  -> currentValue >  threshold;
                case ">=" -> currentValue >= threshold;
                case "<"  -> currentValue <  threshold;
                case "<=" -> currentValue <= threshold;
                case "==" -> currentValue == threshold;
                case "!=" -> currentValue != threshold;
                default   -> false;
            };
        }
    }

    public record AlertRule(
            String         name,
            AlertCondition condition,
            AlertSeverity  severity,
            String         summary,
            String         description) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Alert simulator
    // ─────────────────────────────────────────────────────────────────────────

    public static class AlertSimulator {

        private final List<AlertRule>   rules          = new ArrayList<>();
        private final List<AlertRule>   firingAlerts   = new CopyOnWriteArrayList<>();
        private final List<AlertRule>   resolvedAlerts = new CopyOnWriteArrayList<>();

        public void addRule(AlertRule rule) { rules.add(rule); }

        /** Evaluate all rules against a named metric's current value. */
        public void evaluate(String metric, double currentValue) {
            for (AlertRule rule : rules) {
                if (!rule.condition().metric().equals(metric)) continue;
                boolean firing = rule.condition().evaluate(currentValue);
                if (firing && !firingAlerts.contains(rule)) {
                    firingAlerts.add(rule);
                    resolvedAlerts.remove(rule);
                } else if (!firing && firingAlerts.remove(rule)) {
                    resolvedAlerts.add(rule);
                }
            }
        }

        public List<AlertRule> firingAlerts()   { return Collections.unmodifiableList(firingAlerts); }
        public List<AlertRule> resolvedAlerts() { return Collections.unmodifiableList(resolvedAlerts); }
        public void clearResolved()             { resolvedAlerts.clear(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sample alert rules
    // ─────────────────────────────────────────────────────────────────────────

    public static List<AlertRule> sampleAlertRules() {
        return List.of(
            new AlertRule(
                "HighErrorRate",
                new AlertCondition("error_rate_percent", ">", 5.0, "5m"),
                AlertSeverity.CRITICAL,
                "Error rate is high",
                "HTTP 5xx error rate exceeded 5% for 5 minutes"),
            new AlertRule(
                "HighP99Latency",
                new AlertCondition("p99_latency_seconds", ">", 2.0, "5m"),
                AlertSeverity.WARNING,
                "P99 latency is high",
                "99th percentile latency exceeded 2 seconds for 5 minutes"),
            new AlertRule(
                "LowThroughput",
                new AlertCondition("request_rate_rps", "<", 0.1, "10m"),
                AlertSeverity.WARNING,
                "Suspiciously low request rate",
                "Service may be isolated or traffic not reaching it"),
            new AlertRule(
                "HighHeapUsage",
                new AlertCondition("jvm_heap_usage_percent", ">", 85.0, "10m"),
                AlertSeverity.WARNING,
                "JVM heap usage is high",
                "Heap usage above 85% may indicate a memory leak"),
            new AlertRule(
                "ServiceDown",
                new AlertCondition("service_up", "<", 1.0, "1m"),
                AlertSeverity.CRITICAL,
                "Service is unreachable",
                "Prometheus scrape target returned 0")
        );
    }
}
