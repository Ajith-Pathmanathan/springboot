package com.techleadguru.phase5.day92;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day92GcBasicsTest {

    @Test
    void gcStatsReporter_all_beans_not_null() {
        var reporter = new Day92GcBasics.GcStatsReporter();
        var beans = reporter.getAllBeans();
        assertThat(beans).isNotNull();
        // May be empty in some JVM configurations (trivial sandbox), just verify no exception
    }

    @Test
    void gcStatsReporter_counts_are_non_negative() {
        var reporter = new Day92GcBasics.GcStatsReporter();
        assertThat(reporter.getMinorGcCount()).isGreaterThanOrEqualTo(0);
        assertThat(reporter.getMajorGcCount()).isGreaterThanOrEqualTo(0);
        assertThat(reporter.getMinorGcTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(reporter.getMajorGcTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void heapStatsReporter_usagePercent_between_0_and_100() {
        var reporter = new Day92GcBasics.HeapStatsReporter();
        var stats = reporter.getCurrentHeapStats();
        assertThat(stats.usagePercent()).isBetween(0.0, 100.0);
        assertThat(stats.usedBytes()).isPositive();
        assertThat(stats.committedBytes()).isPositive();
    }

    @Test
    void heapStatsReporter_used_bytes_lte_max() {
        var reporter = new Day92GcBasics.HeapStatsReporter();
        var stats = reporter.getCurrentHeapStats();
        if (stats.maxBytes() > 0) {
            assertThat(stats.usedBytes()).isLessThanOrEqualTo(stats.maxBytes());
        }
    }

    @Test
    void gcPressureDemo_allocateShortLived_does_not_throw() {
        assertThatCode(() -> Day92GcBasics.GcPressureDemo.allocateShortLivedMb(2))
                .doesNotThrowAnyException();
    }

    @Test
    void gcPressureDemo_allocateLongLived_increases_retained_count() {
        Day92GcBasics.GcPressureDemo.releaseRetained(); // ensure clean state
        Day92GcBasics.GcPressureDemo.allocateLongLivedMb(2);
        assertThat(Day92GcBasics.GcPressureDemo.getRetainedMb()).isGreaterThanOrEqualTo(2);
        Day92GcBasics.GcPressureDemo.releaseRetained();
    }

    @Test
    void gcPressureDemo_releaseRetained_clears_all() {
        Day92GcBasics.GcPressureDemo.allocateLongLivedMb(1);
        Day92GcBasics.GcPressureDemo.releaseRetained();
        assertThat(Day92GcBasics.GcPressureDemo.getRetainedMb()).isZero();
    }

    @Test
    void heapStatsReporter_oldGenStats_not_null() {
        var reporter = new Day92GcBasics.HeapStatsReporter();
        // Optional — may be empty in G1GC test environment; just verify no exception
        assertThatCode(() -> reporter.getOldGenStats()).doesNotThrowAnyException();
    }
}
