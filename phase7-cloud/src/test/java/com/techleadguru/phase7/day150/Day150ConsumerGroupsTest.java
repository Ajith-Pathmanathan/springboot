package com.techleadguru.phase7.day150;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day150ConsumerGroupsTest {

    @Test
    void testSingleConsumerGetsAllPartitions() {
        Day150ConsumerGroups.ConsumerGroupSimulator sim =
                new Day150ConsumerGroups.ConsumerGroupSimulator("orders-grp", 3);
        sim.join("consumer-1");
        Map<String, List<Integer>> assignments = sim.getAssignments();
        assertEquals(3, assignments.get("consumer-1").size());
    }

    @Test
    void testTwoConsumersSharePartitions() {
        Day150ConsumerGroups.ConsumerGroupSimulator sim =
                new Day150ConsumerGroups.ConsumerGroupSimulator("orders-grp", 4);
        sim.join("c1");
        sim.join("c2");
        Map<String, List<Integer>> assignments = sim.getAssignments();
        assertEquals(2, assignments.get("c1").size());
        assertEquals(2, assignments.get("c2").size());
    }

    @Test
    void testUnevenPartitionDistribution() {
        Day150ConsumerGroups.ConsumerGroupSimulator sim =
                new Day150ConsumerGroups.ConsumerGroupSimulator("grp", 5);
        sim.join("c1");
        sim.join("c2");
        Map<String, List<Integer>> assignments = sim.getAssignments();
        int total = assignments.values().stream().mapToInt(List::size).sum();
        assertEquals(5, total);
    }

    @Test
    void testRebalanceOnLeave() {
        Day150ConsumerGroups.ConsumerGroupSimulator sim =
                new Day150ConsumerGroups.ConsumerGroupSimulator("grp", 4);
        sim.join("c1");
        sim.join("c2");
        int rebalancesAfterJoins = sim.rebalanceCount();
        sim.leave("c2");
        assertEquals(rebalancesAfterJoins + 1, sim.rebalanceCount());
        // c1 should now own all 4 partitions
        assertEquals(4, sim.getAssignments().get("c1").size());
    }

    @Test
    void testRebalanceSteps() {
        List<String> steps = Day150ConsumerGroups.rebalanceSteps();
        assertEquals(8, steps.size());
    }

    @Test
    void testScalingGuide() {
        List<Day150ConsumerGroups.ScalingRule> guide = Day150ConsumerGroups.scalingGuide();
        assertFalse(guide.isEmpty());
        assertTrue(guide.stream().anyMatch(r -> r.scenario().contains("==")));
    }
}
