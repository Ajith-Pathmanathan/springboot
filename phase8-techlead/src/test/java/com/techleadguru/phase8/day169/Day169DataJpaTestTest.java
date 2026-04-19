package com.techleadguru.phase8.day169;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Day169DataJpaTestTest {

    @Test
    void testOrderBuilderProducesExpectedMap() {
        var order = Day169DataJpaTest.TestOrderBuilder.anOrder()
                .customerId("cust-1")
                .status("CONFIRMED")
                .total(299.99)
                .currency("USD")
                .build();

        assertEquals("cust-1",   order.get("customerId"));
        assertEquals("CONFIRMED", order.get("status"));
        assertEquals(299.99,      (double) order.get("total"), 0.001);
        assertEquals("USD",       order.get("currency"));
    }

    @Test
    void testOrderBuilderUsesDefaults() {
        var order = Day169DataJpaTest.TestOrderBuilder.anOrder().build();
        assertEquals("customer-1", order.get("customerId"));
        assertEquals("PENDING",    order.get("status"));
    }

    @Test
    void dataJpaTestAnnotationsListIsNonEmpty() {
        var list = Day169DataJpaTest.dataJpaTestAnnotations();
        assertFalse(list.isEmpty());
        boolean hasDataJpa = list.stream().anyMatch(a -> a.annotation().equals("@DataJpaTest"));
        assertTrue(hasDataJpa);
    }

    @Test
    void queryTestPatternsAreNonEmpty() {
        var patterns = Day169DataJpaTest.queryTestPatterns();
        assertFalse(patterns.isEmpty());
        patterns.forEach(p -> assertNotNull(p.queryType()));
    }

    @Test
    void transactionBehaviourGuideIsNonEmpty() {
        var guide = Day169DataJpaTest.transactionBehaviourGuide();
        assertFalse(guide.isEmpty());
        assertTrue(guide.stream().anyMatch(s -> s.toLowerCase().contains("roll")));
    }

    @Test
    void repositoryTestGuideHasSixSteps() {
        var steps = Day169DataJpaTest.repositoryTestGuide();
        assertEquals(6, steps.size());
    }
}
