package com.techleadguru.phase6.day107;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class Day107LoginFlowTest {

    @Test
    void usernamePasswordLoginFlow_has_10_steps() {
        assertThat(Day107LoginFlow.usernamePasswordLoginFlow()).hasSize(10);
    }

    @Test
    void usernamePasswordLoginFlow_steps_are_in_order() {
        var steps = Day107LoginFlow.usernamePasswordLoginFlow();
        for (int i = 1; i < steps.size(); i++) {
            assertThat(steps.get(i).order())
                    .as("Step %d should have higher order than step %d", i + 1, i)
                    .isGreaterThan(steps.get(i - 1).order());
        }
    }

    @Test
    void usernamePasswordLoginFlow_first_step_is_filter_matching() {
        var first = Day107LoginFlow.usernamePasswordLoginFlow().getFirst();
        assertThat(first.component()).contains("UsernamePasswordAuthenticationFilter");
        assertThat(first.order()).isEqualTo(1);
    }

    @Test
    void usernamePasswordLoginFlow_last_step_is_failure_redirect() {
        var last = Day107LoginFlow.usernamePasswordLoginFlow().getLast();
        assertThat(last.action()).containsIgnoringCase("redirect");
        assertThat(last.order()).isEqualTo(10);
    }

    @Test
    void each_flow_step_has_non_blank_fields() {
        Day107LoginFlow.usernamePasswordLoginFlow().forEach(step -> {
            assertThat(step.component()).isNotBlank();
            assertThat(step.action()).isNotBlank();
            assertThat(step.detail()).isNotBlank();
        });
    }

    @Test
    void authenticationManagerComponents_contains_key_components() {
        var components = Day107LoginFlow.authenticationManagerComponents();
        assertThat(components).containsKey("AuthenticationManager");
        assertThat(components).containsKey("ProviderManager");
        assertThat(components).containsKey("UserDetailsService");
        assertThat(components).containsKey("PasswordEncoder");
        assertThat(components).containsKey("SecurityContextHolder");
    }

    @Test
    void bearerTokenFlow_has_6_steps() {
        assertThat(Day107LoginFlow.bearerTokenFlow()).hasSize(6);
    }

    @Test
    void bearerTokenFlow_first_step_extracts_bearer_token() {
        var first = Day107LoginFlow.bearerTokenFlow().getFirst();
        assertThat(first.component()).contains("BearerTokenAuthenticationFilter");
        assertThat(first.detail()).containsIgnoringCase("Bearer");
    }

    @Test
    void bearerTokenFlow_contains_jwt_validation_step() {
        var steps = Day107LoginFlow.bearerTokenFlow();
        boolean hasJwtDecoder = steps.stream()
                .anyMatch(s -> s.detail().contains("Signature check") || s.component().contains("JwtDecoder"));
        assertThat(hasJwtDecoder).isTrue();
    }
}
