package com.techleadguru.phase5.day78;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day78JvmMemoryModelTest {

    private Day78JvmMemoryModel.HeapLeakSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new Day78JvmMemoryModel.HeapLeakSimulator();
    }

    @AfterEach
    void tearDown() {
        simulator.clear();
    }

    @Test
    void snapshot_returns_positive_heap_usage() {
        Day78JvmMemoryModel.MemoryReporter.MemorySnapshot snap = Day78JvmMemoryModel.MemoryReporter.snapshot();
        assertThat(snap.heapUsed()).isPositive();
        assertThat(snap.heapMax()).isPositive();
        assertThat(snap.heapUsed()).isLessThanOrEqualTo(snap.heapMax());
    }

    @Test
    void snapshot_returns_non_negative_nonheap() {
        Day78JvmMemoryModel.MemoryReporter.MemorySnapshot snap = Day78JvmMemoryModel.MemoryReporter.snapshot();
        assertThat(snap.nonHeapUsed()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void snapshot_heap_mb_is_positive() {
        Day78JvmMemoryModel.MemoryReporter.MemorySnapshot snap = Day78JvmMemoryModel.MemoryReporter.snapshot();
        assertThat(snap.heapUsedMb()).isPositive();
        assertThat(snap.heapMaxMb()).isPositive();
    }

    @Test
    void allocateChunkMb_increases_held_bytes() {
        long before = simulator.allocateChunkMb(1);
        long after  = simulator.allocateChunkMb(1);
        assertThat(after).isGreaterThan(before);
    }

    @Test
    void clear_releases_all_held_chunks() {
        simulator.allocateChunkMb(2);
        simulator.clear();
        assertThat(simulator.getChunkCount()).isEqualTo(0);
    }

    @Test
    void stackDemo_recurse_completes_within_limit() {
        int reached = Day78JvmMemoryModel.StackDemo.recurse(0, 100);
        assertThat(reached).isEqualTo(100);
    }
}
