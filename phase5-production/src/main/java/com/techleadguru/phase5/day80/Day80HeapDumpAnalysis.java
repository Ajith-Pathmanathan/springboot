package com.techleadguru.phase5.day80;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * DAY 80 — Heap Dump Analysis: Eclipse MAT Dominator Tree
 *
 * DOMINATOR TREE — key concept in MAT:
 *
 *   A node B is a DOMINATOR of node A if every path from the GC roots to A
 *   passes through B. Cutting B releases all of A's retained heap.
 *
 *   Eclipse MAT builds this tree automatically. The top entry is the biggest
 *   "garbage collector" — fixing that one object frees the most memory.
 *
 * RETAINED SIZE vs SHALLOW SIZE:
 *   Shallow size = memory of the object itself (just its fields).
 *   Retained size = shallow size + everything reachable ONLY through this object.
 *   → Retained size is what you free by removing the reference.
 *
 * WHAT TO LOOK FOR IN MAT:
 *   1. Dominator Tree → sort by Retained Heap desc → top entry is the leak root
 *   2. OQL (Object Query Language) — SQL-like queries over the heap:
 *      SELECT * FROM java.util.ArrayList WHERE size > 10000
 *      SELECT * FROM java.lang.String s WHERE s.value.length > 1000
 *   3. Histogram → count instances per class → unusually high count = leak
 *   4. "Path to GC Roots" → trace who keeps it alive
 *
 * REAL SCENARIO — this class models a common leak:
 *   A cache Map<String,Report> held in a static field.
 *   Reports are added but never removed → unbounded growth → OOM.
 */
@Slf4j
public class Day80HeapDumpAnalysis {

    // =========================================================================
    // Static field leak — the dominator pattern
    // =========================================================================

    /**
     * LEAKED PATTERN: static Map grows forever when `addReport()` is called.
     * In MAT: this HashMap would appear as the top dominator.
     * Fix: use a bounded cache (Caffeine), or evict old entries.
     */
    public static class StaticFieldLeak {

        // BUG: static field — lives for the entire JVM lifetime
        private static final Map<String, byte[]> reportCache = new HashMap<>();

        public static void addReport(String id, byte[] data) {
            reportCache.put(id, data);
            if (reportCache.size() % 100 == 0) {
                log.warn("[Day80] LEAK: reportCache has {} entries ({} KB approx)",
                        reportCache.size(), reportCache.size() * (data.length / 1024));
            }
        }

        public static int size()              { return reportCache.size(); }
        public static void clear()            { reportCache.clear(); }
        public static byte[] get(String id)   { return reportCache.get(id); }
    }

    // =========================================================================
    // OQL query simulation — teaches patterns without actual MAT
    // =========================================================================

    /**
     * Simulates what Eclipse MAT OQL would find.
     * OQL: SELECT * FROM LeakedObject WHERE retainedSize > threshold
     */
    public static class OqlSimulator {

        record LeakedObject(String id, long retainedBytes) {}

        public static List<LeakedObject> findLargeObjects(List<LeakedObject> heap, long thresholdBytes) {
            return heap.stream()
                    .filter(o -> o.retainedBytes() > thresholdBytes)
                    .sorted(Comparator.comparingLong(LeakedObject::retainedBytes).reversed())
                    .toList();
        }

        public static long totalRetained(List<LeakedObject> objects) {
            return objects.stream().mapToLong(LeakedObject::retainedBytes).sum();
        }
    }

    // =========================================================================
    // Retained vs shallow size demonstration
    // =========================================================================

    public static class RetainedSizeDemo {

        record Node(String id, byte[] data, Node child) {
            // shallow size of Node ≈ reference sizes (small)
            // retained size = Node + data[] + child's chain
        }

        /**
         * Builds a chain of Nodes where each holds 1KB of data.
         * The head's retained size = N * 1KB.
         * The tail's retained size = just its own 1KB.
         */
        public static Node buildChain(int length) {
            Node tail = null;
            for (int i = 0; i < length; i++) {
                tail = new Node("node-" + i, new byte[1024], tail);
            }
            return tail; // head
        }

        public static long estimateRetainedKb(int chainLength) {
            return (long) chainLength;  // each node = 1KB data
        }
    }

    // =========================================================================
    // Histogram analysis helper
    // =========================================================================

    /**
     * In MAT histogram: each row = class name, instance count, shallow heap, retained heap.
     * Sort by "Shallow Heap" desc to find which class has unusual count.
     */
    public static class HistogramHelper {

        record HistogramRow(String className, long instanceCount, long shallowBytes) {}

        public static List<HistogramRow> sortByShallowHeapDesc(List<HistogramRow> rows) {
            return rows.stream()
                    .sorted(Comparator.comparingLong(HistogramRow::shallowBytes).reversed())
                    .toList();
        }

        /** Flags classes with unusually high instance counts. */
        public static List<HistogramRow> findSuspects(List<HistogramRow> rows, long countThreshold) {
            return rows.stream()
                    .filter(r -> r.instanceCount() > countThreshold)
                    .toList();
        }
    }
}
