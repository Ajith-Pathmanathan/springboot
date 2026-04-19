package com.techleadguru.phase5.day105;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day105Phase5ChecklistTest {

    @Test
    void getMemoryChecks_is_not_empty() {
        assertThat(Day105Phase5Checklist.getMemoryChecks()).isNotEmpty();
    }

    @Test
    void getThreadingChecks_contains_OSIV_item() {
        var items = Day105Phase5Checklist.getThreadingChecks();
        boolean hasOsiv = items.stream()
                .anyMatch(c -> c.item().toLowerCase().contains("open session") ||
                               c.description().toLowerCase().contains("osiv") ||
                               c.description().toLowerCase().contains("open-in-view"));
        assertThat(hasOsiv).isTrue();
    }

    @Test
    void getGcChecks_contains_gc_logging_item() {
        var items = Day105Phase5Checklist.getGcChecks();
        boolean hasGcLog = items.stream()
                .anyMatch(c -> c.item().toLowerCase().contains("gc log") ||
                               c.description().toLowerCase().contains("gc log") ||
                               c.description().toLowerCase().contains("-xlog"));
        assertThat(hasGcLog).isTrue();
    }

    @Test
    void getObservabilityChecks_contains_actuator_item() {
        var items = Day105Phase5Checklist.getObservabilityChecks();
        boolean hasActuator = items.stream()
                .anyMatch(c -> c.description().toLowerCase().contains("actuator") ||
                               c.item().toLowerCase().contains("actuator"));
        assertThat(hasActuator).isTrue();
    }

    @Test
    void getAllChecks_returns_all_categories() {
        var all = Day105Phase5Checklist.getAllChecks();
        var categories = all.stream().map(Day105Phase5Checklist.CheckItem::category).distinct().toList();
        assertThat(categories).containsExactlyInAnyOrder("Memory", "Threading", "GC", "Observability");
    }

    @Test
    void countRequired_counts_only_required_items() {
        long required = Day105Phase5Checklist.countRequired();
        long allRequired = Day105Phase5Checklist.getAllChecks().stream()
                .filter(c -> c.status() == Day105Phase5Checklist.CheckStatus.REQUIRED)
                .count();
        assertThat(required).isEqualTo(allRequired).isGreaterThan(0);
    }

    @Test
    void countByCategory_has_all_four_categories() {
        var counts = Day105Phase5Checklist.countByCategory();
        assertThat(counts).containsKeys("Memory", "Threading", "GC", "Observability");
    }

    @Test
    void countByCategory_counts_match_individual_list_sizes() {
        var counts = Day105Phase5Checklist.countByCategory();
        assertThat(counts.get("Memory")).isEqualTo((long) Day105Phase5Checklist.getMemoryChecks().size());
        assertThat(counts.get("Threading")).isEqualTo((long) Day105Phase5Checklist.getThreadingChecks().size());
        assertThat(counts.get("GC")).isEqualTo((long) Day105Phase5Checklist.getGcChecks().size());
        assertThat(counts.get("Observability")).isEqualTo((long) Day105Phase5Checklist.getObservabilityChecks().size());
    }

    @Test
    void all_check_items_have_non_blank_fields() {
        for (var item : Day105Phase5Checklist.getAllChecks()) {
            assertThat(item.category()).isNotBlank();
            assertThat(item.item()).isNotBlank();
            assertThat(item.description()).isNotBlank();
            assertThat(item.status()).isNotNull();
            assertThat(item.relatedDay()).isNotBlank();
        }
    }
}
