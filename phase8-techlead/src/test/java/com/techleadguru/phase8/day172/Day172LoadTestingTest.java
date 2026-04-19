package com.techleadguru.phase8.day172;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day172LoadTestingTest {

    @Test
    void smokeTestHasOneVu() {
        var scenario = Day172LoadTesting.smokeTest();
        assertEquals("smoke", scenario.name());
        assertEquals(1, scenario.vus());
    }

    @Test
    void loadTestResultMeetsThresholdWhenWithinLimits() {
        var result = new Day172LoadTesting.LoadTestResult(10000, 0.001, 800, 1500, 55.0);
        assertTrue(result.meetsThreshold(1000, 0.01));
    }

    @Test
    void loadTestResultFailsThresholdWhenLatencyExceeded() {
        var result = new Day172LoadTesting.LoadTestResult(5000, 0.005, 1500, 3000, 30.0);
        assertFalse(result.meetsThreshold(1000, 0.01)); // p95 > 1000
    }

    @Test
    void thresholdEvaluatorPassesWhenAllThresholdsOk() {
        var evaluator = new Day172LoadTesting.ThresholdEvaluator();
        var result = new Day172LoadTesting.LoadTestResult(8000, 0.002, 700, 1200, 60.0);
        var thresholds = List.of(
                new Day172LoadTesting.ThresholdSpec("failure_rate", "<", "0.01"),
                new Day172LoadTesting.ThresholdSpec("p95_latency",  "<", "1000"),
                new Day172LoadTesting.ThresholdSpec("rps",          ">", "50")
        );
        assertTrue(evaluator.evaluate(result, thresholds));
    }

    @Test
    void k6ScriptExampleContainsStages() {
        String script = Day172LoadTesting.k6ScriptExample();
        assertTrue(script.contains("stages"));
        assertTrue(script.contains("thresholds"));
    }

    @Test
    void loadTestTypesContainFiveTypes() {
        assertEquals(5, Day172LoadTesting.loadTestTypes().size());
    }

    @Test
    void interpretResultsAreNonEmpty() {
        assertFalse(Day172LoadTesting.interpretResults().isEmpty());
    }
}
