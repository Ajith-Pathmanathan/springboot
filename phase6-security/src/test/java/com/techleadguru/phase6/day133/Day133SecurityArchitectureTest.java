package com.techleadguru.phase6.day133;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class Day133SecurityArchitectureTest {

    @Test
    void architectureGuide_has_five_decisions() {
        List<Day133SecurityArchitecture.ArchitectureDecision> decisions =
                Day133SecurityArchitecture.architectureGuide();
        assertThat(decisions).hasSize(5);
    }

    @Test
    void architectureGuide_covers_all_client_types() {
        List<Day133SecurityArchitecture.ArchitectureDecision> decisions =
                Day133SecurityArchitecture.architectureGuide();
        Set<Day133SecurityArchitecture.ClientType> coveredTypes = new java.util.HashSet<>();
        decisions.forEach(d -> coveredTypes.add(d.clientType()));
        assertThat(coveredTypes).containsAll(
                java.util.Arrays.asList(Day133SecurityArchitecture.ClientType.values()));
    }

    @Test
    void architectureGuide_fields_are_non_blank() {
        Day133SecurityArchitecture.architectureGuide().forEach(d -> {
            assertThat(d.recommendedFlow()).isNotBlank();
            assertThat(d.tokenStorage()).isNotBlank();
            assertThat(d.refreshStrategy()).isNotBlank();
            assertThat(d.csrfStrategy()).isNotBlank();
        });
    }

    @Test
    void productionChecklist_has_twenty_items() {
        List<Day133SecurityArchitecture.SecurityChecklistItem> checklist =
                Day133SecurityArchitecture.productionChecklist();
        assertThat(checklist).hasSize(20);
    }

    @Test
    void productionChecklist_items_have_non_blank_fields() {
        Day133SecurityArchitecture.productionChecklist().forEach(item -> {
            assertThat(item.category()).isNotBlank();
            assertThat(item.item()).isNotBlank();
        });
    }

    @Test
    void countCriticalItems_is_positive() {
        assertThat(Day133SecurityArchitecture.countCriticalItems()).isGreaterThan(0);
    }

    @Test
    void categories_is_non_empty() {
        List<String> cats = Day133SecurityArchitecture.categories();
        assertThat(cats).isNotEmpty();
    }

    @Test
    void categories_includes_expected_security_topics() {
        List<String> cats = Day133SecurityArchitecture.categories();
        String allCats = String.join(" ", cats).toLowerCase();
        // At least some of token/password/pkce/csrf categories should be present
        assertThat(allCats).containsAnyOf("token", "password", "pkce", "csrf", "key", "jwt");
    }

    @Test
    void clientType_enum_has_five_values() {
        assertThat(Day133SecurityArchitecture.ClientType.values()).hasSize(5);
    }

    @Test
    void productionChecklist_contains_critical_and_non_critical_items() {
        List<Day133SecurityArchitecture.SecurityChecklistItem> checklist =
                Day133SecurityArchitecture.productionChecklist();
        long criticalCount = checklist.stream().filter(Day133SecurityArchitecture.SecurityChecklistItem::critical).count();
        long nonCriticalCount = checklist.stream().filter(i -> !i.critical()).count();
        assertThat(criticalCount).isGreaterThan(0);
        assertThat(nonCriticalCount).isGreaterThan(0);
    }
}
