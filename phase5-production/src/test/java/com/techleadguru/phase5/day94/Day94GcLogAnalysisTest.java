package com.techleadguru.phase5.day94;

import org.junit.jupiter.api.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class Day94GcLogAnalysisTest {

    // Sample GC log lines in unified JVM logging format
    // ISO-8601 timestamps require +HH:MM format (not +HHMM)
    private static final String YOUNG_GC =
            "[2024-01-15T10:23:45.123+00:00][1.234s][info][gc] GC(42) Pause Young (Normal) (G1 Evacuation Pause) 512M->480M(8192M) 12.345ms";
    private static final String FULL_GC =
            "[2024-01-15T10:24:05.789+00:00][21.900s][warn][gc] GC(44) Pause Full (G1 Evacuation Failure) 7800M->3200M(8192M) 15234.567ms";
    private static final String NON_GC =
            "Some random log line that is not a GC event";

    @Test
    void parseEntry_parses_valid_young_gc_line() {
        var entry = Day94GcLogAnalysis.GcLogParser.parseEntry(YOUNG_GC);
        assertThat(entry).isPresent();
        assertThat(entry.get().heapBeforeMb()).isEqualTo(512);
        assertThat(entry.get().heapAfterMb()).isEqualTo(480);
        assertThat(entry.get().heapTotalMb()).isEqualTo(8192);
        assertThat(entry.get().pauseMs()).isCloseTo(12.345, within(0.001));
    }

    @Test
    void parseEntry_parses_full_gc_line() {
        var entry = Day94GcLogAnalysis.GcLogParser.parseEntry(FULL_GC);
        assertThat(entry).isPresent();
        assertThat(entry.get().isFull()).isTrue();
        assertThat(entry.get().pauseMs()).isGreaterThan(1000.0);
    }

    @Test
    void parseEntry_returns_empty_for_non_gc_line() {
        var entry = Day94GcLogAnalysis.GcLogParser.parseEntry(NON_GC);
        assertThat(entry).isEmpty();
    }

    @Test
    void gcLogEntry_reclaimedMb_is_correct() {
        var entry = Day94GcLogAnalysis.GcLogParser.parseEntry(YOUNG_GC).orElseThrow();
        assertThat(entry.reclaimedMb()).isEqualTo(512 - 480);
    }

    @Test
    void parseAll_returns_only_gc_entries() {
        var lines = List.of(YOUNG_GC, NON_GC, FULL_GC, "another non-gc line");
        var entries = Day94GcLogAnalysis.GcLogParser.parseAll(lines);
        assertThat(entries).hasSize(2);
    }

    @Test
    void findHighPausePeriods_filters_by_threshold() {
        var entries = Day94GcLogAnalysis.GcLogParser.parseAll(List.of(YOUNG_GC, FULL_GC));
        var high = Day94GcLogAnalysis.GcAnalyzer.findHighPausePeriods(entries, 100.0);
        assertThat(high).hasSize(1);
        assertThat(high.get(0).isFull()).isTrue();
    }

    @Test
    void computeAveragePause_calculates_correctly() {
        var entries = Day94GcLogAnalysis.GcLogParser.parseAll(List.of(YOUNG_GC, FULL_GC));
        double avg = Day94GcLogAnalysis.GcAnalyzer.computeAveragePause(entries);
        assertThat(avg).isGreaterThan(12.0);
    }

    @Test
    void computeGcOverheadPercent_calculates_for_window() {
        var entries = Day94GcLogAnalysis.GcLogParser.parseAll(List.of(YOUNG_GC, FULL_GC));
        double overhead = Day94GcLogAnalysis.GcAnalyzer.computeGcOverheadPercent(entries, 30_000.0);
        assertThat(overhead).isGreaterThan(0.0).isLessThan(100.0);
    }

    @Test
    void isHeapTrendingUp_detects_growing_heap() {
        // Build 8 entries with steadily increasing heapAfter (ISO-8601 +HH:MM timezone)
        var lines = List.of(
                "[2024-01-01T00:00:01.000+00:00][1.0s][info][gc] GC(1) Pause Young (Normal) 100M->90M(1000M) 5.0ms",
                "[2024-01-01T00:00:02.000+00:00][2.0s][info][gc] GC(2) Pause Young (Normal) 110M->100M(1000M) 5.0ms",
                "[2024-01-01T00:00:03.000+00:00][3.0s][info][gc] GC(3) Pause Young (Normal) 120M->110M(1000M) 5.0ms",
                "[2024-01-01T00:00:04.000+00:00][4.0s][info][gc] GC(4) Pause Young (Normal) 130M->120M(1000M) 5.0ms",
                "[2024-01-01T00:00:05.000+00:00][5.0s][info][gc] GC(5) Pause Young (Normal) 200M->180M(1000M) 5.0ms",
                "[2024-01-01T00:00:06.000+00:00][6.0s][info][gc] GC(6) Pause Young (Normal) 280M->260M(1000M) 5.0ms",
                "[2024-01-01T00:00:07.000+00:00][7.0s][info][gc] GC(7) Pause Young (Normal) 350M->330M(1000M) 5.0ms",
                "[2024-01-01T00:00:08.000+00:00][8.0s][info][gc] GC(8) Pause Young (Normal) 400M->380M(1000M) 5.0ms"
        );
        var entries = Day94GcLogAnalysis.GcLogParser.parseAll(lines);
        assertThat(Day94GcLogAnalysis.GcAnalyzer.isHeapTrendingUp(entries)).isTrue();
    }

    @Test
    void summarize_returns_complete_summary() {
        var entries = Day94GcLogAnalysis.GcLogParser.parseAll(List.of(YOUNG_GC, FULL_GC));
        var summary = Day94GcLogAnalysis.GcAnalyzer.summarize(entries);
        assertThat(summary.totalEvents()).isEqualTo(2);
        assertThat(summary.fullGcEvents()).isEqualTo(1);
        assertThat(summary.avgPauseMs()).isPositive();
        assertThat(summary.p95PauseMs()).isPositive();
        assertThat(summary.p99PauseMs()).isPositive();
    }

    @Test
    void summarize_returns_zeroes_for_empty_list() {
        var summary = Day94GcLogAnalysis.GcAnalyzer.summarize(List.of());
        assertThat(summary.totalEvents()).isZero();
        assertThat(summary.avgPauseMs()).isZero();
    }
}
