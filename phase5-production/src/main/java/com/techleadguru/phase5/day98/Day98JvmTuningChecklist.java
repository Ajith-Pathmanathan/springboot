package com.techleadguru.phase5.day98;

import java.lang.management.*;
import java.util.*;

/**
 * DAY 98 — JVM Tuning Checklist for Production
 *
 * PRODUCTION JVM BASELINE (Spring Boot, containerized):
 *
 *   MEMORY:
 *     -Xms = -Xmx                — prevent heap resize (reduces GC pressure)
 *     -XX:MaxMetaspaceSize=256m  — prevent metaspace from growing unbounded
 *     -XX:MaxDirectMemorySize=256m — cap direct buffers (Netty, NIO)
 *     Container: set -Xmx to 75% of container memory limit
 *     Kubernetes: requests.memory = limits.memory (guaranteed QoS)
 *
 *   GC:
 *     -XX:+UseG1GC  (default Java 9+, explicit for clarity)
 *     -XX:MaxGCPauseMillis=200
 *     -XX:G1HeapRegionSize=(heap/2048, round to power of 2, 1-32m)
 *     -Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags:filecount=5,filesize=20m
 *     For latency-sensitive: -XX:+UseZGC (Java 17+ LTS)
 *
 *   THREADS:
 *     Virtual threads (Java 21): -Djdk.virtualThreadScheduler.parallelism=N
 *     Carrier thread count = CPU cores (default)
 *
 *   CRASH RECOVERY:
 *     -XX:+HeapDumpOnOutOfMemoryError
 *     -XX:HeapDumpPath=/var/dumps/
 *     -XX:+ExitOnOutOfMemoryError  — restart cleanly rather than limp along
 *
 *   JIT:
 *     -XX:+TieredCompilation  (default, levels 1-4)
 *     -XX:ReservedCodeCacheSize=256m  — prevent "CodeCache is full" in large apps
 *
 *   PROFILING (low overhead):
 *     -XX:+UnlockDiagnosticVMOptions
 *     -XX:+DebugNonSafepoints  — needed for accurate async-profiler stack traces
 *
 *   CONTAINER-AWARE (Java 11+, automatic):
 *     -XX:+UseContainerSupport  (default enabled)
 *     -XX:InitialRAMPercentage=50.0
 *     -XX:MaxRAMPercentage=75.0
 *
 * ANTI-PATTERNS TO AVOID:
 *   ❌ -Xms != -Xmx (heap resize causes GC pauses)
 *   ❌ No MaxMetaspaceSize (classloader leaks silently grow Metaspace)
 *   ❌ -XX:+DisableExplicitGC (breaks DirectBuffer cleanup with old NIO code)
 *   ❌ GC logging to /dev/null (you will regret this during an incident)
 *   ❌ Container limit != JVM Xmx (JVM sees host memory → allocates too much)
 */
public class Day98JvmTuningChecklist {

    // =========================================================================
    // Recommended flags as structured data
    // =========================================================================

    public record JvmFlag(String flag, String description, String category) {}

    public static class TuningChecklist {

        /**
         * Returns recommended production JVM flags.
         */
        public static Map<String, String> getProductionFlags() {
            Map<String, String> flags = new LinkedHashMap<>();
            // Memory
            flags.put("-Xms", "Set equal to -Xmx to avoid heap resize");
            flags.put("-Xmx", "75% of container memory limit");
            flags.put("-XX:MaxMetaspaceSize=256m", "Prevent classloader leak growth");
            flags.put("-XX:MaxDirectMemorySize=256m", "Cap NIO/Netty direct buffers");
            // GC
            flags.put("-XX:+UseG1GC", "Default for Java 9+, explicit for clarity");
            flags.put("-XX:MaxGCPauseMillis=200", "G1GC pause target");
            flags.put("-Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags:filecount=5,filesize=20m",
                    "GC log rotation");
            // OOM handling
            flags.put("-XX:+HeapDumpOnOutOfMemoryError", "Dump on OOM");
            flags.put("-XX:HeapDumpPath=/var/dumps/", "Dump location");
            flags.put("-XX:+ExitOnOutOfMemoryError", "Restart cleanly instead of limping");
            // JIT
            flags.put("-XX:ReservedCodeCacheSize=256m", "Prevent CodeCache full in large apps");
            // Profiling readiness
            flags.put("-XX:+DebugNonSafepoints", "Accurate async-profiler stack traces");
            // Container awareness
            flags.put("-XX:+UseContainerSupport", "Read cgroup limits (default Java 11+)");
            flags.put("-XX:MaxRAMPercentage=75.0", "Heap = 75% of container limit");
            return Collections.unmodifiableMap(flags);
        }

        /**
         * Analyzes current JVM arguments and flags missing recommended settings.
         */
        public static List<String> analyzeCurrentJvm() {
            List<String> warnings = new ArrayList<>();
            List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();

            boolean hasXms = args.stream().anyMatch(a -> a.startsWith("-Xms"));
            boolean hasXmx = args.stream().anyMatch(a -> a.startsWith("-Xmx"));
            if (!hasXms) warnings.add("MISSING: -Xms — heap may resize on startup (GC pressure)");
            if (!hasXmx) warnings.add("MISSING: -Xmx — heap is unbounded (containerized OOM risk)");

            if (hasXms && hasXmx) {
                // Check if they're equal
                String xms = args.stream().filter(a -> a.startsWith("-Xms")).findFirst().orElse("");
                String xmx = args.stream().filter(a -> a.startsWith("-Xmx")).findFirst().orElse("");
                if (!xms.substring(4).equals(xmx.substring(4))) {
                    warnings.add("WARN: -Xms != -Xmx — heap may grow/shrink, triggering GC pauses");
                }
            }

            if (args.stream().noneMatch(a -> a.contains("MaxMetaspaceSize"))) {
                warnings.add("MISSING: -XX:MaxMetaspaceSize — classloader leaks will silently grow Metaspace");
            }
            if (args.stream().noneMatch(a -> a.contains("HeapDumpOnOutOfMemoryError"))) {
                warnings.add("MISSING: -XX:+HeapDumpOnOutOfMemoryError — OOM will be hard to diagnose");
            }
            if (args.stream().noneMatch(a -> a.contains("ExitOnOutOfMemoryError"))) {
                warnings.add("MISSING: -XX:+ExitOnOutOfMemoryError — JVM may limp after OOM");
            }
            if (args.stream().noneMatch(a -> a.contains("gc") && a.contains("log"))) {
                warnings.add("MISSING: GC logging — GC analysis will be impossible post-incident");
            }

            return warnings;
        }

        /**
         * Returns the full recommended JVM args string for Spring Boot in containers.
         */
        public static String recommendedArgsSnippet() {
            return """
                    JAVA_OPTS="
                      -Xms512m -Xmx512m
                      -XX:MaxMetaspaceSize=256m
                      -XX:MaxDirectMemorySize=256m
                      -XX:+UseG1GC
                      -XX:MaxGCPauseMillis=200
                      -XX:G1HeapRegionSize=16m
                      -Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags:filecount=5,filesize=20m
                      -XX:+HeapDumpOnOutOfMemoryError
                      -XX:HeapDumpPath=/var/dumps/
                      -XX:+ExitOnOutOfMemoryError
                      -XX:ReservedCodeCacheSize=256m
                      -XX:+UseContainerSupport
                      -XX:+DebugNonSafepoints
                    "
                    """;
        }
    }
}
