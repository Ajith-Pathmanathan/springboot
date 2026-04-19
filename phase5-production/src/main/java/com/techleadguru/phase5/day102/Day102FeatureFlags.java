package com.techleadguru.phase5.day102;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAY 102 — Feature Flags: Dynamic Config Without Restart
 *
 * PROBLEM WITH STATIC CONFIGURATION:
 *   Changing a property requires redeployment (minutes of risk).
 *   Feature flags allow runtime changes: enable/disable features gradually.
 *
 * PATTERNS:
 *   1. In-memory map (this day) — simplest, not persistent, single-node only
 *   2. Database-backed — survives restarts, consistent across instances
 *   3. External service (LaunchDarkly, Unleash, AWS AppConfig) — rollout %, A/B
 *   4. Spring Cloud Config + /actuator/refresh — property file based
 *
 * USE CASES FOR FEATURE FLAGS:
 *   - Dark launch: deploy new code but gate it behind a flag
 *   - Canary release: enable for 5% of users, watch metrics, then 100%
 *   - Kill switch: instantly disable a broken feature without redeploy
 *   - A/B testing: different behavior for different user segments
 *
 * ACTUATOR INTEGRATION:
 *   See Day99 FeatureFlagsEndpoint:
 *   GET  /actuator/feature-flags         → list all flags
 *   GET  /actuator/feature-flags/{name}  → get one flag
 *   POST /actuator/feature-flags/{name}  → {"enabled": true}
 *
 * IMPLEMENTATION RULES:
 *   - Thread-safe: ConcurrentHashMap or synchronized
 *   - Fail-open vs fail-closed: if flag check throws, default true or false?
 *   - Avoid flag sprawl: delete flags after full rollout
 */
@Service
public class Day102FeatureFlags {

    private final ConcurrentHashMap<String, Boolean> flags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String>  descriptions = new ConcurrentHashMap<>();

    public Day102FeatureFlags() {
        // Default feature flags
        register("new-checkout-flow",      false, "New 3-step checkout (A/B test variant)");
        register("email-async-processing", true,  "Process emails on async thread pool");
        register("beta-dashboard",         false, "New analytics dashboard (internal only)");
        register("stricter-password-policy", false, "Require 12+ char passwords for new users");
    }

    // =========================================================================
    // Core API
    // =========================================================================

    /**
     * Register a feature flag with a description.
     */
    public void register(String flagName, boolean defaultValue, String description) {
        flags.put(flagName, defaultValue);
        descriptions.put(flagName, description);
    }

    /**
     * Check if a flag is enabled.
     * Returns defaultValue if the flag is not registered.
     */
    public boolean isEnabled(String flagName) {
        return flags.getOrDefault(flagName, false);
    }

    public boolean isEnabled(String flagName, boolean defaultIfUnknown) {
        return flags.getOrDefault(flagName, defaultIfUnknown);
    }

    /**
     * Enable a feature flag.
     */
    public void enable(String flagName) {
        flags.put(flagName, true);
    }

    /**
     * Disable a feature flag.
     */
    public void disable(String flagName) {
        flags.put(flagName, false);
    }

    /**
     * Toggle a flag.
     */
    public boolean toggle(String flagName) {
        return flags.compute(flagName, (k, current) -> current == null || !current);
    }

    /**
     * Get all flags as an immutable snapshot.
     */
    public Map<String, Boolean> getAll() {
        return Collections.unmodifiableMap(new HashMap<>(flags));
    }

    /**
     * Get all flags with descriptions.
     */
    public List<FlagInfo> getAllWithDescriptions() {
        List<FlagInfo> result = new ArrayList<>();
        flags.forEach((name, enabled) ->
                result.add(new FlagInfo(name, enabled, descriptions.getOrDefault(name, ""))));
        result.sort(Comparator.comparing(FlagInfo::name));
        return result;
    }

    /**
     * Remove a flag (after full rollout — clean up flag sprawl).
     */
    public boolean remove(String flagName) {
        descriptions.remove(flagName);
        return flags.remove(flagName) != null;
    }

    public int getFlagCount() { return flags.size(); }

    public record FlagInfo(String name, boolean enabled, String description) {}

    // =========================================================================
    // Usage example in a service — how to use feature flags in practice
    // =========================================================================

    /**
     * Demonstrates how application code uses feature flags.
     * The calling code stays clean; all if/else is inside the gate.
     */
    public String processCheckout(String customerId, String cartId) {
        if (isEnabled("new-checkout-flow")) {
            // New path: 3-step guided checkout
            return "new-checkout:" + customerId + ":" + cartId;
        }
        // Legacy path: single-page checkout
        return "legacy-checkout:" + customerId + ":" + cartId;
    }
}
