package com.techleadguru.phase7.day161;

import java.util.*;

/**
 * Day 161 — Custom Health Indicators and Kubernetes Probes
 *
 * Spring Boot Actuator's /actuator/health aggregates all HealthIndicator beans.
 * Custom indicators expose domain-level health: DB connection, cache, queue lag, etc.
 *
 * Kubernetes probes:
 *   Liveness  → /actuator/health/liveness  — is the app alive? (restart if DOWN)
 *   Readiness → /actuator/health/readiness — is the app ready? (remove from LB if DOWN)
 *
 * Spring Boot auto-wires LivenessStateHealthIndicator and ReadinessStateHealthIndicator
 * when running inside Kubernetes (detected via KUBERNETES_SERVICE_HOST env var).
 */
public class Day161HealthIndicator {

    // ─────────────────────────────────────────────────────────────────────────
    // Health model
    // ─────────────────────────────────────────────────────────────────────────

    public enum HealthStatus { UP, DOWN, OUT_OF_SERVICE, UNKNOWN }

    public record HealthDetail(
            HealthStatus        status,
            String              component,
            Map<String, Object> details) {

        public static HealthDetail up(String component) {
            return new HealthDetail(HealthStatus.UP, component, Map.of());
        }

        public static HealthDetail down(String component, String reason) {
            return new HealthDetail(HealthStatus.DOWN, component,
                    Map.of("error", reason));
        }
    }

    public record CompositeHealth(
            HealthStatus                    status,
            Map<String, HealthDetail>       components) {

        public static CompositeHealth of(Map<String, HealthDetail> components) {
            HealthStatus overall = components.values().stream()
                    .anyMatch(d -> d.status() == HealthStatus.DOWN)
                    ? HealthStatus.DOWN : HealthStatus.UP;
            return new CompositeHealth(overall, components);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HealthChecker interface
    // ─────────────────────────────────────────────────────────────────────────

    public interface HealthChecker {
        String name();
        HealthDetail check();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Database health checker
    // ─────────────────────────────────────────────────────────────────────────

    public static class DatabaseHealthChecker implements HealthChecker {

        private final boolean reachable;
        private final long    queryTimeMs;

        public DatabaseHealthChecker(boolean reachable, long queryTimeMs) {
            this.reachable   = reachable;
            this.queryTimeMs = queryTimeMs;
        }

        @Override public String name() { return "database"; }

        @Override
        public HealthDetail check() {
            if (!reachable) {
                return HealthDetail.down("database", "Cannot connect to database");
            }
            return new HealthDetail(HealthStatus.UP, "database",
                    Map.of("validationQueryMs", queryTimeMs));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cache health checker
    // ─────────────────────────────────────────────────────────────────────────

    public static class CacheHealthChecker implements HealthChecker {

        private final boolean reachable;
        private final double  hitRate;

        public CacheHealthChecker(boolean reachable, double hitRate) {
            this.reachable = reachable;
            this.hitRate   = hitRate;
        }

        @Override public String name() { return "cache"; }

        @Override
        public HealthDetail check() {
            if (!reachable) {
                return HealthDetail.down("cache", "Redis connection refused");
            }
            return new HealthDetail(HealthStatus.UP, "cache",
                    Map.of("hitRate", String.format("%.1f%%", hitRate * 100)));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Composite health aggregator
    // ─────────────────────────────────────────────────────────────────────────

    public static CompositeHealth aggregate(List<HealthChecker> checkers) {
        Map<String, HealthDetail> results = new LinkedHashMap<>();
        checkers.forEach(c -> results.put(c.name(), c.check()));
        return CompositeHealth.of(results);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Liveness vs Readiness
    // ─────────────────────────────────────────────────────────────────────────

    public record ProbeComparison(
            String aspect,
            String liveness,
            String readiness) {}

    public static List<ProbeComparison> livenessVsReadiness() {
        return List.of(
            new ProbeComparison("Question asked",
                "Is the app alive and not deadlocked?",
                "Is the app ready to serve traffic?"),
            new ProbeComparison("On failure",
                "Kubernetes restarts the Pod",
                "Kubernetes removes Pod from Service endpoints (no restart)"),
            new ProbeComparison("Typical checks",
                "Is the event loop running? No fatal error?",
                "Is DB connection pool available? Is startup complete?"),
            new ProbeComparison("Spring Boot path",
                "/actuator/health/liveness",
                "/actuator/health/readiness"),
            new ProbeComparison("State enum",
                "LivenessState.CORRECT / BROKEN",
                "ReadinessState.ACCEPTING_TRAFFIC / REFUSING_TRAFFIC")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // K8s probe guide
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> k8sProbeGuide() {
        return List.of(
            "Add management.health.livenessState.enabled=true",
            "Add management.health.readinessState.enabled=true",
            "Expose: management.endpoints.web.exposure.include=health",
            "livenessProbe: httpGet path=/actuator/health/liveness port=8080",
            "readinessProbe: httpGet path=/actuator/health/readiness port=8080",
            "Set initialDelaySeconds to give app time to start (e.g. 30s)",
            "Set failureThreshold=3 before restarting/removing from load balancer"
        );
    }
}
