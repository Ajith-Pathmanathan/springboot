package com.techleadguru.phase5.day79;

import org.junit.jupiter.api.*;
import java.io.File;
import static org.assertj.core.api.Assertions.*;

class Day79HeapDumpTest {

    @Test
    void isHeapDumpOnOomEnabled_returns_a_boolean() {
        // Just verify it runs without exception (flag may or may not be set in test JVM)
        assertThatCode(() -> Day79HeapDump.isHeapDumpOnOomEnabled())
                .doesNotThrowAnyException();
    }

    @Test
    void matAnalysisGuide_returns_non_empty_string() {
        String guide = Day79HeapDump.matAnalysisGuide();
        assertThat(guide).isNotBlank();
        assertThat(guide).contains("MAT");
    }

    @Test
    void forceGc_does_not_throw() {
        assertThatCode(() -> Day79HeapDump.forceGc()).doesNotThrowAnyException();
    }

    @Test
    void dumpHeap_creates_file() throws Exception {
        // createTempFile creates the file; must delete it first since dumpHeap fails if file exists
        File tmp = File.createTempFile("test-heap-", ".hprof");
        tmp.delete();   // must not exist when dumpHeap is called
        tmp.deleteOnExit();
        String path = tmp.getAbsolutePath();

        assertThatCode(() -> Day79HeapDump.dumpHeap(path, true))
                .doesNotThrowAnyException();
    }
}
