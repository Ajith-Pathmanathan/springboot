package com.techleadguru.phase8.day177;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day177ADRTest {

    @Test
    void adrRegistryStoresAndRetrievesById() {
        var registry = new Day177ADR.AdrRegistry();
        var adr = new Day177ADR.Adr(
                "ADR-001", "Use PostgreSQL", Day177ADR.AdrStatus.ACCEPTED,
                "Need relational DB", "Use PostgreSQL 16",
                List.of("ACID guarantees", "PG expertise"),
                List.of("MySQL"),
                LocalDate.of(2024, 1, 1)
        );
        registry.register(adr);

        assertTrue(registry.findById("ADR-001").isPresent());
        assertEquals("Use PostgreSQL", registry.findById("ADR-001").get().title());
    }

    @Test
    void adrRegistryFiltersByStatus() {
        var registry = new Day177ADR.AdrRegistry();
        Day177ADR.sampleAdrs().forEach(registry::register);

        var accepted = registry.findByStatus(Day177ADR.AdrStatus.ACCEPTED);
        assertEquals(3, accepted.size()); // all 3 sample ADRs are ACCEPTED
    }

    @Test
    void adrRegistryListAllReturnsCopy() {
        var registry = new Day177ADR.AdrRegistry();
        Day177ADR.sampleAdrs().forEach(registry::register);
        assertEquals(3, registry.listAll().size());
        assertEquals(3, registry.size());
    }

    @Test
    void adrTemplateContainsKeyFields() {
        var template = Day177ADR.adrTemplate();
        assertFalse(template.isEmpty());
        assertTrue(template.stream().anyMatch(l -> l.contains("Context")));
        assertTrue(template.stream().anyMatch(l -> l.contains("Decision")));
        assertTrue(template.stream().anyMatch(l -> l.contains("Consequences")));
    }

    @Test
    void sampleAdrsHaveThreeRecords() {
        var adrs = Day177ADR.sampleAdrs();
        assertEquals(3, adrs.size());
        adrs.forEach(a -> {
            assertNotNull(a.id());
            assertNotNull(a.decision());
            assertFalse(a.consequences().isEmpty());
        });
    }
}
