package com.techleadguru.phase6.day121;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class Day121AuthorizationCodeFlowTest {

    @Test
    void authorizationCodeFlow_has_7_steps() {
        assertThat(Day121AuthorizationCodeFlow.authorizationCodeFlow()).hasSize(7);
    }

    @Test
    void flow_steps_cover_all_steps_enum_values() {
        var stepTypes = Day121AuthorizationCodeFlow.authorizationCodeFlow().stream()
                .map(Day121AuthorizationCodeFlow.FlowStepDetail::step)
                .toList();
        for (Day121AuthorizationCodeFlow.Step step : Day121AuthorizationCodeFlow.Step.values()) {
            assertThat(stepTypes).contains(step);
        }
    }

    @Test
    void each_flow_step_has_non_blank_fields() {
        Day121AuthorizationCodeFlow.authorizationCodeFlow().forEach(s -> {
            assertThat(s.actor()).isNotBlank();
            assertThat(s.httpMethod()).isNotBlank();
            assertThat(s.endpoint()).isNotBlank();
            assertThat(s.keyParameters()).isNotBlank();
        });
    }

    @Test
    void step_redirect_to_auth_server_has_code_param() {
        var step = Day121AuthorizationCodeFlow.authorizationCodeFlow().stream()
                .filter(s -> s.step() == Day121AuthorizationCodeFlow.Step.REDIRECT_TO_AUTH_SERVER)
                .findFirst();
        assertThat(step).isPresent();
        assertThat(step.get().keyParameters()).contains("response_type=code");
    }

    @Test
    void step_client_exchanges_code_uses_post() {
        var step = Day121AuthorizationCodeFlow.authorizationCodeFlow().stream()
                .filter(s -> s.step() == Day121AuthorizationCodeFlow.Step.CLIENT_EXCHANGES_CODE)
                .findFirst();
        assertThat(step).isPresent();
        assertThat(step.get().httpMethod()).isEqualTo("POST");
        assertThat(step.get().keyParameters()).contains("authorization_code");
    }

    @Test
    void generateState_returns_non_blank_random_string() {
        String state1 = Day121AuthorizationCodeFlow.generateState();
        String state2 = Day121AuthorizationCodeFlow.generateState();
        assertThat(state1).isNotBlank();
        assertThat(state2).isNotBlank();
        assertThat(state1).isNotEqualTo(state2);
    }

    @Test
    void validateState_true_when_matching() {
        String state = Day121AuthorizationCodeFlow.generateState();
        assertThat(Day121AuthorizationCodeFlow.validateState(state, state)).isTrue();
    }

    @Test
    void validateState_false_when_different() {
        assertThat(Day121AuthorizationCodeFlow.validateState("abc", "xyz")).isFalse();
    }

    @Test
    void validateState_false_when_null() {
        assertThat(Day121AuthorizationCodeFlow.validateState(null, "state")).isFalse();
        assertThat(Day121AuthorizationCodeFlow.validateState("state", null)).isFalse();
    }

    @Test
    void authorizationParams_includes_required_params() {
        Map<String, String> params = Day121AuthorizationCodeFlow.authorizationParams(
                "my-client", "http://localhost/callback", "openid profile", "abc123");
        assertThat(params).containsKey("response_type");
        assertThat(params.get("response_type")).isEqualTo("code");
        assertThat(params).containsKey("client_id");
        assertThat(params).containsKey("redirect_uri");
        assertThat(params).containsKey("scope");
        assertThat(params).containsKey("state");
    }
}
