package com.techleadguru.phase7.day142;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day142RefreshScopeTest {

    @Test
    void testRefreshableConfigHoldsValue() {
        Day142RefreshScope.RefreshableConfig cfg =
                new Day142RefreshScope.RefreshableConfig("initial");
        assertEquals("initial", cfg.getValue());
    }

    @Test
    void testRefreshableConfigUpdatesOnRefresh() {
        Day142RefreshScope.RefreshableConfig cfg =
                new Day142RefreshScope.RefreshableConfig("v1");
        cfg.refresh("v2");
        assertEquals("v2", cfg.getValue());
        assertNotNull(cfg.getLastRefreshed());
    }

    @Test
    void testSimulatorRegisterAndGet() {
        Day142RefreshScope.RefreshScopeSimulator simulator =
                new Day142RefreshScope.RefreshScopeSimulator();
        Day142RefreshScope.RefreshableConfig cfg =
                new Day142RefreshScope.RefreshableConfig("hello");
        simulator.register("myConfig", cfg);
        assertNotNull(simulator.get("myConfig"));
        assertEquals("hello", simulator.get("myConfig").getValue());
    }

    @Test
    void testSimulatorRefreshUpdatesValue() {
        Day142RefreshScope.RefreshScopeSimulator simulator =
                new Day142RefreshScope.RefreshScopeSimulator();
        simulator.register("cfg",
                new Day142RefreshScope.RefreshableConfig("old"));
        boolean refreshed = simulator.refresh("cfg", "new");
        assertTrue(refreshed);
        assertEquals("new", simulator.get("cfg").getValue());
        assertEquals(1, simulator.refreshedCount());
    }

    @Test
    void testSimulatorRefreshNonExistentReturnsFalse() {
        Day142RefreshScope.RefreshScopeSimulator simulator =
                new Day142RefreshScope.RefreshScopeSimulator();
        assertFalse(simulator.refresh("nonExistent", "val"));
    }

    @Test
    void testHowRefreshWorks() {
        List<String> steps = Day142RefreshScope.howRefreshWorks();
        assertEquals(7, steps.size());
    }

    @Test
    void testCommonPitfalls() {
        List<Day142RefreshScope.Pitfall> pitfalls = Day142RefreshScope.commonPitfalls();
        assertEquals(5, pitfalls.size());
    }
}
