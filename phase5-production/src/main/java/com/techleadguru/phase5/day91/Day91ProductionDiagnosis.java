package com.techleadguru.phase5.day91;

import java.lang.management.*;
import java.util.*;

/**
 * DAY 91 — Production Diagnosis: jstack, top -H, jcmd
 *
 * WHEN PRODUCTION IS SICK (CPU high, latency high, threads stuck):
 *
 *   STEP 1 — Find the hot thread (top -H -p <pid>):
 *     top -H -p $(pgrep -f 'java')
 *     Shows per-thread CPU usage. Grab the PID of the hot thread (decimal).
 *     Convert to hex: printf '%x\n' <pid>  → e.g. 0x1a2b
 *
 *   STEP 2 — Get thread dump (jstack <pid>):
 *     jstack <pid> > /tmp/threaddump.txt
 *     Search for hex thread id from Step 1 (e.g. "nid=0x1a2b")
 *     Look at stack trace — what is this thread doing?
 *
 *   STEP 3 — jcmd for more detail:
 *     jcmd <pid> Thread.print               — thread dump (like jstack)
 *     jcmd <pid> GC.heap_info               — heap usage
 *     jcmd <pid> VM.flags                   — JVM flags
 *     jcmd <pid> GC.run                     — trigger GC
 *     jcmd <pid> VM.native_memory           — native memory (needs -XX:NativeMemoryTracking=summary)
 *     jcmd <pid> JFR.start name=prof duration=60s filename=/tmp/prof.jfr
 *     jcmd <pid> JFR.dump name=prof filename=/tmp/prof.jfr
 *
 *   STEP 4 — Heap analysis (if OOM / high memory):
 *     jmap -dump:format=b,file=/tmp/heap.hprof <pid>
 *     OR set -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/
 *     Analyze with Eclipse MAT or IntelliJ Profiler
 *
 *   COMMON PATTERNS:
 *     100% CPU + GC threads hot → GC storm (Old Gen full → heap dump)
 *     100% CPU + app threads hot → CPU-intensive work (flamegraph → Day96)
 *     Locked threads / BLOCKED → deadlock (deadlock section in jstack) → Day87
 *     WAITING threads (most) + latency high → pool exhaustion → Day85
 */
public class Day91ProductionDiagnosis {

    // =========================================================================
    // Live thread snapshot via ThreadMXBean
    // =========================================================================

    public static class ThreadDiagnostic {

        private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        public record ThreadSnapshot(long totalThreads, long peakThreads, long daemonThreads,
                                     long startedThreads, List<String> blockedThreadNames,
                                     long[] deadlockedIds) {}

        /**
         * Capture a live thread snapshot — useful in tests and health endpoints.
         */
        public ThreadSnapshot captureSnapshot() {
            long total   = threadMXBean.getThreadCount();
            long peak    = threadMXBean.getPeakThreadCount();
            long daemon  = threadMXBean.getDaemonThreadCount();
            long started = threadMXBean.getTotalStartedThreadCount();

            List<String> blocked = new ArrayList<>();
            for (ThreadInfo info : threadMXBean.dumpAllThreads(false, false)) {
                if (info.getThreadState() == Thread.State.BLOCKED) {
                    blocked.add(info.getThreadName());
                }
            }

            long[] deadlocked = threadMXBean.findDeadlockedThreads();
            return new ThreadSnapshot(total, peak, daemon, started, blocked,
                    deadlocked == null ? new long[0] : deadlocked);
        }

        /**
         * Returns thread stacks grouped by state.
         */
        public Map<Thread.State, List<String>> threadsByState() {
            Map<Thread.State, List<String>> map = new EnumMap<>(Thread.State.class);
            for (Thread.State s : Thread.State.values()) {
                map.put(s, new ArrayList<>());
            }
            for (ThreadInfo info : threadMXBean.dumpAllThreads(false, false)) {
                map.get(info.getThreadState()).add(info.getThreadName());
            }
            return map;
        }

        /**
         * Find the most CPU-intensive threads in the current JVM.
         * Only works if ThreadMXBean.isThreadCpuTimeSupported().
         */
        public List<CpuHotThread> topCpuThreads(int limit) {
            if (!threadMXBean.isThreadCpuTimeSupported()) {
                return List.of();
            }
            long[] ids = threadMXBean.getAllThreadIds();
            List<CpuHotThread> result = new ArrayList<>();
            ThreadInfo[] infos = threadMXBean.getThreadInfo(ids);
            for (int i = 0; i < ids.length; i++) {
                if (infos[i] == null) continue;
                long cpuNs = threadMXBean.getThreadCpuTime(ids[i]);
                result.add(new CpuHotThread(infos[i].getThreadName(), ids[i], cpuNs));
            }
            result.sort(Comparator.comparingLong(CpuHotThread::cpuNanos).reversed());
            return result.subList(0, Math.min(limit, result.size()));
        }

        public record CpuHotThread(String name, long id, long cpuNanos) {
            public String hexId() { return "0x" + Long.toHexString(id); }
            public double cpuMs() { return cpuNanos / 1_000_000.0; }
        }
    }

    // =========================================================================
    // Diagnosis guide as structured data
    // =========================================================================

    public static class DiagnosisPlaybook {

        public record Step(int order, String symptom, String tool, String command, String interpretation) {}

        public static List<Step> getSteps() {
            return List.of(
                    new Step(1,
                            "High CPU",
                            "top -H",
                            "top -H -p $(pgrep -f 'java')",
                            "Identify thread PID. Convert to hex for jstack lookup."),

                    new Step(2,
                            "Thread identification",
                            "jstack",
                            "jstack $(pgrep -f 'java') | grep -A 20 'nid=0x<HEX>'",
                            "Find the stack trace of the hot thread."),

                    new Step(3,
                            "Full thread dump",
                            "jcmd",
                            "jcmd $(pgrep -f 'java') Thread.print > /tmp/threads.txt",
                            "Look for BLOCKED threads, deadlock summary at bottom."),

                    new Step(4,
                            "GC pressure",
                            "jcmd",
                            "jcmd $(pgrep -f 'java') GC.heap_info",
                            "If Old Gen > 80% and climbing, prepare for OOM."),

                    new Step(5,
                            "JFR profiling",
                            "jcmd JFR",
                            "jcmd <pid> JFR.start name=p duration=60s filename=/tmp/p.jfr",
                            "Flamegraph-quality profiling with < 2% overhead."),

                    new Step(6,
                            "Heap dump",
                            "jmap",
                            "jmap -dump:live,format=b,file=/tmp/heap.hprof <pid>",
                            "Analyze in Eclipse MAT. Dominant retained tree = root cause.")
            );
        }

        public static String jstackDeadlockSection() {
            return """
                    Found one Java-level deadlock:
                    =============================
                    "Thread-1":
                      waiting to lock monitor 0x00007f... (object of type java.lang.Object),
                      which is held by "Thread-2"
                    "Thread-2":
                      waiting to lock monitor 0x00007f... (object of type java.lang.Object),
                      which is held by "Thread-1"
                    
                    → Signals a deadlock. Look at the lock hierarchy and apply Day87 fix.
                    """;
        }
    }
}
