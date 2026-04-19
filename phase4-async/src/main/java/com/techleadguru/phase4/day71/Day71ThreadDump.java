package com.techleadguru.phase4.day71;

import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * DAY 71 — Thread Dump: Find BLOCKED Threads
 *
 * THREAD STATES:
 *   RUNNABLE   → actively executing on CPU or ready to execute
 *   BLOCKED    → waiting to acquire a monitor lock (synchronized block held by another thread)
 *   WAITING    → waiting indefinitely (Object.wait(), Thread.join(), LockSupport.park())
 *   TIMED_WAITING → waiting with timeout (Thread.sleep(), wait(timeout), parkNanos())
 *   NEW        → created but not yet started
 *   TERMINATED → execution completed
 *
 * HOW TO CAPTURE A THREAD DUMP:
 *   jstack <pid>                → command line (process must be running)
 *   kill -3 <pid>               → Linux: prints dump to stdout
 *   jcmd <pid> Thread.print     → modern alternative to jstack
 *   Actuator: GET /actuator/threaddump → JSON thread dump (with spring-boot-starter-actuator)
 *   ThreadMXBean (this demo)    → programmatic access from within the JVM
 *
 * READING A THREAD DUMP — look for:
 *   1. BLOCKED threads + which lock they're waiting for ("waiting to lock <0x...>")
 *   2. "Found one Java-level deadlock" section → deadlock detected!
 *   3. Many threads WAITING on the same object → potential bottleneck
 *   4. High thread count with empty queues → thread pool misconfig
 *
 * COMMON PRODUCTION THREAD DUMP PATTERNS:
 *
 *   Pattern 1: All Tomcat threads BLOCKED
 *     → DB connection pool exhausted (everyone waiting for getConnection())
 *     → Fix: increase pool size or fix slow queries
 *
 *   Pattern 2: Thread at TIMED_WAITING in sun.misc.Unsafe.park
 *     → Normal: idle pool worker waiting for tasks
 *
 *   Pattern 3: Thread BLOCKED in synchronized method of a custom cache
 *     → Replace synchronized HashMap with ConcurrentHashMap (Day 75)
 *
 *   Pattern 4: "Found deadlock" section
 *     → Thread A holds lock1, waiting for lock2
 *     → Thread B holds lock2, waiting for lock1
 *     → Fix: consistent lock ordering (Day 72)
 */
@Slf4j
public class Day71ThreadDump {

    // =========================================================================
    // Thread dump utility
    // =========================================================================

    public static class ThreadDumpUtil {

        private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        /**
         * Returns all thread info, optionally including locked monitors and synchronizers.
         */
        public static List<ThreadInfo> getAllThreads() {
            return Arrays.asList(threadMXBean.dumpAllThreads(true, true));
        }

        /**
         * Returns only threads in BLOCKED state.
         * Useful for detecting contention hot spots.
         */
        public static List<ThreadInfo> getBlockedThreads() {
            return Arrays.stream(threadMXBean.dumpAllThreads(true, true))
                    .filter(t -> t.getThreadState() == Thread.State.BLOCKED)
                    .toList();
        }

        /**
         * Detects deadlocked threads — threads circularly waiting for each other's locks.
         * Returns their IDs, or empty array if no deadlock.
         */
        public static long[] findDeadlockedThreadIds() {
            long[] ids = threadMXBean.findDeadlockedThreads();
            return ids != null ? ids : new long[0];
        }

        /**
         * Returns a human-readable summary of thread states.
         */
        public static Map<Thread.State, Long> getThreadStateHistogram() {
            return Arrays.stream(threadMXBean.dumpAllThreads(false, false))
                    .collect(java.util.stream.Collectors.groupingBy(
                            ThreadInfo::getThreadState,
                            java.util.stream.Collectors.counting()
                    ));
        }

        /**
         * Returns top N threads by CPU time (useful for finding CPU hogs).
         */
        public static List<ThreadInfo> getTopCpuThreads(int topN) {
            if (!threadMXBean.isThreadCpuTimeEnabled()) {
                threadMXBean.setThreadCpuTimeEnabled(true);
            }
            return Arrays.stream(threadMXBean.getAllThreadIds())
                    .mapToObj(id -> {
                        long cpu = threadMXBean.getThreadCpuTime(id);
                        ThreadInfo info = threadMXBean.getThreadInfo(id);
                        return info != null ? Map.entry(info, cpu) : null;
                    })
                    .filter(e -> e != null && e.getValue() > 0)
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .limit(topN)
                    .map(Map.Entry::getKey)
                    .toList();
        }

        /**
         * Formats a ThreadInfo into a readable string like jstack output.
         */
        public static String format(ThreadInfo t) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("\"%s\" #%d %s%n",
                    t.getThreadName(), t.getThreadId(), t.getThreadState()));
            if (t.getLockName() != null) {
                sb.append(String.format("   waiting to lock: %s%n", t.getLockName()));
            }
            if (t.getLockOwnerName() != null) {
                sb.append(String.format("   held by: \"%s\" (#%d)%n",
                        t.getLockOwnerName(), t.getLockOwnerId()));
            }
            StackTraceElement[] stack = t.getStackTrace();
            for (int i = 0; i < Math.min(stack.length, 8); i++) {
                sb.append("   at ").append(stack[i]).append("\n");
            }
            return sb.toString();
        }
    }
}
