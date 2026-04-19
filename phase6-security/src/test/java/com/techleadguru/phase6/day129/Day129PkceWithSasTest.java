package com.techleadguru.phase6.day129;

import com.techleadguru.phase6.day122.Day122PKCE;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class Day129PkceWithSasTest {

    private Day129PkceWithSas.AuthorizationRequest buildAuthReq(String clientId, String challenge) {
        // AuthorizationRequest(clientId, redirectUri, scope, state, codeChallenge, challengeMethod)
        return new Day129PkceWithSas.AuthorizationRequest(
                clientId, "https://app.example.com/cb",
                "openid profile", "state-xyz", challenge, "S256");
    }

    @Test
    void issueAuthCode_returns_non_blank_code() {
        Day129PkceWithSas.PkceFlowSimulator sim = new Day129PkceWithSas.PkceFlowSimulator();
        Day122PKCE.PkceParameters pkce = Day122PKCE.generate();

        Day129PkceWithSas.AuthorizationResponse resp = sim.issueAuthCode(buildAuthReq("client-x", pkce.codeChallenge()));
        assertThat(resp.code()).isNotBlank();
        assertThat(resp.state()).isNotNull();
    }

    @Test
    void issueAuthCode_increments_pending_count() {
        Day129PkceWithSas.PkceFlowSimulator sim = new Day129PkceWithSas.PkceFlowSimulator();
        Day122PKCE.PkceParameters pkce = Day122PKCE.generate();

        assertThat(sim.pendingCodeCount()).isEqualTo(0);
        sim.issueAuthCode(buildAuthReq("client-1", pkce.codeChallenge()));
        assertThat(sim.pendingCodeCount()).isEqualTo(1);
    }

    @Test
    void exchangeCode_success_with_correct_verifier() {
        Day129PkceWithSas.PkceFlowSimulator sim = new Day129PkceWithSas.PkceFlowSimulator();
        Day122PKCE.PkceParameters pkce = Day122PKCE.generate();

        Day129PkceWithSas.AuthorizationResponse authResp = sim.issueAuthCode(buildAuthReq("client-2", pkce.codeChallenge()));

        Day129PkceWithSas.TokenRequest tokenReq = new Day129PkceWithSas.TokenRequest(
                authResp.code(), pkce.codeVerifier(), "client-2", "https://app.example.com/cb");
        Day129PkceWithSas.TokenResponse tokenResp = sim.exchangeCode(tokenReq).orElseThrow();

        assertThat(tokenResp.accessToken()).isNotBlank();
        assertThat(tokenResp.tokenType()).isEqualToIgnoringCase("Bearer");
    }

    @Test
    void exchangeCode_returns_empty_with_wrong_verifier() {
        Day129PkceWithSas.PkceFlowSimulator sim = new Day129PkceWithSas.PkceFlowSimulator();
        Day122PKCE.PkceParameters pkce = Day122PKCE.generate();

        Day129PkceWithSas.AuthorizationResponse authResp = sim.issueAuthCode(buildAuthReq("client-3", pkce.codeChallenge()));

        Day129PkceWithSas.TokenRequest tokenReq = new Day129PkceWithSas.TokenRequest(
                authResp.code(), "wrong-verifier-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                "client-3", "https://app.example.com/cb");

        // Wrong verifier → empty Optional (code already consumed, no retry)
        assertThat(sim.exchangeCode(tokenReq)).isEmpty();
    }

    @Test
    void exchangeCode_is_single_use() {
        Day129PkceWithSas.PkceFlowSimulator sim = new Day129PkceWithSas.PkceFlowSimulator();
        Day122PKCE.PkceParameters pkce = Day122PKCE.generate();

        Day129PkceWithSas.AuthorizationResponse authResp = sim.issueAuthCode(buildAuthReq("client-4", pkce.codeChallenge()));

        Day129PkceWithSas.TokenRequest tokenReq = new Day129PkceWithSas.TokenRequest(
                authResp.code(), pkce.codeVerifier(), "client-4", "https://app.example.com/cb");
        sim.exchangeCode(tokenReq); // first use — succeeds

        // Second use must fail (code is gone)
        assertThat(sim.exchangeCode(tokenReq)).isEmpty();
    }

    @Test
    void exchangeCode_pending_count_decreases_after_exchange() {
        Day129PkceWithSas.PkceFlowSimulator sim = new Day129PkceWithSas.PkceFlowSimulator();
        Day122PKCE.PkceParameters pkce = Day122PKCE.generate();

        Day129PkceWithSas.AuthorizationResponse resp = sim.issueAuthCode(buildAuthReq("client-5", pkce.codeChallenge()));
        assertThat(sim.pendingCodeCount()).isEqualTo(1);

        Day129PkceWithSas.TokenRequest tokenReq = new Day129PkceWithSas.TokenRequest(
                resp.code(), pkce.codeVerifier(), "client-5", "https://app.example.com/cb");
        sim.exchangeCode(tokenReq);
        assertThat(sim.pendingCodeCount()).isEqualTo(0);
    }

    @Test
    void issueAuthCode_fails_when_challenge_is_missing() {
        Day129PkceWithSas.PkceFlowSimulator sim = new Day129PkceWithSas.PkceFlowSimulator();
        // codeChallenge is null
        Day129PkceWithSas.AuthorizationRequest req = new Day129PkceWithSas.AuthorizationRequest(
                "client-6", "https://app.example.com/cb",
                "openid", "state-x", null, "S256");

        assertThatThrownBy(() -> sim.issueAuthCode(req))
                .isInstanceOf(Exception.class);
    }
}

