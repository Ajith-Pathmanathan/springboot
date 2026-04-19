package com.techleadguru.phase7.day158;

import java.util.*;

/**
 * Day 158 — Prometheus metrics and Grafana dashboards
 *
 * Prometheus scrapes metrics from /actuator/prometheus (text exposition format).
 * Grafana queries Prometheus using PromQL and renders dashboards.
 *
 * Spring Boot setup:
 *   - Add micrometer-registry-prometheus dependency
 *   - Expose prometheus endpoint: management.endpoints.web.exposure.include=prometheus
 *   - Prometheus scrape config points to {host}/actuator/prometheus
 */
public class Day158PrometheusGrafana {

    // ─────────────────────────────────────────────────────────────────────────
    // Prometheus text format model
    // ─────────────────────────────────────────────────────────────────────────

    public record PromMetric(
            String name,
            String type,    // counter, gauge, histogram, summary
            String help,
            Map<String, String> labels,
            double value) {}

    /**
     * Format a metric in Prometheus text exposition format.
     * Example output:
     *   # HELP http_requests_total Total HTTP requests
     *   # TYPE http_requests_total counter
     *   http_requests_total{method="GET",status="200"} 42.0
     */
    public static String prometheusFormat(PromMetric metric) {
        StringBuilder sb = new StringBuilder();
        sb.append("# HELP ").append(metric.name()).append(" ").append(metric.help()).append("\n");
        sb.append("# TYPE ").append(metric.name()).append(" ").append(metric.type()).append("\n");
        sb.append(metric.name());
        if (!metric.labels().isEmpty()) {
            sb.append("{");
            List<String> labelPairs = new ArrayList<>();
            metric.labels().forEach((k, v) -> labelPairs.add(k + "=\"" + v + "\""));
            sb.append(String.join(",", labelPairs));
            sb.append("}");
        }
        sb.append(" ").append(metric.value());
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RED dashboard panels
    // ─────────────────────────────────────────────────────────────────────────

    public record DashboardPanel(
            String title,
            String promqlExpression,
            String visualization,
            String description) {}

    public static List<DashboardPanel> redDashboardPanels() {
        return List.of(
            new DashboardPanel(
                "Request Rate (RPS)",
                "rate(http_server_requests_seconds_count[1m])",
                "Graph",
                "Requests per second — group by method and uri"),
            new DashboardPanel(
                "Error Rate (%)",
                "rate(http_server_requests_seconds_count{status=~'5..'}[1m]) / rate(http_server_requests_seconds_count[1m]) * 100",
                "Graph",
                "Percentage of HTTP 5xx responses"),
            new DashboardPanel(
                "P99 Latency (ms)",
                "histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[1m])) * 1000",
                "Graph",
                "99th percentile response time"),
            new DashboardPanel(
                "P50 Latency (ms)",
                "histogram_quantile(0.50, rate(http_server_requests_seconds_bucket[1m])) * 1000",
                "Graph",
                "Median response time"),
            new DashboardPanel(
                "Active Connections",
                "tomcat_connections_active_current_connections",
                "Gauge",
                "Current active Tomcat connections"),
            new DashboardPanel(
                "JVM Heap Used",
                "jvm_memory_used_bytes{area='heap'}",
                "Graph",
                "Heap memory usage in bytes")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Alert rules
    // ─────────────────────────────────────────────────────────────────────────

    public enum AlertSeverity { CRITICAL, WARNING, INFO }

    public record AlertRule(
            String        name,
            String        expression,       // PromQL
            String        forDuration,      // e.g. "5m"
            AlertSeverity severity,
            String        summary) {}

    public static List<AlertRule> sampleAlertRules() {
        return List.of(
            new AlertRule(
                "HighErrorRate",
                "rate(http_server_requests_seconds_count{status=~'5..'}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05",
                "5m",
                AlertSeverity.CRITICAL,
                "Error rate above 5% for 5 minutes"),
            new AlertRule(
                "HighP99Latency",
                "histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m])) > 2",
                "5m",
                AlertSeverity.WARNING,
                "P99 latency above 2 seconds for 5 minutes"),
            new AlertRule(
                "HighHeapUsage",
                "jvm_memory_used_bytes{area='heap'} / jvm_memory_max_bytes{area='heap'} > 0.85",
                "10m",
                AlertSeverity.WARNING,
                "Heap usage above 85% for 10 minutes"),
            new AlertRule(
                "ServiceDown",
                "up == 0",
                "1m",
                AlertSeverity.CRITICAL,
                "Service scrape target is down")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actuator / Spring Boot properties
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> actuatorProperties() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("management.endpoints.web.exposure.include",      "prometheus,health,info,metrics");
        props.put("management.endpoint.prometheus.enabled",         "true");
        props.put("management.metrics.export.prometheus.enabled",   "true");
        props.put("management.metrics.tags.application",            "${spring.application.name}");
        return props;
    }
}
