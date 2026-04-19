package com.techleadguru.phase6.day132;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class Day132TokenExchangeTest {

    private Day132TokenExchange.TokenExchangeRequest delegationRequest() {
        return new Day132TokenExchange.TokenExchangeRequest(
                "subject-token-abc",
                "actor-token-xyz",
                Day132TokenExchange.ExchangeType.DELEGATION,
                "openid profile"
        );
    }

    private Day132TokenExchange.TokenExchangeRequest impersonationRequest() {
        return new Day132TokenExchange.TokenExchangeRequest(
                "subject-token-abc",
                "actor-token-xyz",
                Day132TokenExchange.ExchangeType.IMPERSONATION,
                "openid"
        );
    }

    @Test
    void rfc8693Parameters_has_eight_params() {
        List<Day132TokenExchange.TokenExchangeParameter> params = Day132TokenExchange.rfc8693Parameters();
        assertThat(params).hasSize(8);
    }

    @Test
    void rfc8693Parameters_fields_are_non_blank() {
        Day132TokenExchange.rfc8693Parameters().forEach(p -> {
            assertThat(p.name()).isNotBlank();
            assertThat(p.description()).isNotBlank();
        });
    }

    @Test
    void requiredParameterCount_is_three() {
        assertThat(Day132TokenExchange.requiredParameterCount()).isEqualTo(3);
    }

    @Test
    void isValidForExchange_true_for_valid_request() {
        Day132TokenExchange.TokenExchangeService svc = new Day132TokenExchange.TokenExchangeService();
        assertThat(svc.isValidForExchange(delegationRequest().subjectToken())).isTrue();
    }

    @Test
    void isValidForExchange_false_when_subject_token_blank() {
        Day132TokenExchange.TokenExchangeService svc = new Day132TokenExchange.TokenExchangeService();
        assertThat(svc.isValidForExchange("")).isFalse();
    }

    @Test
    void exchange_delegation_keeps_original_subject() {
        Day132TokenExchange.TokenExchangeService svc = new Day132TokenExchange.TokenExchangeService();
        Day132TokenExchange.ExchangedToken result = svc.exchange(delegationRequest());

        assertThat(result).isNotNull();
        assertThat(result.token()).isNotBlank();
        // For DELEGATION, subject comes from subjectToken
        assertThat(result.subject()).isNotBlank();
        assertThat(result.type()).isEqualTo(Day132TokenExchange.ExchangeType.DELEGATION);
    }

    @Test
    void exchange_impersonation_actor_becomes_subject() {
        Day132TokenExchange.TokenExchangeService svc = new Day132TokenExchange.TokenExchangeService();
        Day132TokenExchange.ExchangedToken result = svc.exchange(impersonationRequest());

        assertThat(result).isNotNull();
        // For IMPERSONATION, actor replaces the subject
        assertThat(result.subject()).isNotBlank();
        assertThat(result.type()).isEqualTo(Day132TokenExchange.ExchangeType.IMPERSONATION);
        // actor field should be null for impersonation (actor became subject)
        assertThat(result.actor()).isNull();
    }

    @Test
    void exchangeType_enum_has_delegation_and_impersonation() {
        Day132TokenExchange.ExchangeType[] values = Day132TokenExchange.ExchangeType.values();
        assertThat(values).contains(
                Day132TokenExchange.ExchangeType.DELEGATION,
                Day132TokenExchange.ExchangeType.IMPERSONATION
        );
    }

    @Test
    void exchange_delegation_sets_actor_field() {
        Day132TokenExchange.TokenExchangeService svc = new Day132TokenExchange.TokenExchangeService();
        Day132TokenExchange.ExchangedToken result = svc.exchange(delegationRequest());
        // For delegation, actor should be set (the requesting service)
        assertThat(result.actor()).isNotNull();
    }

    @Test
    void exchange_throws_for_blank_subject_token() {
        Day132TokenExchange.TokenExchangeRequest invalid = new Day132TokenExchange.TokenExchangeRequest(
                "", null, Day132TokenExchange.ExchangeType.DELEGATION, "openid");
        Day132TokenExchange.TokenExchangeService svc = new Day132TokenExchange.TokenExchangeService();
        // The exchange should throw because subjectToken is too short
        assertThatThrownBy(() -> svc.exchange(invalid))
                .isInstanceOf(Exception.class);
    }
}
