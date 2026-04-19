package com.techleadguru.phase5.day93;

import java.util.*;

/**
 * DAY 93 — GC Algorithms: G1GC vs ZGC vs Shenandoah
 *
 * OVERVIEW:
 *   GC Algorithm     Pause Model    Target Pause    Heap Range     Java Version
 *   ─────────────── ─────────────  ─────────────  ─────────────  ─────────────
 *   Serial           Stop-The-World  100ms+        small heaps    Java 1.0+
 *   Parallel (PSYoung) STW           50-200ms      throughput     Java 1.4+
 *   G1GC             Mostly concurrent ≤ 200ms     4GB-100GB      Java 9+ (default)
 *   ZGC              Concurrent     < 1ms          8MB-16TB       Java 15+ production
 *   Shenandoah       Concurrent     < 10ms         medium-large   OpenJDK only
 *
 * G1GC (Garbage-First):
 *   - Heap divided into equal-sized regions (1-32 MB)
 *   - Collects regions with most garbage first (hence "Garbage First")
 *   - Young: concurrent marking → STW evacuation pause
 *   - Mixed GC: collects Young + some Old regions
 *   - Full GC: single-threaded fallback (should be rare)
 *   - Best for: general-purpose, heaps 4–32 GB, pause target ~200ms
 *
 * ZGC (Z Garbage Collector):
 *   - Fully concurrent: marking, relocation, reference processing — all concurrent
 *   - Uses colored pointers (load barriers) for concurrent object moves
 *   - Maximum pause: ~1 ms regardless of heap size
 *   - Best for: latency-sensitive (trading systems, payment APIs), heaps > 32 GB
 *   - Trade-off: higher CPU overhead (~15%), higher memory overhead
 *
 * SHENANDOAH:
 *   - Concurrent evacuation (unique to Shenandoah)
 *   - Lower latency than G1 but not as low as ZGC
 *   - Only in OpenJDK (not Oracle JDK)
 *   - Best for: interactive applications, medium heaps
 *
 * VIRTUAL THREADS (Java 21) vs GC choice:
 *   - Virtual threads reduce thread count → less survivor pressure
 *   - But still benefit from ZGC if latency critical
 */
public class Day93GcAlgorithms {

    // =========================================================================
    // JVM flag recommendations per algorithm
    // =========================================================================

    public static class GcConfig {

        /**
         * G1GC flags for a production Spring Boot service (heap 4–16 GB).
         */
        public static String g1gcFlags() {
            return """
                    # G1GC — general purpose, default Java 9+
                    -XX:+UseG1GC
                    -Xms4g -Xmx8g
                    -XX:NewRatio=2
                    -XX:MaxGCPauseMillis=200
                    -XX:G1HeapRegionSize=16m
                    -XX:G1NewSizePercent=20
                    -XX:G1MaxNewSizePercent=40
                    -XX:G1MixedGCLiveThresholdPercent=85
                    -XX:G1ReservePercent=10
                    -Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=5,filesize=20m
                    """;
        }

        /**
         * ZGC flags for a latency-sensitive service.
         */
        public static String zgcFlags() {
            return """
                    # ZGC — ultra-low latency, Java 15+
                    -XX:+UseZGC
                    -Xms8g -Xmx16g
                    -XX:ZCollectionInterval=5          # max seconds between GC cycles
                    -XX:ZAllocationSpikeTolerance=2    # allow 2x allocation spikes
                    -XX:ZUncommitDelay=300             # wait 5 min before releasing memory to OS
                    -Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=5,filesize=20m
                    """;
        }

        /**
         * Shenandoah flags (OpenJDK only).
         */
        public static String shenandoahFlags() {
            return """
                    # Shenandoah — concurrent evacuation, OpenJDK only
                    -XX:+UseShenandoahGC
                    -Xms4g -Xmx8g
                    -XX:ShenandoahGCMode=adaptive       # adaptive, static, compact, aggressive
                    -XX:ShenandoahAllocationThreshold=10
                    -Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=5,filesize=20m
                    """;
        }

        /**
         * Active current JVM GC name (best effort).
         */
        public static String detectCurrentGc() {
            for (java.lang.management.GarbageCollectorMXBean b :
                    java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()) {
                String name = b.getName();
                if (name.contains("ZGC"))               return "ZGC";
                if (name.contains("Shenandoah"))        return "Shenandoah";
                if (name.contains("G1"))                return "G1GC";
                if (name.contains("PS"))                return "Parallel";
                if (name.contains("Copy"))              return "Serial/ParNew";
            }
            return "Unknown";
        }
    }

    // =========================================================================
    // Workload-based GC selector
    // =========================================================================

    public enum GcRecommendation { G1GC, ZGC, SHENANDOAH, PARALLEL }

    public record WorkloadProfile(long pauseTargetMs, long heapSizeMb,
                                  boolean isLatencySensitive, boolean isOpenJdk) {}

    public static GcRecommendation selectGcForWorkload(WorkloadProfile profile) {
        if (profile.pauseTargetMs() < 5) {
            // Sub-millisecond targets → ZGC
            return GcRecommendation.ZGC;
        }
        if (profile.pauseTargetMs() < 50 && profile.isOpenJdk()) {
            return GcRecommendation.SHENANDOAH;
        }
        if (profile.heapSizeMb() > 32_000 || profile.isLatencySensitive()) {
            return GcRecommendation.ZGC;
        }
        if (!profile.isLatencySensitive() && profile.heapSizeMb() < 4_000) {
            return GcRecommendation.PARALLEL; // throughput for batch jobs
        }
        return GcRecommendation.G1GC; // sensible default
    }

    // =========================================================================
    // G1GC region simulation (educational)
    // =========================================================================

    public static class G1RegionSimulator {

        public enum RegionType { FREE, EDEN, SURVIVOR, OLD, HUMONGOUS }

        public record Region(int id, RegionType type, int usedPercent) {}

        /**
         * Simulate a simplified G1 heap layout for visualization.
         */
        public static List<Region> simulateLayout(int totalRegions) {
            List<Region> regions = new ArrayList<>();
            for (int i = 0; i < totalRegions; i++) {
                RegionType type;
                int used;
                if (i < totalRegions * 0.15)      { type = RegionType.EDEN;       used = 50 + (i * 3) % 49; }
                else if (i < totalRegions * 0.20)  { type = RegionType.SURVIVOR;   used = 30 + (i * 7) % 40; }
                else if (i < totalRegions * 0.70)  { type = RegionType.OLD;        used = 20 + (i * 5) % 70; }
                else if (i < totalRegions * 0.72)  { type = RegionType.HUMONGOUS;  used = 100; }
                else                               { type = RegionType.FREE;        used = 0; }
                regions.add(new Region(i, type, used));
            }
            return regions;
        }

        /**
         * G1 collection set: regions sorted by garbage ratio (most garbage first).
         */
        public static List<Region> buildCollectionSet(List<Region> regions, int maxRegions) {
            return regions.stream()
                    .filter(r -> r.type() == RegionType.OLD || r.type() == RegionType.EDEN)
                    .sorted(Comparator.comparingInt(r -> -(100 - r.usedPercent()))) // most garbage = lowest used
                    .limit(maxRegions)
                    .toList();
        }
    }
}
