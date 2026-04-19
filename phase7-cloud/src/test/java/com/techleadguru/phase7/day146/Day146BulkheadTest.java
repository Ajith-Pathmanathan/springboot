package com.techleadguru.phase7.day146;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day146BulkheadTest {

    @Test
    void testAcquireWithinLimit() throws InterruptedException {
        Day146Bulkhead.BulkheadConfig config =
                new Day146Bulkhead.BulkheadConfig(3, 0);
        Day146Bulkhead.BulkheadSimulator bulkhead =
                new Day146Bulkhead.BulkheadSimulator(config);
        assertTrue(bulkhead.acquire());
        assertTrue(bulkhead.acquire());
        assertTrue(bulkhead.acquire());
        assertEquals(0, bulkhead.availableSlots());
    }

    @Test
    void testRejectWhenFull() throws InterruptedException {
        Day146Bulkhead.BulkheadConfig config =
                new Day146Bulkhead.BulkheadConfig(2, 0);
        Day146Bulkhead.BulkheadSimulator bulkhead =
                new Day146Bulkhead.BulkheadSimulator(config);
        bulkhead.acquire();
        bulkhead.acquire();
        assertFalse(bulkhead.acquire());
        assertEquals(1, bulkhead.rejectedCount());
    }

    @Test
    void testReleaseRestoresSlot() throws InterruptedException {
        Day146Bulkhead.BulkheadConfig config =
                new Day146Bulkhead.BulkheadConfig(1, 0);
        Day146Bulkhead.BulkheadSimulator bulkhead =
                new Day146Bulkhead.BulkheadSimulator(config);
        bulkhead.acquire();
        assertEquals(0, bulkhead.availableSlots());
        bulkhead.release();
        assertEquals(1, bulkhead.availableSlots());
    }

    @Test
    void testMaxConcurrent() {
        Day146Bulkhead.BulkheadConfig config =
                new Day146Bulkhead.BulkheadConfig(5, 0);
        Day146Bulkhead.BulkheadSimulator bulkhead =
                new Day146Bulkhead.BulkheadSimulator(config);
        assertEquals(5, bulkhead.maxConcurrent());
    }

    @Test
    void testBulkheadVsThreadPoolComparison() {
        List<Day146Bulkhead.BulkheadComparison> comparisons =
                Day146Bulkhead.bulkheadVsThreadPool();
        assertEquals(5, comparisons.size());
        assertTrue(comparisons.stream()
                .anyMatch(c -> c.aspect().equals("Resource type")));
    }
}
