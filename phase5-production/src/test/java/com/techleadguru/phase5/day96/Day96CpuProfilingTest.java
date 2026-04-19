package com.techleadguru.phase5.day96;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day96CpuProfilingTest {

    @Test
    void asyncProfilerCommand_contains_pid_and_duration() {
        String cmd = Day96CpuProfiling.ProfilerConfig.asyncProfilerCommand(
                12345L, 60, Day96CpuProfiling.ProfilerConfig.ProfileMode.CPU, "/tmp/cpu.html");
        assertThat(cmd).contains("12345");
        assertThat(cmd).contains("60");
        assertThat(cmd).contains("/tmp/cpu.html");
        assertThat(cmd).contains("cpu");
    }

    @Test
    void asyncProfilerCommand_alloc_mode_has_alloc_event() {
        String cmd = Day96CpuProfiling.ProfilerConfig.asyncProfilerCommand(
                1L, 30, Day96CpuProfiling.ProfilerConfig.ProfileMode.ALLOC, "/tmp/alloc.html");
        assertThat(cmd).contains("alloc");
    }

    @Test
    void jfrStartCommand_contains_name_and_maxsize() {
        String cmd = Day96CpuProfiling.ProfilerConfig.jfrStartCommand(12345L, "prof", 256);
        assertThat(cmd).contains("12345");
        assertThat(cmd).contains("prof");
        assertThat(cmd).contains("256");
        assertThat(cmd).containsIgnoringCase("JFR.start");
    }

    @Test
    void jfrDumpCommand_contains_name_and_output_file() {
        String cmd = Day96CpuProfiling.ProfilerConfig.jfrDumpCommand(12345L, "prof", "/tmp/out.jfr");
        assertThat(cmd).contains("prof");
        assertThat(cmd).contains("/tmp/out.jfr");
        assertThat(cmd).containsIgnoringCase("JFR.dump");
    }

    @Test
    void jfrStopCommand_contains_name() {
        String cmd = Day96CpuProfiling.ProfilerConfig.jfrStopCommand(12345L, "myRec");
        assertThat(cmd).contains("myRec");
        assertThat(cmd).containsIgnoringCase("JFR.stop");
    }

    @Test
    void flameGraphCommand_is_a_cpu_profile_command() {
        String cmd = Day96CpuProfiling.ProfilerConfig.flameGraphCommand(12345L, 30, "/tmp/fg.html");
        assertThat(cmd).contains("cpu");
        assertThat(cmd).contains("/tmp/fg.html");
    }

    @Test
    void commonPatterns_returns_non_empty_list() {
        var patterns = Day96CpuProfiling.commonPatterns();
        assertThat(patterns).isNotEmpty();
    }

    @Test
    void commonPatterns_each_has_symptom_rootCause_fix() {
        for (var pattern : Day96CpuProfiling.commonPatterns()) {
            assertThat(pattern.symptom()).isNotBlank();
            assertThat(pattern.rootCause()).isNotBlank();
            assertThat(pattern.fix()).isNotBlank();
        }
    }
}
