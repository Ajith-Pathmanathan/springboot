package com.techleadguru.phase8.day180;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Day180CapstonTest {

    @Test
    void orderManagementSystemHasAllComponents() {
        var design = Day180Capstone.orderManagementSystem();
        assertEquals("Order Management System", design.name());

        var types = design.components().stream()
                .map(Day180Capstone.ArchitectureComponent::type)
                .toList();

        assertTrue(types.contains(Day180Capstone.ComponentType.API_GATEWAY));
        assertTrue(types.contains(Day180Capstone.ComponentType.ORDER_SERVICE));
        assertTrue(types.contains(Day180Capstone.ComponentType.PAYMENT_SERVICE));
        assertTrue(types.contains(Day180Capstone.ComponentType.INVENTORY_SERVICE));
        assertTrue(types.contains(Day180Capstone.ComponentType.NOTIFICATION_SERVICE));
        assertTrue(types.contains(Day180Capstone.ComponentType.EVENT_BUS));
    }

    @Test
    void systemDesignReferencesAdrs() {
        var design = Day180Capstone.orderManagementSystem();
        assertFalse(design.adrs().isEmpty());
        assertTrue(design.adrs().stream().anyMatch(a -> a.contains("ADR-001")));
    }

    @Test
    void dataFlowStepsAreOrdered() {
        var steps = Day180Capstone.dataFlowSteps();
        assertFalse(steps.isEmpty());
        assertTrue(steps.get(0).startsWith("1."));
        // last step starts with its step number
        assertTrue(steps.get(steps.size() - 1).contains("."));
    }

    @Test
    void designDecisionsCoversKeyAreas() {
        var decisions = Day180Capstone.designDecisions();
        assertFalse(decisions.isEmpty());
        boolean hasConsistency = decisions.stream()
                .anyMatch(d -> d.area().toLowerCase().contains("consistency"));
        assertTrue(hasConsistency);
    }

    @Test
    void scalabilityConsiderationsAreNonEmpty() {
        var considerations = Day180Capstone.scalabilityConsiderations();
        assertFalse(considerations.isEmpty());
        assertTrue(considerations.stream().anyMatch(c -> c.toLowerCase().contains("kafka")));
    }

    @Test
    void eachComponentHasResponsibilitiesAndTechnology() {
        Day180Capstone.orderManagementSystem().components().forEach(c -> {
            assertNotNull(c.technology());
            assertFalse(c.responsibilities().isEmpty());
        });
    }
}
