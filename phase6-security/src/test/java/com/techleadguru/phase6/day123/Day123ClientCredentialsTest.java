package com.techleadguru.phase6.day123;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

class Day123ClientCredentialsTest {

    private Day123ClientCredentials.ClientCredentialsFlowSimulator newSim() {
        return new Day123ClientCredentials.ClientCredentialsFlowSimulator(Duration.ofMinutes(15));
    }

    @Test
    void registerClient_increments_count() {
        Day123ClientCredentials.ClientCredentialsFlowSimulator sim = newSim();
        sim.registerClient("svc-a", "secret-a");
        assertThat(sim.registeredClientCount()).isEqualTo(1);
    }

    @Test
    void authenticate_returns_token_for_valid_credentials() {
        Day123ClientCredentials.ClientCredentialsFlowSimulator sim = newSim();
        sim.registerClient("svc-b", "pass-b");

        Optional<Day123ClientCredentials.ClientToken> token = sim.authenticate("svc-b", "pass-b", "write");
        assertThat(token).isPresent();
        assertThat(token.get().tokenType()).isEqualToIgnoringCase("Bearer");
        assertThat(token.get().accessToken()).isNotBlank();
        assertThat(token.get().scope()).isEqualTo("write");
    }

    @Test
    void authenticate_returns_empty_when_wrong_secret() {
        Day123ClientCredentials.ClientCredentialsFlowSimulator sim = newSim();
        sim.registerClient("svc-c", "correct");

        Optional<Day123ClientCredentials.ClientToken> result = sim.authenticate("svc-c", "wrong", "read");
        assertThat(result).isEmpty();
    }

    @Test
    void authenticate_returns_empty_for_unknown_client() {
        Day123ClientCredentials.ClientCredentialsFlowSimulator sim = newSim();

        Optional<Day123ClientCredentials.ClientToken> result = sim.authenticate("unknown", "any", "read");
        assertThat(result).isEmpty();
    }

    @Test
    void isTokenValid_true_for_recently_issued_token() {
        Day123ClientCredentials.ClientCredentialsFlowSimulator sim = newSim();
        sim.registerClient("svc-d", "sec");

        Day123ClientCredentials.ClientToken token = sim.authenticate("svc-d", "sec", "api").orElseThrow();
        assertThat(sim.isTokenValid(token.accessToken())).isTrue();
    }

    @Test
    void isTokenValid_false_for_random_token() {
        Day123ClientCredentials.ClientCredentialsFlowSimulator sim = newSim();
        assertThat(sim.isTokenValid("made-up-token")).isFalse();
    }

    @Test
    void clientToken_isExpired_false_right_after_issue() {
        Day123ClientCredentials.ClientCredentialsFlowSimulator sim = newSim();
        sim.registerClient("svc-e", "s");
        Day123ClientCredentials.ClientToken t = sim.authenticate("svc-e", "s", "r").orElseThrow();
        assertThat(t.isExpired(Instant.now())).isFalse();
    }

    @Test
    void clientToken_isExpired_true_when_past_expiry() {
        // expiresIn=0 means it expires immediately
        Day123ClientCredentials.ClientToken t = new Day123ClientCredentials.ClientToken(
                "tok", "Bearer", 0L, "read");
        // isExpired(issuedAt) — issued 10 seconds ago, expiresIn=0 → expired
        assertThat(t.isExpired(Instant.now().minusSeconds(10))).isTrue();
    }

    @Test
    void springProperties_contains_grant_type() {
        Map<String, String> props = Day123ClientCredentials.springProperties(
                "my-svc", "client-id", "https://auth.example.com/oauth2/token", "api.read");
        assertThat(props).isNotEmpty();
        String allValues = String.join(" ", props.values());
        assertThat(allValues.toLowerCase()).contains("client_credentials");
    }
}
