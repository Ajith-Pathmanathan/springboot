package com.techleadguru.phase8.day179;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Day179IncidentRunbookTest {

    @Test
    void oomRunbookHasExpectedType() {
        var runbook = Day179IncidentRunbook.oomRunbook();
        assertEquals(Day179IncidentRunbook.IncidentType.OOM, runbook.type());
    }

    @Test
    void oomRunbookHasStepsAndSymptoms() {
        var runbook = Day179IncidentRunbook.oomRunbook();
        assertFalse(runbook.symptoms().isEmpty());
        assertFalse(runbook.steps().isEmpty());
        assertFalse(runbook.preventionActions().isEmpty());
    }

    @Test
    void oomRunbookStepsAreOrdered() {
        var steps = Day179IncidentRunbook.oomRunbook().steps();
        for (int i = 0; i < steps.size(); i++) {
            assertEquals(i + 1, steps.get(i).order());
        }
    }

    @Test
    void poolExhaustionRunbookHasDbCommands() {
        var runbook = Day179IncidentRunbook.poolExhaustionRunbook();
        assertEquals(Day179IncidentRunbook.IncidentType.POOL_EXHAUSTION, runbook.type());
        boolean hasDbQuery = runbook.steps().stream()
                .anyMatch(s -> s.command().toLowerCase().contains("pg_stat_activity"));
        assertTrue(hasDbQuery);
    }

    @Test
    void deadlockRunbookHasThreadDumpStep() {
        var runbook = Day179IncidentRunbook.deadlockRunbook();
        assertEquals(Day179IncidentRunbook.IncidentType.DEADLOCK, runbook.type());
        boolean hasThreadDump = runbook.steps().stream()
                .anyMatch(s -> s.command().toLowerCase().contains("jcmd") ||
                               s.command().toLowerCase().contains("jstack"));
        assertTrue(hasThreadDump);
    }

    @Test
    void diagnosticCommandsMapIsNonEmpty() {
        var commands = Day179IncidentRunbook.diagnosticCommands();
        assertFalse(commands.isEmpty());
        assertTrue(commands.containsKey("JVM heap dump"));
        assertTrue(commands.containsKey("JVM thread dump"));
    }
}
