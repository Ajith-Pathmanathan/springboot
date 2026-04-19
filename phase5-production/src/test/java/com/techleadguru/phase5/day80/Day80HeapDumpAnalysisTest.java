package com.techleadguru.phase5.day80;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class Day80HeapDumpAnalysisTest {

    @AfterEach
    void cleanUp() {
        Day80HeapDumpAnalysis.StaticFieldLeak.clear();
    }

    // ---- StaticFieldLeak ----

    @Test
    void staticFieldLeak_grows_with_each_add() {
        Day80HeapDumpAnalysis.StaticFieldLeak.addReport("r1", new byte[1024]);
        Day80HeapDumpAnalysis.StaticFieldLeak.addReport("r2", new byte[1024]);
        assertThat(Day80HeapDumpAnalysis.StaticFieldLeak.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void staticFieldLeak_clear_empties_cache() {
        Day80HeapDumpAnalysis.StaticFieldLeak.addReport("x", new byte[512]);
        Day80HeapDumpAnalysis.StaticFieldLeak.clear();
        assertThat(Day80HeapDumpAnalysis.StaticFieldLeak.size()).isEqualTo(0);
    }

    @Test
    void staticFieldLeak_get_returns_stored_bytes() {
        byte[] data = {1, 2, 3};
        Day80HeapDumpAnalysis.StaticFieldLeak.addReport("k", data);
        assertThat(Day80HeapDumpAnalysis.StaticFieldLeak.get("k")).isEqualTo(data);
    }

    // ---- OqlSimulator ----

    @Test
    void oqlSimulator_findLargeObjects_filters_correctly() {
        var heap = List.of(
                new Day80HeapDumpAnalysis.OqlSimulator.LeakedObject("a", 500),
                new Day80HeapDumpAnalysis.OqlSimulator.LeakedObject("b", 1_500),
                new Day80HeapDumpAnalysis.OqlSimulator.LeakedObject("c", 2_000)
        );
        var large = Day80HeapDumpAnalysis.OqlSimulator.findLargeObjects(heap, 1_000);
        assertThat(large).hasSize(2);
        assertThat(large).extracting(Day80HeapDumpAnalysis.OqlSimulator.LeakedObject::id)
                .containsExactlyInAnyOrder("b", "c");
    }

    @Test
    void oqlSimulator_totalRetained_sums_correctly() {
        var objects = List.of(
                new Day80HeapDumpAnalysis.OqlSimulator.LeakedObject("x", 100),
                new Day80HeapDumpAnalysis.OqlSimulator.LeakedObject("y", 200)
        );
        assertThat(Day80HeapDumpAnalysis.OqlSimulator.totalRetained(objects)).isEqualTo(300);
    }

    // ---- RetainedSizeDemo ----

    @Test
    void retainedSizeDemo_buildChain_creates_chain() {
        Day80HeapDumpAnalysis.RetainedSizeDemo.Node head =
                Day80HeapDumpAnalysis.RetainedSizeDemo.buildChain(5);
        assertThat(head).isNotNull();
        assertThat(head.data()).isNotNull();
    }

    @Test
    void retainedSizeDemo_estimateRetainedKb_grows_with_length() {
        long small = Day80HeapDumpAnalysis.RetainedSizeDemo.estimateRetainedKb(5);
        long large = Day80HeapDumpAnalysis.RetainedSizeDemo.estimateRetainedKb(10);
        assertThat(large).isGreaterThan(small);
    }

    // ---- HistogramHelper ----

    @Test
    void histogramHelper_sortByShallowHeapDesc_sorts_correctly() {
        // HistogramRow fields: className, instanceCount, shallowBytes
        var rows = List.of(
                new Day80HeapDumpAnalysis.HistogramHelper.HistogramRow("A", 10, 100),
                new Day80HeapDumpAnalysis.HistogramHelper.HistogramRow("B", 5,  500),
                new Day80HeapDumpAnalysis.HistogramHelper.HistogramRow("C", 20, 50)
        );
        var sorted = Day80HeapDumpAnalysis.HistogramHelper.sortByShallowHeapDesc(rows);
        assertThat(sorted.get(0).shallowBytes()).isEqualTo(500);
    }

    @Test
    void histogramHelper_findSuspects_filters_by_count() {
        var rows = List.of(
                new Day80HeapDumpAnalysis.HistogramHelper.HistogramRow("X", 100, 1000),
                new Day80HeapDumpAnalysis.HistogramHelper.HistogramRow("Y", 5,   500),
                new Day80HeapDumpAnalysis.HistogramHelper.HistogramRow("Z", 50,  200)
        );
        var suspects = Day80HeapDumpAnalysis.HistogramHelper.findSuspects(rows, 10);
        assertThat(suspects).extracting(Day80HeapDumpAnalysis.HistogramHelper.HistogramRow::className)
                .contains("X", "Z")
                .doesNotContain("Y");
    }
}
