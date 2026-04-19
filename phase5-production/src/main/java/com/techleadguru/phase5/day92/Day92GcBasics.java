package com.techleadguru.phase5.day92;

import java.lang.management.*;
import java.util.*;

/**
 * DAY 92 — GC Basics: Young Gen, Old Gen, Minor GC, Full GC
 *
 * JVM HEAP LAYOUT (G1GC default in Java 11+):
 *   ┌────────────────────────────────────────────────┐
 *   │  Young Generation  │  Old Generation (Tenured)  │
 *   │  Eden + S0 + S1   │  Long-lived objects         │
 *   └────────────────────────────────────────────────┘
 *   Metaspace (off-heap) — class metadata, method bytecodes
 *
 * GC EVENTS:
 *   Minor GC  — collects Young Gen only (fast, frequent, short pause ~1-10ms)
 *   Full GC   — collects entire heap (slow, infrequent, long pause ~100ms-seconds)
 *   Concurrent — G1/ZGC do most work concurrently (pause only for STW phases)
 *
 * OBJECT LIFECYCLE:
 *   1. New object → Eden
 *   2. Eden fills → Minor GC → live objects copied to Survivor (S0 or S1)
 *   3. After N GCs (age threshold, default 15) → Survivor → Old Gen (tenuring)
 *   4. Old Gen fills → Full GC (most expensive)
 *
 * KEY JVM FLAGS:
 *   -Xms<size>   — initial heap (set equal to Xmx to avoid resizing)
 *   -Xmx<size>   — max heap
 *   -Xmn<size>   — Young Gen size (or use -XX:NewRatio)
 *   -XX:+UseG1GC          — G1GC (default Java 9+)
 *   -XX:+UseZGC           — ZGC (Java 15+ production)
 *   -XX:MaxGCPauseMillis=200  — G1GC pause target
 *   -Xlog:gc*:file=/tmp/gc.log:time,uptime,level,tags — GC log
 *
 * METRICS TO WATCH:
 *   jvm.gc.pause → alert if p99 > 500ms
 *   jvm.memory.used{area=heap} → alert if > 80% of max
 *   jvm.gc.memory.promoted → rate of promotion to Old Gen (should be low)
 */
public class Day92GcBasics {

    // =========================================================================
    // Live GC stats from JMX GarbageCollectorMXBeans
    // =========================================================================

    public static class GcStatsReporter {

        private final List<GarbageCollectorMXBean> gcBeans =
                ManagementFactory.getGarbageCollectorMXBeans();

        /**
         * Minor GC collectors have names containing "Young" / "scavenge" / "Copy" / "PS Scavenge".
         * Major GC collectors have names containing "Old" / "Mark" / "MarkSweep" / "G1 Old".
         */
        private boolean isMinorGc(String name) {
            String n = name.toLowerCase();
            return n.contains("young") || n.contains("scavenge") || n.contains("copy") || n.contains("new");
        }

        public long getMinorGcCount() {
            return gcBeans.stream()
                    .filter(b -> isMinorGc(b.getName()))
                    .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                    .sum();
        }

        public long getMajorGcCount() {
            return gcBeans.stream()
                    .filter(b -> !isMinorGc(b.getName()))
                    .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                    .sum();
        }

        public long getMinorGcTimeMs() {
            return gcBeans.stream()
                    .filter(b -> isMinorGc(b.getName()))
                    .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                    .sum();
        }

        public long getMajorGcTimeMs() {
            return gcBeans.stream()
                    .filter(b -> !isMinorGc(b.getName()))
                    .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                    .sum();
        }

        public List<GcBean> getAllBeans() {
            List<GcBean> result = new ArrayList<>();
            for (GarbageCollectorMXBean b : gcBeans) {
                result.add(new GcBean(b.getName(), b.getCollectionCount(),
                        b.getCollectionTime(), isMinorGc(b.getName())));
            }
            return result;
        }

        public record GcBean(String name, long count, long timeMs, boolean isMinor) {}
    }

    // =========================================================================
    // Heap stats from MemoryMXBean
    // =========================================================================

    public static class HeapStatsReporter {

        private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        private final List<MemoryPoolMXBean> poolBeans = ManagementFactory.getMemoryPoolMXBeans();

        public record HeapStats(long usedBytes, long committedBytes, long maxBytes,
                                double usagePercent) {}

        public HeapStats getCurrentHeapStats() {
            MemoryUsage usage = memoryBean.getHeapMemoryUsage();
            double pct = usage.getMax() > 0
                    ? (double) usage.getUsed() / usage.getMax() * 100.0
                    : 0.0;
            return new HeapStats(usage.getUsed(), usage.getCommitted(), usage.getMax(), pct);
        }

        public Optional<MemoryPoolStats> getOldGenStats() {
            return poolBeans.stream()
                    .filter(p -> p.getName().toLowerCase().contains("old")
                            || p.getName().toLowerCase().contains("tenured"))
                    .map(p -> {
                        MemoryUsage u = p.getUsage();
                        double pct = u.getMax() > 0
                                ? (double) u.getUsed() / u.getMax() * 100.0 : 0.0;
                        return new MemoryPoolStats(p.getName(), u.getUsed(), u.getMax(), pct);
                    })
                    .findFirst();
        }

        public record MemoryPoolStats(String name, long usedBytes, long maxBytes,
                                      double usagePercent) {}
    }

    // =========================================================================
    // Allocate objects to trigger GC for demonstration
    // =========================================================================

    public static class GcPressureDemo {

        private static final List<Object> HOLDER = new ArrayList<>();

        /**
         * Allocate short-lived objects to trigger Minor GC.
         * After this call, references are released so they become garbage.
         */
        public static long allocateShortLivedMb(int mb) {
            GcStatsReporter reporter = new GcStatsReporter();
            long before = reporter.getMinorGcCount();

            // Allocate without retaining → young-gen pressure
            for (int i = 0; i < mb; i++) {
                byte[] chunk = new byte[1024 * 1024]; // 1 MB
                // intentionally not retained
                chunk[0] = 1; // prevent dead-code elimination
            }

            return reporter.getMinorGcCount() - before;
        }

        /**
         * Allocate long-lived objects that survive Minor GC.
         * Call releaseRetained() afterwards to clean up.
         */
        public static void allocateLongLivedMb(int mb) {
            for (int i = 0; i < mb; i++) {
                HOLDER.add(new byte[1024 * 1024]);
            }
        }

        public static void releaseRetained() {
            HOLDER.clear();
        }

        public static int getRetainedMb() {
            return HOLDER.size();
        }
    }
}
