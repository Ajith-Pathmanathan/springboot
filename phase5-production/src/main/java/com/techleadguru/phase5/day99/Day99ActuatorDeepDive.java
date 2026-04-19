package com.techleadguru.phase5.day99;

import org.springframework.boot.actuate.endpoint.annotation.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAY 99 — Actuator Deep Dive: Custom Endpoints and Security
 *
 * SPRING BOOT ACTUATOR BUILT-IN ENDPOINTS:
 *   /actuator/health          — liveness + readiness (see Day100)
 *   /actuator/metrics         — Micrometer metrics (JVM, HTTP, DB, cache)
 *   /actuator/info            — build/git info (customize with info.*)
 *   /actuator/env             — property sources (NEVER expose in production without auth)
 *   /actuator/beans           — all Spring beans
 *   /actuator/threaddump      — thread dump (see Day91)
 *   /actuator/heapdump        — heap dump download (DANGER: locks JVM briefly)
 *   /actuator/loggers         — change log levels at runtime
 *   /actuator/scheduledtasks  — see @Scheduled methods
 *   /actuator/httptrace       — last N HTTP requests (Spring Boot 3: httpexchanges)
 *
 * SECURITY RULES:
 *   1. NEVER expose /actuator/** without authentication in production
 *   2. Separate management port: management.server.port=8081 (not reachable externally)
 *   3. Expose only what you need:
 *      management.endpoints.web.exposure.include=health,metrics,info,loggers
 *      management.endpoints.web.exposure.exclude=env,beans,heapdump
 *   4. If internal management port not possible, secure with Spring Security:
 *      requestMatchers("/actuator/**").hasRole("ACTUATOR_ADMIN")
 *
 * CUSTOM ENDPOINT ANNOTATIONS:
 *   @Endpoint(id="my-endpoint")       — accessible via JMX + HTTP
 *   @WebEndpoint(id="my-endpoint")    — HTTP only
 *   @JmxEndpoint(id="my-endpoint")    — JMX only
 *   @ReadOperation  — HTTP GET
 *   @WriteOperation — HTTP POST
 *   @DeleteOperation — HTTP DELETE
 *   @Selector — path variable in the endpoint URL
 */
public class Day99ActuatorDeepDive {

    // =========================================================================
    // Custom actuator endpoint: feature flags (exposed as /actuator/feature-flags)
    // =========================================================================

    @Component
    @Endpoint(id = "feature-flags")
    public static class FeatureFlagsEndpoint {

        private final Map<String, Object> flags = new ConcurrentHashMap<>();

        public FeatureFlagsEndpoint() {
            // Default feature flags
            flags.put("new-checkout-flow", false);
            flags.put("dark-mode-ui", true);
            flags.put("beta-analytics", false);
            flags.put("last-updated", Instant.now().toString());
        }

        @ReadOperation
        public Map<String, Object> getAll() {
            return Collections.unmodifiableMap(flags);
        }

        @ReadOperation
        public Object getFlag(@Selector String flagName) {
            return flags.getOrDefault(flagName, "NOT_FOUND");
        }

        @WriteOperation
        public Map<String, Object> setFlag(@Selector String flagName, boolean enabled) {
            flags.put(flagName, enabled);
            flags.put("last-updated", Instant.now().toString());
            return Map.of("flag", flagName, "enabled", enabled, "status", "updated");
        }

        @DeleteOperation
        public Map<String, Object> removeFlag(@Selector String flagName) {
            Object removed = flags.remove(flagName);
            if (removed == null) {
                return Map.of("flag", flagName, "status", "NOT_FOUND");
            }
            return Map.of("flag", flagName, "status", "removed");
        }

        // For tests — direct access
        public boolean isFlagEnabled(String flagName) {
            Object value = flags.get(flagName);
            return Boolean.TRUE.equals(value);
        }

        public void setFlagDirect(String flagName, boolean enabled) {
            flags.put(flagName, enabled);
        }

        public int getFlagCount() {
            return (int) flags.entrySet().stream()
                    .filter(e -> e.getValue() instanceof Boolean)
                    .count();
        }
    }

    // =========================================================================
    // Custom endpoint: application info
    // =========================================================================

    @Component
    @Endpoint(id = "app-diagnostics")
    public static class AppDiagnosticsEndpoint {

        private final Instant startTime = Instant.now();

        @ReadOperation
        public Map<String, Object> getDiagnostics() {
            Runtime rt = Runtime.getRuntime();
            return Map.of(
                    "startedAt", startTime.toString(),
                    "uptimeMs", System.currentTimeMillis() - startTime.toEpochMilli(),
                    "heapUsedMb", (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024),
                    "heapMaxMb", rt.maxMemory() / (1024 * 1024),
                    "threadCount", Thread.activeCount(),
                    "availableProcessors", rt.availableProcessors(),
                    "javaVersion", System.getProperty("java.version")
            );
        }
    }

    // =========================================================================
    // Security configuration guidance (not wiring Spring Security here —
    // just the configuration snippet for reference)
    // =========================================================================

    public static class ActuatorSecurityGuide {

        public static String securityConfigSnippet() {
            return """
                    // Recommended Spring Security config for Actuator:
                    @Bean
                    SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
                        return http
                            .securityMatcher("/actuator/**")
                            .authorizeHttpRequests(auth -> auth
                                // Allow health/info without auth (for load balancer probes)
                                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                                // Everything else requires ACTUATOR_ADMIN role
                                .anyRequest().hasRole("ACTUATOR_ADMIN")
                            )
                            .httpBasic(Customizer.withDefaults())
                            .build();
                    }
                    
                    // application.properties:
                    management.server.port=8081           # separate port (not public-facing)
                    management.endpoints.web.exposure.include=health,metrics,info,loggers,threaddump
                    management.endpoints.web.exposure.exclude=env,beans,heapdump,httptrace
                    management.endpoint.health.show-details=when-authorized
                    """;
        }

        public static String exposureRecommendations() {
            return """
                    # SAFE to expose (low risk):
                    health, info, metrics, loggers, scheduledtasks
                    
                    # EXPOSE ONLY INTERNALLY (high risk if public):
                    env        — reveals all config values including passwords
                    beans      — reveals all bean names and types (recon)
                    conditions — reveals autoconfiguration decisions
                    threaddump — reveals thread names (recon)
                    
                    # NEVER EXPOSE PUBLICLY:
                    heapdump   — downloads full heap (contains all data in memory)
                    shutdown   — kills the application
                    """;
        }
    }
}
