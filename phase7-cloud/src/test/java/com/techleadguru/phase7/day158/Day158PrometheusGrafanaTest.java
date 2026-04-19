package com.techleadguru.phase7.day158;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day158PrometheusGrafanaTest {

    @Test
    void testPrometheusFormatCounter() {
        Day158PrometheusGrafana.PromMetric metric = new Day158PrometheusGrafana.PromMetric(
                "http_requests_total",
                "counter",
                "Total HTTP requests",
                Map.of("method", "GET", "status", "200"),
                42.0);
        String output = Day158PrometheusGrafana.prometheusFormat(metric);
        assertTrue(output.contains("# HELP http_requests_total Total HTTP requests"));
        assertTrue(output.contains("# TYPE http_requests_total counter"));
        assertTrue(output.contains("http_requests_total{"));
        assertTrue(output.contains("42.0"));
    }

    @Test
    void testPrometheusFormatNoLabels() {
        Day158PrometheusGrafana.PromMetric metric = new Day158PrometheusGrafana.PromMetric(
                "app_up", "gauge", "App is up", Map.of(), 1.0);
        String output = Day158PrometheusGrafana.prometheusFormat(metric);
        assertFalse(output.contains("{"));
        assertTrue(output.contains("app_up 1.0"));
    }

    @Test
    void testRedDashboardPanels() {
        List<Day158PrometheusGrafana.DashboardPanel> panels =
                Day158PrometheusGrafana.redDashboardPanels();
        assertEquals(6, panels.size());
        assertTrue(panels.stream().anyMatch(p -> p.title().contains("Rate")));
        assertTrue(panels.stream().anyMatch(p -> p.title().contains("Error")));
        assertTrue(panels.stream().anyMatch(p -> p.title().contains("Latency")));
    }

    @Test
    void testSampleAlertRules() {
        List<Day158PrometheusGrafana.AlertRule> rules =
                Day158PrometheusGrafana.sampleAlertRules();
        assertEquals(4, rules.size());
        assertTrue(rules.stream().anyMatch(
                r -> r.severity() == Day158PrometheusGrafana.AlertSeverity.CRITICAL));
    }

    @Test
    void testActuatorProperties() {
        Map<String, String> props = Day158PrometheusGrafana.actuatorProperties();
        assertTrue(props.get("management.endpoints.web.exposure.include").contains("prometheus"));
        assertEquals("true", props.get("management.endpoint.prometheus.enabled"));
    }
}
