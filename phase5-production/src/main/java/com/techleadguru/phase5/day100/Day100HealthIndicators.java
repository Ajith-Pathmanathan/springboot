package com.techleadguru.phase5.day100;

import org.springframework.boot.actuate.health.*;
import org.springframework.boot.availability.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DAY 100 — Health Indicators: Liveness vs Readiness
 *
 * LIVENESS vs READINESS:
 *
 *   LIVENESS — "Is the app alive? Should it be restarted?"
 *     Failing liveness → Kubernetes restarts the pod
 *     Use when: application entered an unrecoverable state (deadlock, infinite loop)
 *     DO NOT fail liveness for: slow database, downstream service down
 *     (that would cause restart storm — all pods restart simultaneously)
 *
 *   READINESS — "Should traffic be sent to this app?"
 *     Failing readiness → Kubernetes removes pod from service endpoints
 *     Use when: database connection lost, warmup not complete, cache not populated
 *     RECOVERING readiness is expected and normal (pod stays up, just gets no traffic)
 *
 * SPRING BOOT HEALTH GROUPS:
 *   GET /actuator/health/liveness   → LivenessStateHealthIndicator
 *   GET /actuator/health/readiness  → ReadinessStateHealthIndicator
 *
 *   application.properties:
 *     management.endpoint.health.probes.enabled=true
 *     management.health.livenessState.enabled=true
 *     management.health.readinessState.enabled=true
 *
 * K8S PROBE CONFIG:
 *   livenessProbe:
 *     httpGet:
 *       path: /actuator/health/liveness
 *       port: 8080
 *     initialDelaySeconds: 30    # wait for app to start
 *     periodSeconds: 10
 *     failureThreshold: 3        # restart after 3 consecutive failures
 *
 *   readinessProbe:
 *     httpGet:
 *       path: /actuator/health/readiness
 *       port: 8080
 *     initialDelaySeconds: 10
 *     periodSeconds: 5
 *     failureThreshold: 2        # remove from LB after 2 failures
 *
 * CUSTOM HEALTH INDICATOR:
 *   Implement HealthIndicator or AbstractHealthIndicator.
 *   Return Health.up() / Health.down() / Health.outOfService() with details.
 */
public class Day100HealthIndicators {

    // =========================================================================
    // Custom HealthIndicator — checks downstream service reachability
    // =========================================================================

    @Component("downstreamService")
    public static class ExternalServiceHealthIndicator extends AbstractHealthIndicator {

        private final String serviceUrl;
        private final Duration timeout;

        public ExternalServiceHealthIndicator() {
            this("https://httpbin.org/get", Duration.ofSeconds(3));
        }

        public ExternalServiceHealthIndicator(String serviceUrl, Duration timeout) {
            this.serviceUrl = serviceUrl;
            this.timeout     = timeout;
        }

        @Override
        protected void doHealthCheck(Health.Builder builder) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(timeout)
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(serviceUrl))
                        .timeout(timeout)
                        .GET()
                        .build();

                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    builder.up()
                            .withDetail("url", serviceUrl)
                            .withDetail("statusCode", status)
                            .withDetail("responseTime", "< " + timeout.toMillis() + "ms");
                } else {
                    builder.down()
                            .withDetail("url", serviceUrl)
                            .withDetail("statusCode", status)
                            .withDetail("reason", "Non-2xx response");
                }
            } catch (java.net.http.HttpTimeoutException e) {
                builder.down()
                        .withDetail("url", serviceUrl)
                        .withDetail("reason", "Timeout after " + timeout.toMillis() + "ms");
            } catch (Exception e) {
                builder.down()
                        .withDetail("url", serviceUrl)
                        .withDetail("reason", e.getMessage());
            }
        }
    }

    // =========================================================================
    // Custom HealthIndicator — database record count sanity check
    // =========================================================================

    @Component("appDatabaseSanity")
    public static class DatabaseSanityHealthIndicator implements HealthIndicator {

        private final int minExpectedRecords;
        private int currentRecordCount; // set by tests/service

        public DatabaseSanityHealthIndicator() {
            this(0);
        }

        public DatabaseSanityHealthIndicator(int minExpectedRecords) {
            this.minExpectedRecords = minExpectedRecords;
        }

        public void setCurrentRecordCount(int count) {
            this.currentRecordCount = count;
        }

        @Override
        public Health health() {
            if (currentRecordCount >= minExpectedRecords) {
                return Health.up()
                        .withDetail("recordCount", currentRecordCount)
                        .withDetail("minExpected", minExpectedRecords)
                        .build();
            }
            return Health.down()
                    .withDetail("recordCount", currentRecordCount)
                    .withDetail("minExpected", minExpectedRecords)
                    .withDetail("reason", "Record count below minimum threshold")
                    .build();
        }
    }

    // =========================================================================
    // Programmatic liveness / readiness state change
    // =========================================================================

    @Component
    public static class AppAvailabilityManager {

        private final ApplicationEventPublisher events;
        private final ApplicationAvailability availability;

        public AppAvailabilityManager(ApplicationEventPublisher events,
                                      ApplicationAvailability availability) {
            this.events = events;
            this.availability = availability;
        }

        /**
         * Mark application as live (recovers from a prior broken state).
         */
        public void markLive() {
            AvailabilityChangeEvent.publish(events, this, LivenessState.CORRECT);
        }

        /**
         * Mark application as broken — Kubernetes will restart the pod.
         * Use only for truly unrecoverable states (deadlock, data corruption).
         */
        public void markBroken() {
            AvailabilityChangeEvent.publish(events, this, LivenessState.BROKEN);
        }

        /**
         * Mark application ready to accept traffic.
         */
        public void markReady() {
            AvailabilityChangeEvent.publish(events, this, ReadinessState.ACCEPTING_TRAFFIC);
        }

        /**
         * Mark application NOT ready — removes from load-balancer endpoints.
         * Use for: warmup, DB reconnection, maintenance mode.
         */
        public void markNotReady() {
            AvailabilityChangeEvent.publish(events, this, ReadinessState.REFUSING_TRAFFIC);
        }
    }

    // =========================================================================
    // Testable liveness state machine (no Spring context required)
    // =========================================================================

    public static class LivenessStateMachine {

        private final AtomicBoolean broken = new AtomicBoolean(false);
        private volatile String reason = "OK";

        public void markBroken(String reason) {
            this.reason = reason;
            this.broken.set(true);
        }

        public void recover() {
            this.reason = "OK";
            this.broken.set(false);
        }

        public boolean isBroken()  { return broken.get(); }
        public String getReason()  { return reason; }

        public Health toHealth() {
            if (!broken.get()) {
                return Health.up().withDetail("reason", reason).build();
            }
            return Health.down().withDetail("reason", reason).build();
        }
    }
}
