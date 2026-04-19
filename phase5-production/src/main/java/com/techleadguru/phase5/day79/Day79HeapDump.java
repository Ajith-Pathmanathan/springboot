package com.techleadguru.phase5.day79;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;

/**
 * DAY 79 — Heap Dump: -XX:+HeapDumpOnOutOfMemoryError
 *
 * HOW TO GET A HEAP DUMP:
 *
 *   1. On OOM (automatic):
 *      Add JVM flag: -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/app.hprof
 *      Spring Boot's pom.xml plugin section already sets this!
 *
 *   2. On demand with jmap:
 *      jmap -dump:format=b,file=/tmp/on-demand.hprof <PID>
 *
 *   3. Via Actuator (Spring Boot):
 *      GET /actuator/heapdump   → downloads live heap dump as .hprof
 *      Enabled by: management.endpoint.heapdump.enabled=true
 *
 *   4. Via jcmd:
 *      jcmd <PID> GC.heap_dump /tmp/dump.hprof
 *
 * FINDING THE PID:
 *   jps -l           → list all JVM processes
 *   pidof java       → Linux shortcut
 *
 * WHY HEAP DUMPS:
 *   Thread dumps show WHAT threads are doing (live view).
 *   Heap dumps show WHAT OBJECTS are on the heap (snapshot).
 *   Use Eclipse MAT or JProfiler to analyze: find the dominator tree,
 *   identify the object keeping the most memory alive.
 *
 * HEAP DUMP WORKFLOW:
 *   1. Get OOM (or trigger on-demand dump)
 *   2. Open in Eclipse MAT → "Leak Suspects" report
 *   3. Find dominator: the object retaining thousands of MB
 *   4. Trace GC roots → find the field that holds the reference
 *   5. Fix the leak: clear the collection, remove listener, etc.
 */
@Slf4j
public class Day79HeapDump {

    // =========================================================================
    // Heap dump trigger utility
    // =========================================================================

    /**
     * Checks if the JVM was started with -XX:+HeapDumpOnOutOfMemoryError.
     * Uses HotSpot MBean if available (works on standard JDK).
     */
    public static boolean isHeapDumpOnOomEnabled() {
        try {
            var server = ManagementFactory.getPlatformMBeanServer();
            var name = new javax.management.ObjectName("com.sun.management:type=HotSpotDiagnostic");
            var vmOption = (com.sun.management.VMOption)
                    server.invoke(name, "getVMOption",
                            new Object[]{"HeapDumpOnOutOfMemoryError"},
                            new String[]{String.class.getName()});
            return Boolean.parseBoolean(vmOption.getValue());
        } catch (Exception e) {
            log.warn("[Day79] Could not check HeapDumpOnOutOfMemoryError flag: {}", e.getMessage());
            return false; // not HotSpot or MBean not available
        }
    }

    /**
     * Requests a heap dump to the given path via HotSpot Diagnostic MBean.
     * Equivalent to: jcmd <PID> GC.heap_dump <path>
     * Set live=true to dump only live objects (smaller file, better for analysis).
     */
    public static void dumpHeap(String outputPath, boolean liveOnly) throws Exception {
        var server = ManagementFactory.getPlatformMBeanServer();
        var name = new javax.management.ObjectName("com.sun.management:type=HotSpotDiagnostic");
        server.invoke(name, "dumpHeap",
                new Object[]{outputPath, liveOnly},
                new String[]{String.class.getName(), boolean.class.getName()});
        log.info("[Day79] Heap dump written to: {} (liveOnly={})", outputPath, liveOnly);
    }

    // =========================================================================
    // Actuator heapdump is automatic: GET /actuator/heapdump
    // Below is documentation about how to analyze the dump.
    // =========================================================================

    /**
     * Step-by-step MAT analysis guide (documentation only — no runtime code needed).
     *
     *   1. Open the .hprof file in Eclipse MAT (Memory Analyzer Tool).
     *   2. Run "Leak Suspects" report → MAT shows top suspects automatically.
     *   3. Open "Dominator Tree" → objects retaining the most heap.
     *   4. Right-click the top object → "Path to GC Roots" → "Exclude all phantom/weak/soft refs"
     *      This shows the chain of references keeping the object alive.
     *   5. Click "Object Details" → see the fields, the collection contents.
     *   6. Fix: remove the field reference, properly close the resource, use WeakReference.
     *
     * COMMON FINDINGS:
     *   - HashMap / ArrayList held in a static field that grows without bound
     *   - Listener registered in EventBus, never de-registered
     *   - ThreadLocal not removed after request → accumulates on pooled threads
     *   - Session / HTTP state held past user logout
     */
    public static String matAnalysisGuide() {
        return """
            MAT Analysis Steps:
            1. File → Open Heap Dump (.hprof)
            2. Window → Heap Dump Details → Leak Suspects Report
            3. Inspect dominator tree
            4. Path to GC roots (exclude weak/soft refs)
            5. Identify field holding reference
            6. Fix the leak
            """;
    }

    // =========================================================================
    // GC request utility
    // =========================================================================

    /**
     * Forces a GC cycle so that after clearing a leak simulation,
     * we can verify that heap usage actually drops.
     * NOTE: System.gc() is a hint, not a guarantee.
     */
    public static void forceGc() {
        log.info("[Day79] Requesting GC...");
        System.gc();
        System.runFinalization();
        log.info("[Day79] GC request sent");
    }
}
