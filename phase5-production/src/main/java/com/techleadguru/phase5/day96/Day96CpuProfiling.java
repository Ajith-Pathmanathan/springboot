package com.techleadguru.phase5.day96;

import java.util.*;

/**
 * DAY 96 — CPU Profiling: async-profiler, JFR, Flame Graphs
 *
 * WHY SAMPLING PROFILERS?
 *   Instrumentation profilers (add timing to every method) have 3–10x overhead.
 *   Sampling profilers (interrupt threads periodically, record stack) have < 2% overhead.
 *   Production: always use sampling profilers.
 *
 * ASYNC-PROFILER:
 *   Open source, < 1% overhead, Linux + macOS.
 *   Captures: CPU, heap allocations, wall-clock time, lock contention.
 *   Output: flamegraph HTML, JFR format, collapsed text.
 *
 *   Download: https://github.com/async-profiler/async-profiler
 *
 *   COMMANDS:
 *   # Start profiling for 60 seconds, output flamegraph
 *   asprof -d 60 -f /tmp/flamegraph.html <pid>
 *
 *   # Profile allocations (find where memory is allocated)
 *   asprof -e alloc -d 30 -f /tmp/alloc.html <pid>
 *
 *   # Profile wall-clock time (shows sleeping/blocked threads too)
 *   asprof -e wall -d 30 -f /tmp/wall.html <pid>
 *
 *   # Lock contention profiling
 *   asprof -e lock -d 30 -f /tmp/lock.html <pid>
 *
 *   # One-liner for K8s pod:
 *   kubectl exec -it <pod> -- asprof -d 60 -f /tmp/cpu.html $(pgrep -f 'java')
 *   kubectl cp <pod>:/tmp/cpu.html ./cpu_flamegraph.html
 *
 * JAVA FLIGHT RECORDER (JFR):
 *   Built into JDK, zero-overhead for many events.
 *   Captures: GC, JIT, threads, I/O, exceptions, HTTP requests.
 *
 *   # Start continuous JFR (circular buffer — no disk write until dump)
 *   jcmd <pid> JFR.start name=continuous settings=default maxsize=256m
 *
 *   # Dump snapshot
 *   jcmd <pid> JFR.dump name=continuous filename=/tmp/snapshot.jfr
 *
 *   # Analyze in: JDK Mission Control (JMC), IntelliJ Profiler, JFR Analyzer (web)
 *
 * READING A FLAME GRAPH:
 *   X axis: stack depth proportion (not time — alphabetical/sorted by name)
 *   Y axis: call stack (bottom = main/run, top = deepest frame)
 *   Wide bars at top = methods consuming most CPU
 *   Flat tops = leaf methods (CPU actually spent here)
 *   Plateau = hot method to optimize
 *
 * COMMON HOT SPOTS IN SPRING APPS:
 *   - JSON serialization (Jackson) — use MixIns, avoid reflection, consider streaming
 *   - Database query building — check Hibernate N+1 (Day88)
 *   - String manipulation — see Day97 (JMH benchmarks)
 *   - Reflection (Spring proxy, AOP) — cache Method instances, avoid excessive proxying
 *   - Logging (log.debug() building the string) — use isDebugEnabled() guard
 */
public class Day96CpuProfiling {

    // =========================================================================
    // Profiler command generator
    // =========================================================================

    public static class ProfilerConfig {

        public enum ProfileMode { CPU, ALLOC, WALL, LOCK }

        /**
         * Generate async-profiler command for the given PID and mode.
         */
        public static String asyncProfilerCommand(long pid, int durationSec,
                                                   ProfileMode mode, String outputFile) {
            String event = switch (mode) {
                case CPU   -> "cpu";
                case ALLOC -> "alloc";
                case WALL  -> "wall";
                case LOCK  -> "lock";
            };
            return String.format("asprof -e %s -d %d -f %s %d", event, durationSec, outputFile, pid);
        }

        /**
         * JFR start command.
         */
        public static String jfrStartCommand(long pid, String name, int maxSizeMb) {
            return String.format("jcmd %d JFR.start name=%s settings=default maxsize=%dm",
                    pid, name, maxSizeMb);
        }

        /**
         * JFR dump command.
         */
        public static String jfrDumpCommand(long pid, String name, String outputFile) {
            return String.format("jcmd %d JFR.dump name=%s filename=%s", pid, name, outputFile);
        }

        /**
         * JFR stop command.
         */
        public static String jfrStopCommand(long pid, String name) {
            return String.format("jcmd %d JFR.stop name=%s", pid, name);
        }

        /**
         * One-liner: attach async-profiler to current JVM PID.
         */
        public static String flameGraphCommand(long pid, int durationSec, String outputFile) {
            return asyncProfilerCommand(pid, durationSec, ProfileMode.CPU, outputFile);
        }
    }

    // =========================================================================
    // Common hotspot patterns and fixes
    // =========================================================================

    public record HotspotPattern(String symptom, String rootCause, String fix) {}

    public static List<HotspotPattern> commonPatterns() {
        return List.of(
                new HotspotPattern(
                        "Wide plateau on Jackson ObjectMapper.readValue()",
                        "Creating new ObjectMapper per request (expensive)",
                        "Use a singleton ObjectMapper bean (it's thread-safe)"
                ),
                new HotspotPattern(
                        "Wide plateau on Hibernate SQL building (HQL → SQL)",
                        "N+1 query problem — loading child entities one by one",
                        "Use JOIN FETCH, @EntityGraph, or DTO projections (Day88)"
                ),
                new HotspotPattern(
                        "Wide plateau on String concat in hot loop",
                        "String + in loops creates O(n²) byte copies",
                        "Use StringBuilder or String.join() (Day97)"
                ),
                new HotspotPattern(
                        "Wide plateau on java.lang.reflect.Method.invoke()",
                        "Excessive Spring AOP proxying / reflection per request",
                        "Cache Method instances, reduce proxy depth, use @Transactional judiciously"
                ),
                new HotspotPattern(
                        "Wide plateau on LoggingSystem / Logger.debug()",
                        "Building log string even when debug is disabled",
                        "Use log.debug(() -> \"msg \" + expensive) or if (log.isDebugEnabled())"
                )
        );
    }
}
