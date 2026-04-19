package com.techleadguru.phase7.day142;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day 142 — @RefreshScope and runtime configuration refresh
 *
 * @RefreshScope beans are re-created when /actuator/refresh is called.
 * The bean proxy returns to an uninitialized state on refresh;
 * the next method call triggers re-injection from the current Environment.
 *
 * Common trigger: Git push → Config Server updated → POST /actuator/refresh
 * or Spring Cloud Bus (RabbitMQ/Kafka) for mass-broadcast refresh.
 */
public class Day142RefreshScope {

    // ─────────────────────────────────────────────────────────────────────────
    // Refreshable config holder (models a @RefreshScope @ConfigurationProperties)
    // ─────────────────────────────────────────────────────────────────────────

    public static class RefreshableConfig {

        private volatile String  value;
        private volatile Instant lastRefreshed;

        public RefreshableConfig(String initialValue) {
            this.value         = initialValue;
            this.lastRefreshed = Instant.now();
        }

        public String  getValue()        { return value; }
        public Instant getLastRefreshed() { return lastRefreshed; }

        /** Simulates Spring re-injecting the field from the updated Environment. */
        public void refresh(String newValue) {
            this.value         = newValue;
            this.lastRefreshed = Instant.now();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Registry simulating the RefreshScope proxy store
    // ─────────────────────────────────────────────────────────────────────────

    public static class RefreshScopeSimulator {

        private final Map<String, RefreshableConfig> registry = new ConcurrentHashMap<>();
        private final AtomicInteger refreshCount = new AtomicInteger(0);

        public void register(String name, RefreshableConfig config) {
            registry.put(name, config);
        }

        /** Refresh one bean with a new value. */
        public boolean refresh(String name, String newValue) {
            RefreshableConfig cfg = registry.get(name);
            if (cfg == null) return false;
            cfg.refresh(newValue);
            refreshCount.incrementAndGet();
            return true;
        }

        public RefreshableConfig get(String name) {
            return registry.get(name);
        }

        public int refreshedCount() { return refreshCount.get(); }
        public int registeredCount() { return registry.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // How refresh works — step-by-step
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> howRefreshWorks() {
        return List.of(
            "1. Developer pushes new property to Git config repo",
            "2. Config Server immediately serves the updated value",
            "3. POST /actuator/refresh triggers ContextRefresher on the client",
            "4. Spring re-reads all @ConfigurationProperties and @Value bindings",
            "5. Beans annotated @RefreshScope have their proxy cache cleared",
            "6. Next method call on a @RefreshScope bean creates a new instance",
            "7. New instance is initialized with values from the refreshed Environment"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Common pitfalls
    // ─────────────────────────────────────────────────────────────────────────

    public record Pitfall(String issue, String explanation, String remedy) {}

    public static List<Pitfall> commonPitfalls() {
        return List.of(
            new Pitfall(
                "@Value in @Configuration class not refreshed",
                "@Configuration classes are singletons; @Value fields are set once at startup",
                "Use @RefreshScope on @Configuration, or inject via @ConfigurationProperties"),
            new Pitfall(
                "Circular proxy issue",
                "@RefreshScope beans proxied by CGLIB; final methods cause proxy failure",
                "Avoid final methods in @RefreshScope beans"),
            new Pitfall(
                "Stateful beans lose state on refresh",
                "Cache or connection pools inside @RefreshScope beans are destroyed on refresh",
                "Separate stateful resources from refreshable config; use lifecycle hooks"),
            new Pitfall(
                "Refresh not propagated to all instances",
                "POST /actuator/refresh only refreshes the target instance",
                "Use Spring Cloud Bus (RabbitMQ/Kafka) to broadcast to all instances"),
            new Pitfall(
                "spring.config.import not set",
                "Without spring.config.import client cannot connect to Config Server",
                "Add spring.config.import=optional:configserver:http://localhost:8888")
        );
    }
}
