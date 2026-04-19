package com.techleadguru.phase5.day105;

import java.util.*;

/**
 * DAY 105 — Phase 5 Production Checklist
 *
 * This is the capstone of Phase 5: Production Problem Solving.
 * Each category consolidates the week's lessons into actionable checklist items.
 *
 * Reference: Days 78-104 all feed into this checklist.
 */
public class Day105Phase5Checklist {

    public enum CheckStatus { REQUIRED, RECOMMENDED, OPTIONAL }

    public record CheckItem(String category, String item, String description,
                            CheckStatus status, String relatedDay) {}

    // =========================================================================
    // Memory checks (Days 78-84)
    // =========================================================================

    public static List<CheckItem> getMemoryChecks() {
        return List.of(
                new CheckItem("Memory", "Set -Xms == -Xmx",
                        "Prevents heap resize; eliminates GC pressure on startup",
                        CheckStatus.REQUIRED, "Day98"),

                new CheckItem("Memory", "Set -XX:MaxMetaspaceSize",
                        "Prevents classloader leaks from silently growing Metaspace",
                        CheckStatus.REQUIRED, "Day98"),

                new CheckItem("Memory", "Set -XX:+HeapDumpOnOutOfMemoryError",
                        "Capture heap dump on OOM for post-mortem analysis (Day79, Day80)",
                        CheckStatus.REQUIRED, "Day79"),

                new CheckItem("Memory", "Use bounded caches (Caffeine with maximumSize)",
                        "Unbounded HashMap caches cause OOM (Day84)",
                        CheckStatus.REQUIRED, "Day84"),

                new CheckItem("Memory", "Eliminate static field leaks",
                        "Static collections that grow unbounded cause memory leaks (Day81)",
                        CheckStatus.REQUIRED, "Day81"),

                new CheckItem("Memory", "Use WeakReference for observer/listener registries",
                        "Listeners not deregistered will hold GC roots (Day82)",
                        CheckStatus.RECOMMENDED, "Day82"),

                new CheckItem("Memory", "Always clean ThreadLocals in try/finally",
                        "Thread pool reuse causes stale data across requests (Day83)",
                        CheckStatus.REQUIRED, "Day83"),

                new CheckItem("Memory", "Periodically analyze heap with MAT",
                        "Schedule heap dump analysis quarterly or after OOM events (Day80)",
                        CheckStatus.RECOMMENDED, "Day80")
        );
    }

    // =========================================================================
    // Threading checks (Days 85-91)
    // =========================================================================

    public static List<CheckItem> getThreadingChecks() {
        return List.of(
                new CheckItem("Threading", "Size thread pools with formula: N*(1+WT/ST)",
                        "Undersized pools exhaust under load (Day85, Day89)",
                        CheckStatus.REQUIRED, "Day85"),

                new CheckItem("Threading", "Set HTTP client timeouts (connect + read)",
                        "Default RestClient has no timeouts — one slow service blocks all threads (Day86)",
                        CheckStatus.REQUIRED, "Day86"),

                new CheckItem("Threading", "Use consistent lock ordering to prevent deadlocks",
                        "Always acquire locks in sorted (ascending ID) order (Day87)",
                        CheckStatus.REQUIRED, "Day87"),

                new CheckItem("Threading", "Disable Open Session in View (OSIV)",
                        "spring.jpa.open-in-view=false; use JOIN FETCH instead (Day88)",
                        CheckStatus.REQUIRED, "Day88"),

                new CheckItem("Threading", "Use CallerRunsPolicy for backpressure",
                        "AbortPolicy drops work silently; CallerRuns applies backpressure (Day89)",
                        CheckStatus.RECOMMENDED, "Day89"),

                new CheckItem("Threading", "Enable graceful shutdown",
                        "server.shutdown=graceful + spring.lifecycle.timeout-per-shutdown-phase=30s (Day90)",
                        CheckStatus.REQUIRED, "Day90"),

                new CheckItem("Threading", "Use bulkhead thread pools per concern",
                        "Isolate fast lanes from slow lanes to prevent saturation cascade (Day103)",
                        CheckStatus.RECOMMENDED, "Day103")
        );
    }

