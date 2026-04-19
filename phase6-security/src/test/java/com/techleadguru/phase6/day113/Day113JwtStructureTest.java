package com.techleadguru.phase6.day113;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class Day113JwtStructureTest {

    // A well-known test JWT (alg=HS256, not verified here)
    // Header: {"alg":"HS256","typ":"JWT"}
    // Payload: {"sub":"alice","iss":"test-issuer","iat":1700000000,"exp":9999999999}
    private static final String TEST_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
            ".eyJzdWIiOiJhbGljZSIsImlzcyI6InRlc3QtaXNzdWVyIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjk5OTk5OTk5OTl9" +
            ".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @Test
    void split_returns_three_parts() {
        Day113JwtStructure.JwtParts parts = Day113JwtStructure.split(TEST_JWT);
        assertThat(parts.headerB64()).isNotBlank();
        assertThat(parts.payloadB64()).isNotBlank();
        assertThat(parts.signatureB64()).isNotBlank();
    }

    @Test
    void split_throws_for_invalid_jwt() {
        assertThatThrownBy(() -> Day113JwtStructure.split("not.a.valid.jwt.parts"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decodeHeader_returns_hs256_algorithm() {
        Day113JwtStructure.JwtHeader header = Day113JwtStructure.decodeHeader(TEST_JWT);
        assertThat(header.alg()).isEqualTo("HS256");
        assertThat(header.typ()).isEqualTo("JWT");
    }

    @Test
    void decodePayload_extracts_subject() {
        Day113JwtStructure.JwtPayload payload = Day113JwtStructure.decodePayload(TEST_JWT);
        assertThat(payload.sub()).isEqualTo("alice");
    }

    @Test
    void decodePayload_extracts_issuer() {
        Day113JwtStructure.JwtPayload payload = Day113JwtStructure.decodePayload(TEST_JWT);
        assertThat(payload.iss()).isEqualTo("test-issuer");
    }

    @Test
    void decodePayload_extracts_iat_and_exp() {
        Day113JwtStructure.JwtPayload payload = Day113JwtStructure.decodePayload(TEST_JWT);
        assertThat(payload.iat()).isEqualTo(1700000000L);
        assertThat(payload.exp()).isEqualTo(9999999999L);
    }

    @Test
    void isExpired_returns_false_for_far_future_exp() {
        assertThat(Day113JwtStructure.isExpired(TEST_JWT)).isFalse();
    }

    @Test
    void isExpired_returns_true_for_expired_token() {
        // Build an unsigned JWT with exp in the past
        long pastExp = Instant.now().minusSeconds(3600).getEpochSecond();
        String expired = Day113JwtStructure.buildUnsignedJwt("bob", "issuer", pastExp);
        assertThat(Day113JwtStructure.isExpired(expired)).isTrue();
    }

    @Test
    void buildUnsignedJwt_produces_3_parts() {
        String jwt = Day113JwtStructure.buildUnsignedJwt("user", "issuer", 9999999999L);
        assertThat(jwt.split("\\.", -1)).hasSize(3);
    }

    @Test
    void buildUnsignedJwt_header_declares_alg_none() {
        String jwt    = Day113JwtStructure.buildUnsignedJwt("user", "issuer", 9999999999L);
        Day113JwtStructure.JwtHeader header = Day113JwtStructure.decodeHeader(jwt);
        assertThat(header.alg()).isEqualTo("none");
    }

    @Test
    void buildUnsignedJwt_payload_contains_subject() {
        String jwt     = Day113JwtStructure.buildUnsignedJwt("charlie", "my-issuer", 9999999999L);
        Day113JwtStructure.JwtPayload payload = Day113JwtStructure.decodePayload(jwt);
        assertThat(payload.sub()).isEqualTo("charlie");
        assertThat(payload.iss()).isEqualTo("my-issuer");
    }

    @Test
    void simpleJsonParse_parses_flat_json() {
        var map = Day113JwtStructure.simpleJsonParse(
                "{\"sub\":\"alice\",\"exp\":12345,\"active\":true}");
        assertThat(map.get("sub")).isEqualTo("alice");
        assertThat(map.get("exp")).isEqualTo(12345L);
        assertThat(map.get("active")).isEqualTo(Boolean.TRUE);
    }
}
