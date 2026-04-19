package com.techleadguru.phase8.day167;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Day167UnitTestingTest {

    @Test
    void testDoubleGuideContainsAllTypes() {
        var guide = Day167UnitTesting.testDoubleGuide();
        assertEquals(5, guide.size()); // DUMMY STUB FAKE MOCK SPY

        boolean hasMock = guide.stream().anyMatch(i -> i.type() == Day167UnitTesting.TestDoubleType.MOCK);
        boolean hasSpy  = guide.stream().anyMatch(i -> i.type() == Day167UnitTesting.TestDoubleType.SPY);
        assertTrue(hasMock);
        assertTrue(hasSpy);
    }

    @Test
    void testDoubleInfoFieldsAreNonNull() {
        Day167UnitTesting.testDoubleGuide().forEach(info -> {
            assertNotNull(info.description());
            assertNotNull(info.mockitoCreation());
            assertNotNull(info.bestUseCase());
        });
    }

    @Test
    void argumentCaptorExamplesAreNonEmpty() {
        var examples = Day167UnitTesting.argumentCaptorExamples();
        assertFalse(examples.isEmpty());
        examples.forEach(ex -> {
            assertNotNull(ex.scenario());
            assertNotNull(ex.captorDeclaration());
            assertNotNull(ex.verifyLine());
        });
    }

    @Test
    void mockitoPatternsCoverKeyConcepts() {
        var patterns = Day167UnitTesting.mockitoPatterns();
        assertFalse(patterns.isEmpty());
        boolean hasVerify = patterns.stream().anyMatch(p -> p.name().equalsIgnoreCase("verify"));
        assertTrue(hasVerify);
    }

    @Test
    void mockVsSpyComparisonIsNonEmpty() {
        var list = Day167UnitTesting.mockVsSpy();
        assertFalse(list.isEmpty());
        list.forEach(info -> assertNotNull(info.aspect()));
    }
}
