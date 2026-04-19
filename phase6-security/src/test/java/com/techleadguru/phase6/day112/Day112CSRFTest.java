package com.techleadguru.phase6.day112;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class Day112CSRFTest {

    @Test
    void csrfPolicyGuide_has_6_scenarios() {
        assertThat(Day112CSRF.csrfPolicyGuide()).hasSize(6);
    }

    @Test
    void each_scenario_has_non_blank_fields() {
        Day112CSRF.csrfPolicyGuide().forEach(s -> {
            assertThat(s.apiType()).isNotBlank();
            assertThat(s.reason()).isNotBlank();
            assertThat(s.recommendedPolicy()).isNotNull();
        });
    }

    @Test
    void stateless_jwt_api_recommends_disable() {
        var scenario = Day112CSRF.csrfPolicyGuide().stream()
                .filter(s -> s.apiType().contains("Stateless REST API using JWT Bearer"))
                .findFirst();
        assertThat(scenario).isPresent();
        assertThat(scenario.get().recommendedPolicy()).isEqualTo(Day112CSRF.CsrfPolicy.DISABLE);
    }

    @Test
    void session_cookie_app_recommends_enable() {
        var scenario = Day112CSRF.csrfPolicyGuide().stream()
                .filter(s -> s.apiType().contains("Thymeleaf"))
                .findFirst();
        assertThat(scenario).isPresent();
        assertThat(scenario.get().recommendedPolicy()).isEqualTo(Day112CSRF.CsrfPolicy.ENABLE);
    }

    @Test
    void recommendedPolicyFor_returns_enable_for_unknown_type() {
        // Unknown types should default to the safe option (ENABLE)
        assertThat(Day112CSRF.recommendedPolicyFor("unknown type"))
                .isEqualTo(Day112CSRF.CsrfPolicy.ENABLE);
    }

    @Test
    void recommendedPolicyFor_returns_disable_for_stateless_jwt_api() {
        String apiType = "Stateless REST API using JWT Bearer tokens";
        assertThat(Day112CSRF.recommendedPolicyFor(apiType))
                .isEqualTo(Day112CSRF.CsrfPolicy.DISABLE);
    }

    @Test
    void disableCsrfSnippet_contains_csrf_disable() {
        String snippet = Day112CSRF.disableCsrfSnippet();
        assertThat(snippet).contains("csrf");
        assertThat(snippet).contains("disable");
    }

    @Test
    void enableCsrfWithCookieSnippet_contains_cookie_repository() {
        String snippet = Day112CSRF.enableCsrfWithCookieSnippet();
        assertThat(snippet).containsIgnoringCase("cookie");
        assertThat(snippet).containsIgnoringCase("csrf");
    }

    @Test
    void attackVectors_has_4_entries() {
        assertThat(Day112CSRF.attackVectors()).hasSize(4);
    }

    @Test
    void attackVectors_all_non_blank() {
        Day112CSRF.attackVectors().forEach(v -> assertThat(v).isNotBlank());
    }
}
