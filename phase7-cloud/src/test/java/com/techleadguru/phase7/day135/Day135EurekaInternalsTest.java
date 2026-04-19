package com.techleadguru.phase7.day135;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day135EurekaInternalsTest {

    @Test
    void testShouldEvictExpiredLease() {
        long now = 1_000_000L;
        // lease renewed at 0, duration 100s
        Day135EurekaInternals.InstanceLease lease =
                new Day135EurekaInternals.InstanceLease("inst-1", 0L, 100);
        assertTrue(Day135EurekaInternals.shouldEvict(lease, now));
    }

    @Test
    void testShouldNotEvictActiveLease() {
        long now = System.currentTimeMillis();
        Day135EurekaInternals.InstanceLease lease =
                new Day135EurekaInternals.InstanceLease("inst-1", now, 90);
        assertFalse(Day135EurekaInternals.shouldEvict(lease, now));
    }

    @Test
    void testSelfPreservationTriggered() {
        Day135EurekaInternals.SelfPreservationCheck check =
                Day135EurekaInternals.evaluateSelfPreservation(10, 5, 0.85);
        // expected = 10*2 = 20; 5 < 20*0.85 = 17 → triggered
        assertTrue(check.selfPreservationTriggered());
    }

    @Test
    void testSelfPreservationNotTriggered() {
        Day135EurekaInternals.SelfPreservationCheck check =
                Day135EurekaInternals.evaluateSelfPreservation(10, 20, 0.85);
        // 20 >= 20*0.85 = 17 → not triggered
        assertFalse(check.selfPreservationTriggered());
    }

    @Test
    void testDefaultRenewalPercentThreshold() {
        assertEquals(0.85, Day135EurekaInternals.defaultRenewalPercentThreshold(), 1e-9);
    }

    @Test
    void testApplyDeltaAdded() {
        Map<String, String> current = new HashMap<>();
        current.put("inst-1", "ORDER-SERVICE");
        Day135EurekaInternals.DeltaEntry entry =
                new Day135EurekaInternals.DeltaEntry("inst-2",
                        Day135EurekaInternals.DeltaAction.ADDED, "ORDER-SERVICE");
        Map<String, String> result = Day135EurekaInternals.applyDelta(current, List.of(entry));
        assertTrue(result.containsKey("inst-2"));
    }

    @Test
    void testApplyDeltaDeleted() {
        Map<String, String> current = new HashMap<>();
        current.put("inst-1", "ORDER-SERVICE");
        Day135EurekaInternals.DeltaEntry entry =
                new Day135EurekaInternals.DeltaEntry("inst-1",
                        Day135EurekaInternals.DeltaAction.DELETED, "ORDER-SERVICE");
        Map<String, String> result = Day135EurekaInternals.applyDelta(current, List.of(entry));
        assertFalse(result.containsKey("inst-1"));
    }

    @Test
    void testConfigurationTips() {
        List<Day135EurekaInternals.ConfigTip> tips =
                Day135EurekaInternals.configurationTips();
        assertEquals(5, tips.size());
    }
}
