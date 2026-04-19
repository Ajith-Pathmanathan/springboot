package com.techleadguru.phase5.day91;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day91ProductionDiagnosisTest {

    private final Day91ProductionDiagnosis.ThreadDiagnostic diagnostic =
            new Day91ProductionDiagnosis.ThreadDiagnostic();

    @Test
    void captureSnapshot_returns_positive_thread_counts() {
        var snapshot = diagnostic.captureSnapshot();
        assertThat(snapshot.totalThreads()).isPositive();
        assertThat(snapshot.peakThreads()).isPositive();
        assertThat(snapshot.daemonThreads()).isGreaterThanOrEqualTo(0);
        assertThat(snapshot.startedThreads()).isPositive();
    }

    @Test
    void captureSnapshot_deadlockedIds_empty_normally() {
        var snapshot = diagnostic.captureSnapshot();
        assertThat(snapshot.deadlockedIds()).isEmpty();
    }

    @Test
    void captureSnapshot_blockedThreadNames_is_not_null() {
        var snapshot = diagnostic.captureSnapshot();
        assertThat(snapshot.blockedThreadNames()).isNotNull();
    }

    @Test
    void topCpuThreads_returns_limited_results() {
        var threads = diagnostic.topCpuThreads(3);
        assertThat(threads).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void cpuHotThread_hexId_has_0x_prefix() {
        var threads = diagnostic.topCpuThreads(5);
        if (!threads.isEmpty()) {
            assertThat(threads.get(0).hexId()).startsWith("0x");
        }
    }

    @Test
    void threadsByState_contains_all_states() {
        var byState = diagnostic.threadsByState();
        assertThat(byState).containsKeys(
                Thread.State.RUNNABLE,
                Thread.State.WAITING,
                Thread.State.TIMED_WAITING
        );
    }

    @Test
    void diagnosisPlaybook_steps_are_non_empty() {
        var steps = Day91ProductionDiagnosis.DiagnosisPlaybook.getSteps();
        assertThat(steps).isNotEmpty();
    }

    @Test
    void diagnosisPlaybook_steps_are_ordered() {
        var steps = Day91ProductionDiagnosis.DiagnosisPlaybook.getSteps();
        for (int i = 0; i < steps.size(); i++) {
            assertThat(steps.get(i).order()).isEqualTo(i + 1);
        }
    }

    @Test
    void diagnosisPlaybook_each_step_has_symptom_and_tool() {
        var steps = Day91ProductionDiagnosis.DiagnosisPlaybook.getSteps();
        for (var step : steps) {
            assertThat(step.symptom()).isNotBlank();
            assertThat(step.tool()).isNotBlank();
            assertThat(step.command()).isNotBlank();
        }
    }
}
