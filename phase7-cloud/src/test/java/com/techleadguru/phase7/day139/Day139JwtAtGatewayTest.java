package com.techleadguru.phase7.day139;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day139JwtAtGatewayTest {

    private static final String ISSUER   = "myapp";
    private static final String AUDIENCE = "api";

    private Day139JwtAtGateway.GatewayJwtValidator validator() {
        return new Day139JwtAtGateway.GatewayJwtValidator(ISSUER, AUDIENCE);
    }

    private String token(String sub, long expMs) {
        return "sub=" + sub + ";iss=" + ISSUER + ";aud=" + AUDIENCE
                + ";exp=" + expMs + ";roles=ROLE_USER";
    }

    @Test
    void testValidToken() {
        long future = System.currentTimeMillis() + 60_000;
        Day139JwtAtGateway.TokenValidationResult result =
                validator().validate("Bearer " + token("alice", future), System.currentTimeMillis());
        assertTrue(result.valid());
        assertEquals("alice", result.subject());
        assertTrue(result.roles().contains("ROLE_USER"));
    }

    @Test
    void testExpiredToken() {
        long past = System.currentTimeMillis() - 60_000;
        Day139JwtAtGateway.TokenValidationResult result =
                validator().validate("Bearer " + token("alice", past), System.currentTimeMillis());
        assertFalse(result.valid());
        assertNotNull(result.reason());
        assertTrue(result.reason().toLowerCase().contains("expir"));
    }

    @Test
    void testMissingAuthorizationHeader() {
        Day139JwtAtGateway.TokenValidationResult result =
                validator().validate(null, System.currentTimeMillis());
        assertFalse(result.valid());
    }

    @Test
    void testWrongIssuer() {
        long future = System.currentTimeMillis() + 60_000;
        String badToken = "sub=alice;iss=badapp;aud=" + AUDIENCE + ";exp=" + future;
        Day139JwtAtGateway.TokenValidationResult result =
                validator().validate("Bearer " + badToken, System.currentTimeMillis());
        assertFalse(result.valid());
    }

    @Test
    void testWrongAudience() {
        long future = System.currentTimeMillis() + 60_000;
        String badToken = "sub=alice;iss=" + ISSUER + ";aud=other;exp=" + future;
        Day139JwtAtGateway.TokenValidationResult result =
                validator().validate("Bearer " + badToken, System.currentTimeMillis());
        assertFalse(result.valid());
    }

    @Test
    void testMissingBearerPrefix() {
        Day139JwtAtGateway.TokenValidationResult result =
                validator().validate("Basic abc123", System.currentTimeMillis());
        assertFalse(result.valid());
    }

    @Test
    void testSampleRoutePolicies() {
        Map<String, Day139JwtAtGateway.RouteAuthPolicy> policies =
                Day139JwtAtGateway.sampleRoutePolicies();
        assertEquals(Day139JwtAtGateway.AuthPolicy.PUBLIC,
                policies.get("health").policy());
        assertEquals(Day139JwtAtGateway.AuthPolicy.ROLE_REQUIRED,
                policies.get("admin").policy());
        assertEquals("ROLE_ADMIN", policies.get("admin").requiredRole());
    }

    @Test
    void testValidationSteps() {
        List<Day139JwtAtGateway.JwtValidationStep> steps =
                Day139JwtAtGateway.validationSteps();
        assertEquals(10, steps.size());
        assertEquals(1, steps.get(0).order());
    }
}
