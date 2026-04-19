package com.techleadguru.phase7.day135;

import java.util.*;

/**
 * Day 135 — Eureka internals: eviction, self-preservation mode, and delta updates.
 *
 * EVICTION:
 *   Eureka server runs an eviction task every 60s (default).
 *   An instance is evicted if its lastRenewalTimestamp is older than:
 *     (duration * renewalPercentThreshold) seconds
 *   Default renewal interval: 30s → expected renewals per minute per instance: 2
 *
 * SELF-PRESERVATION MODE:
 *   When the server receives fewer renewals than
 *   (expectedRenewalsPerMin * renewalPercentThreshold) in the last minute,
 *   it pauses evictions and logs:
 *     "EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP WHEN THEY'RE NOT."
 *   This prevents cascading failures during network partitions (CAP: AP over CP).
 *
 * PROD TIP: Disable self-preservation in dev/test; keep on in prod.
 */
public class Day135EurekaInternals {

    // ─────────────────────────────────────────────────────────────────────────
    // Eviction simulation
    // ─────────────────────────────────────────────────────────────────────────

    public record InstanceLease(
            String instanceId,
            long   lastRenewalMs,   // epoch-millis of last heartbeat
            int    durationSeconds) {} // eviction threshold (default 90)

    /** Returns true if the lease should be evicted given the current time. */
    public static boolean shouldEvict(InstanceLease lease, long nowMs) {
        long evictAfterMs = lease.lastRenewalMs() + (long) lease.durationSeconds() * 1000;
        return nowMs > evictAfterMs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Self-preservation calculator
    // ─────────────────────────────────────────────────────────────────────────

    public record SelfPreservationCheck(
            int  registeredInstances,
            long renewalsReceivedLastMinute,
            double renewalPercentThreshold,
            boolean selfPreservationTriggered) {}

    /**
     * Determines whether self-preservation should trigger.
     *
     * expectedRenewalsPerMin = registeredInstances * 2  (heartbeat every 30s)
     * threshold = expectedRenewalsPerMin * renewalPercentThreshold (default 0.85)
     */
    public static SelfPreservationCheck evaluateSelfPreservation(
            int registeredInstances,
            long renewalsReceivedLastMinute,
            double renewalPercentThreshold) {

        double expected = registeredInstances * 2.0;
        double threshold = expected * renewalPercentThreshold;
        boolean triggered = renewalsReceivedLastMinute < threshold;

        return new SelfPreservationCheck(
                registeredInstances, renewalsReceivedLastMinute,
                renewalPercentThreshold, triggered);
    }

    public static double defaultRenewalPercentThreshold() { return 0.85; }

    // ─────────────────────────────────────────────────────────────────────────
    // Delta updates
    // ─────────────────────────────────────────────────────────────────────────

    public enum DeltaAction { ADDED, MODIFIED, DELETED }

    public record DeltaEntry(String instanceId, DeltaAction action, String appName) {}

    /** Applies a list of delta entries to a current registry snapshot. */
    public static Map<String, String> applyDelta(
            Map<String, String> current,  // instanceId → appName
            List<DeltaEntry> delta) {
        Map<String, String> result = new HashMap<>(current);
        for (DeltaEntry entry : delta) {
            switch (entry.action()) {
                case ADDED, MODIFIED -> result.put(entry.instanceId(), entry.appName());
                case DELETED         -> result.remove(entry.instanceId());
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Configuration guide
    // ─────────────────────────────────────────────────────────────────────────

    public record ConfigTip(String property, String defaultValue, String explanation) {}

    public static List<ConfigTip> configurationTips() {
        return List.of(
            new ConfigTip("eureka.server.enable-self-preservation", "true",
                "Keep true in prod to avoid mass evictions during network blips"),
            new ConfigTip("eureka.server.eviction-interval-timer-in-ms", "60000",
                "How often to run eviction task (ms); lower in dev for fast cleanup"),
            new ConfigTip("eureka.instance.lease-renewal-interval-in-seconds", "30",
                "How often client sends heartbeats; lower = faster detection of failure"),
            new ConfigTip("eureka.instance.lease-expiration-duration-in-seconds", "90",
                "Server evicts after missing this many seconds of heartbeats"),
            new ConfigTip("eureka.client.registry-fetch-interval-seconds", "30",
                "How often client refreshes registry cache (delta fetch)")
        );
    }
}
