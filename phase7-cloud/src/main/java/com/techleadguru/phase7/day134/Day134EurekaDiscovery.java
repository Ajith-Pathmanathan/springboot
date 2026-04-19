package com.techleadguru.phase7.day134;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 134 — Eureka: service registration, heartbeat, and discovery.
 *
 * Eureka is a REST-based service registry (Netflix OSS).
 * Each service instance registers itself and sends heartbeats every 30s.
 * The server evicts instances that miss three consecutive heartbeats (90s).
 *
 * Key annotations:
 *   @EnableEurekaServer  — on the registry application
 *   @EnableDiscoveryClient (or @EnableEurekaClient) — on each service
 *
 * How discovery works:
 *   1. Service registers on startup → POST /eureka/apps/{appName}
 *   2. Client fetches registry → GET /eureka/apps
 *   3. Client caches registry locally (delta refresh every 30s)
 *   4. Heartbeat (renew) → PUT /eureka/apps/{appName}/{instanceId}
 *   5. De-registration on shutdown → DELETE /eureka/apps/{appName}/{instanceId}
 */
public class Day134EurekaDiscovery {

    // ─────────────────────────────────────────────────────────────────────────
    // Domain model (simulates Eureka registry in-memory)
    // ─────────────────────────────────────────────────────────────────────────

    public enum InstanceStatus { UP, DOWN, STARTING, OUT_OF_SERVICE, UNKNOWN }

    public record ServiceInstance(
            String appName,
            String instanceId,
            String hostName,
            int    port,
            InstanceStatus status) {}

    // ─────────────────────────────────────────────────────────────────────────
    // In-memory Eureka registry simulator
    // ─────────────────────────────────────────────────────────────────────────

    public static class EurekaRegistrySimulator {

        /** appName (uppercase) → instanceId → ServiceInstance */
        private final Map<String, Map<String, ServiceInstance>> registry = new ConcurrentHashMap<>();

        /** Register or re-register an instance. */
        public void register(ServiceInstance instance) {
            registry.computeIfAbsent(instance.appName().toUpperCase(), k -> new ConcurrentHashMap<>())
                    .put(instance.instanceId(), instance);
        }

        /** Deregister an instance (called on graceful shutdown). */
        public void deregister(String appName, String instanceId) {
            Map<String, ServiceInstance> app = registry.get(appName.toUpperCase());
            if (app != null) app.remove(instanceId);
        }

        /** Returns all UP instances for the given app name. */
        public List<ServiceInstance> getUpInstances(String appName) {
            Map<String, ServiceInstance> app = registry.get(appName.toUpperCase());
            if (app == null) return List.of();
            return app.values().stream()
                    .filter(i -> i.status() == InstanceStatus.UP)
                    .toList();
        }

        /** Returns all registered app names. */
        public Set<String> registeredApps() { return Collections.unmodifiableSet(registry.keySet()); }

        /** Total count of all registered instances across all apps. */
        public int totalInstanceCount() {
            return registry.values().stream().mapToInt(Map::size).sum();
        }

        public void clear() { registry.clear(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Heartbeat tracker
    // ─────────────────────────────────────────────────────────────────────────

    public record HeartbeatConfig(
            int renewalIntervalSeconds,   // default 30
            int evictionThresholdSeconds) {} // default 90 (3 missed heartbeats)

    public static HeartbeatConfig defaultHeartbeatConfig() {
        return new HeartbeatConfig(30, 90);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Key concepts reference
    // ─────────────────────────────────────────────────────────────────────────

    public record EurekaConcept(String concept, String description, String springAnnotation) {}

    public static List<EurekaConcept> keyConcepts() {
        return List.of(
            new EurekaConcept("Eureka Server",
                "Holds the service registry; exposes REST API",
                "@EnableEurekaServer"),
            new EurekaConcept("Eureka Client",
                "Registers itself; fetches registry for discovery",
                "@EnableDiscoveryClient"),
            new EurekaConcept("Heartbeat",
                "PUT /eureka/apps/{app}/{id} every 30s to stay registered",
                "eureka.instance.lease-renewal-interval-in-seconds=30"),
            new EurekaConcept("Eviction",
                "Server removes instance missing heartbeats for >90s",
                "eureka.server.eviction-interval-timer-in-ms=60000"),
            new EurekaConcept("Self-Preservation",
                "Pauses evictions when too many instances disappear at once (split-brain guard)",
                "eureka.server.enable-self-preservation=true"),
            new EurekaConcept("Delta Fetch",
                "Client refreshes only changes every 30s, not full registry",
                "eureka.client.registry-fetch-interval-seconds=30")
        );
    }

    /** Returns the minimal Spring Boot properties to enable a Eureka client. */
    public static Map<String, String> clientProperties(String serviceId, String eurekaServerUrl) {
        return Map.of(
            "spring.application.name",               serviceId,
            "eureka.client.service-url.defaultZone", eurekaServerUrl + "/eureka/",
            "eureka.instance.prefer-ip-address",     "true"
        );
    }
}
