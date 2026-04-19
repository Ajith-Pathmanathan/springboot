package com.techleadguru.phase5.day95;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day95OffHeapTest {

    @Test
    void directBufferDemo_isAllocated_false_before_allocate() {
        var demo = new Day95OffHeap.DirectBufferDemo(1024);
        assertThat(demo.isAllocated()).isFalse();
    }

    @Test
    void directBufferDemo_isAllocated_true_after_allocate() {
        var demo = new Day95OffHeap.DirectBufferDemo(1024);
        demo.allocate();
        assertThat(demo.isAllocated()).isTrue();
        demo.release();
    }

    @Test
    void directBufferDemo_readWrite_succeeds_after_allocate() {
        var demo = new Day95OffHeap.DirectBufferDemo(1024);
        demo.allocate();
        assertThat(demo.readWrite()).isTrue();
        demo.release();
    }

    @Test
    void directBufferDemo_readWrite_throws_without_allocate() {
        var demo = new Day95OffHeap.DirectBufferDemo(1024);
        assertThatThrownBy(demo::readWrite)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void directBufferDemo_release_clears_allocation() {
        var demo = new Day95OffHeap.DirectBufferDemo(1024);
        demo.allocate();
        demo.release();
        assertThat(demo.isAllocated()).isFalse();
    }

    @Test
    void nativeMemoryStats_nonHeapCommittedMb_is_positive() {
        assertThat(Day95OffHeap.NativeMemoryStats.nonHeapCommittedMb()).isPositive();
    }

    @Test
    void nativeMemoryStats_metaspaceUsage_is_present() {
        var usage = Day95OffHeap.NativeMemoryStats.metaspaceUsage();
        assertThat(usage).isPresent();
        assertThat(usage.get().getUsed()).isPositive();
    }

    @Test
    void nativeMemoryStats_summarize_returns_valid_values() {
        var summary = Day95OffHeap.NativeMemoryStats.summarize();
        assertThat(summary.heapUsedMb()).isPositive();
        assertThat(summary.nonHeapCommittedMb()).isPositive();
        assertThat(summary.metaspaceUsedMb()).isGreaterThanOrEqualTo(0);
    }
}
