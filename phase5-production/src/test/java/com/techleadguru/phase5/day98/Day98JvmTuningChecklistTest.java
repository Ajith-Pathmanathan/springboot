package com.techleadguru.phase5.day98;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day98JvmTuningChecklistTest {

    @Test
    void getProductionFlags_contains_required_memory_flags() {
        var flags = Day98JvmTuningChecklist.TuningChecklist.getProductionFlags();
        assertThat(flags).containsKey("-Xms");
        assertThat(flags).containsKey("-Xmx");
        assertThat(flags).containsKey("-XX:MaxMetaspaceSize=256m");
    }

    @Test
    void getProductionFlags_contains_gc_flags() {
        var flags = Day98JvmTuningChecklist.TuningChecklist.getProductionFlags();
        assertThat(flags).containsKey("-XX:+UseG1GC");
        assertThat(flags).containsKey("-XX:MaxGCPauseMillis=200");
    }

    @Test
    void getProductionFlags_contains_oom_flags() {
        var flags = Day98JvmTuningChecklist.TuningChecklist.getProductionFlags();
        assertThat(flags).containsKey("-XX:+HeapDumpOnOutOfMemoryError");
        assertThat(flags).containsKey("-XX:+ExitOnOutOfMemoryError");
    }

    @Test
    void getProductionFlags_returns_at_least_10_flags() {
        var flags = Day98JvmTuningChecklist.TuningChecklist.getProductionFlags();
        assertThat(flags).hasSizeGreaterThanOrEqualTo(10);
    }

    @Test
    void analyzeCurrentJvm_returns_list() {
        var warnings = Day98JvmTuningChecklist.TuningChecklist.analyzeCurrentJvm();
        assertThat(warnings).isNotNull();
        // In test environment, several flags are normally missing — just check no crash
    }

    @Test
    void recommendedArgsSnippet_contains_xmx() {
        String snippet = Day98JvmTuningChecklist.TuningChecklist.recommendedArgsSnippet();
        assertThat(snippet).contains("-Xmx");
    }

    @Test
    void recommendedArgsSnippet_contains_gc_log_setting() {
        String snippet = Day98JvmTuningChecklist.TuningChecklist.recommendedArgsSnippet();
        assertThat(snippet).contains("-Xlog:gc");
    }

    @Test
    void recommendedArgsSnippet_contains_heap_dump_flag() {
        String snippet = Day98JvmTuningChecklist.TuningChecklist.recommendedArgsSnippet();
        assertThat(snippet).contains("HeapDumpOnOutOfMemoryError");
    }
}