    // =========================================================================
    // GC checks (Days 92-95)
    // =========================================================================

    public static List<CheckItem> getGcChecks() {
        return List.of(
                new CheckItem("GC", "Enable GC logging",
                        "-Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags:filecount=5,filesize=20m (Day94)",
                        CheckStatus.REQUIRED, "Day94"),

                new CheckItem("GC", "Monitor GC overhead percentage",
                        "Alert if GC overhead > 5% (gcPauseMs / wallTime * 100) (Day94)",
                        CheckStatus.REQUIRED, "Day94"),

                new CheckItem("GC", "Use ZGC for latency-sensitive services",
                        "Sub-millisecond GC pauses for payment APIs, trading (Day93)",
                        CheckStatus.OPTIONAL, "Day93"),

                new CheckItem("GC", "Alert on Old Gen usage > 80%",
                        "Old Gen > 80% and climbing = memory leak or heap too small (Day92)",
                        CheckStatus.REQUIRED, "Day92"),

                new CheckItem("GC", "Set -XX:MaxDirectMemorySize",
                        "Cap NIO/Netty direct buffer usage to prevent off-heap OOM (Day95)",
                        CheckStatus.RECOMMENDED, "Day95"),

                new CheckItem("GC", "Monitor Metaspace usage",
                        "Growing Metaspace = classloader leak (often dynamic scripting) (Day95)",
                        CheckStatus.RECOMMENDED, "Day95")
        );
    }

    // =========================================================================
    // Observability checks (Days 96-104)
    // =========================================================================

    public static List<CheckItem> getObservabilityChecks() {
        return List.of(
                new CheckItem("Observability", "Use sampling profiler (async-profiler / JFR)",
                        "< 2% overhead; flamegraphs identify hot methods (Day96)",
                        CheckStatus.RECOMMENDED, "Day96"),

                new CheckItem("Observability", "Benchmark before optimizing (JMH)",
                        "Measure actual performance, not assumed performance (Day97)",
                        CheckStatus.RECOMMENDED, "Day97"),

                new CheckItem("Observability", "Secure Actuator endpoints",
                        "Never expose /actuator/env or /actuator/heapdump without auth (Day99)",
                        CheckStatus.REQUIRED, "Day99"),

                new CheckItem("Observability", "Separate liveness from readiness probes",
                        "Never fail liveness for downstream outages — causes restart storms (Day100)",
                        CheckStatus.REQUIRED, "Day100"),

                new CheckItem("Observability", "Use feature flags for risk reduction",
                        "Kill switches allow disabling broken features without redeploy (Day102)",
                        CheckStatus.RECOMMENDED, "Day102"),

                new CheckItem("Observability", "Use distributed lock for scheduled jobs",
                        "Without lock, N-instance deployments run scheduled jobs N times (Day101)",
                        CheckStatus.REQUIRED, "Day101"),

                new CheckItem("Observability", "Pool HTTP client connections",
                        "Default RestClient opens new TCP per request — costly with HTTPS (Day104)",
                        CheckStatus.REQUIRED, "Day104"),

                new CheckItem("Observability", "Expose key metrics via Actuator + Micrometer",
                        "thread.pool.active, db.pool.active, cache.hit.rate, http.server.requests (Day99)",
                        CheckStatus.REQUIRED, "Day99")
        );
    }

    // =========================================================================
    // Combined summary
    // =========================================================================

    public static List<CheckItem> getAllChecks() {
        List<CheckItem> all = new ArrayList<>();
        all.addAll(getMemoryChecks());
        all.addAll(getThreadingChecks());
        all.addAll(getGcChecks());
        all.addAll(getObservabilityChecks());
        return Collections.unmodifiableList(all);
    }

    public static long countRequired() {
        return getAllChecks().stream()
                .filter(c -> c.status() == CheckStatus.REQUIRED)
                .count();
    }

    public static Map<String, Long> countByCategory() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (CheckItem item : getAllChecks()) {
            counts.merge(item.category(), 1L, Long::sum);
        }
        return counts;
    }
}
