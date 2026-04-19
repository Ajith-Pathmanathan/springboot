package com.techleadguru.phase4.day71;

import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 71 — Thread Dump: Find BLOCKED Threads Test
 *
 * Verifies:
 * 1. getAllThreads() returns a non-empty list (the test thread itself is there)
 * 2. getThreadStateHistogram() contains at least RUNNABLE (this test thread)
 * 3. findDeadlockedThreadIds() returns empty when no deadlock exists
 * 4. getTopCpuThreads() returns a list
 *
 * Note: No Spring context needed — ThreadDumpUtil is a static utility class.
 */
class Day71ThreadDumpTest {

    @Test
    void getAllThreads_returns_non_empty_list() {
        List<ThreadInfo> threads = Day71ThreadDump.ThreadDumpUtil.getAllThreads();
        assertThat(threads).isNotEmpty();
    }

    @Test
    void getAllThreads_includes_current_thread() {
        String currentThreadName = Thread.currentThread().getName();
        List<ThreadInfo> threads = Day71ThreadDump.ThreadDumpUtil.getAllThreads();
        boolean found = threads.stream()
                .anyMatch(t -> t.getThreadName().equals(currentThreadName));
        assertThat(found).as("Current thread should appear in dump").isTrue();
    }

    @Test
    void getThreadStateHistogram_contains_runnable() {
        Map<Thread.State, Long> histogram = Day71ThreadDump.ThreadDumpUtil.getThreadStateHistogram();
        assertThat(histogram).containsKey(Thread.State.RUNNABLE);
        assertThat(histogram.get(Thread.State.RUNNABLE)).isPositive();
    }

    @Test
    void findDeadlockedThreadIds_returns_empty_when_no_deadlock() {
        // Skip if a prior test (e.g., Day72) already left a JVM-level deadlock
        org.junit.jupiter.api.Assumptions.assumeTrue(
                ManagementFactory.getThreadMXBean().findDeadlockedThreads() == null,
                "Pre-existing JVM deadlock detected (likely from Day72); skipping");
        long[] ids = Day71ThreadDump.ThreadDumpUtil.findDeadlockedThreadIds();
        assertThat(ids).isEmpty();
    }

    @Test
    void getBlockedThreads_returns_list_when_no_blocking() {
        // No threads are BLOCKED in this simple test — should be empty
        List<ThreadInfo> blocked = Day71ThreadDump.ThreadDumpUtil.getBlockedThreads();
        assertThat(blocked).isNotNull(); // can be empty, but not null
    }

    @Test
    void format_produces_readable_output_for_current_thread() {
        ThreadInfo currentInfo = java.lang.management.ManagementFactory.getThreadMXBean()
                .getThreadInfo(Thread.currentThread().threadId(), 8);
        String formatted = Day71ThreadDump.ThreadDumpUtil.format(currentInfo);
        assertThat(formatted).contains(Thread.currentThread().getName());
        assertThat(formatted).containsAnyOf("RUNNABLE", "at ");
    }

    @Test
    void getTopCpuThreads_returns_list() {
        List<ThreadInfo> topThreads = Day71ThreadDump.ThreadDumpUtil.getTopCpuThreads(5);
        // May be empty on some JVMs if CPU time isn't tracked, but should not throw
        assertThat(topThreads).isNotNull();
    }
}
