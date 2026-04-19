package com.techleadguru.phase6.day127;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class Day127AuthorizationServerTest {

    @Test
    void defaultLocalClient_has_expected_fields() {
        Day127AuthorizationServer.ClientConfig client = Day127AuthorizationServer.defaultLocalClient();
        assertThat(client.clientId()).isNotBlank();
        assertThat(client.clientSecret()).isNotBlank();
        assertThat(client.grantTypes()).isNotEmpty();
        assertThat(client.scopes()).isNotEmpty();
        assertThat(client.redirectUris()).isNotEmpty();
    }

    @Test
    void serviceClient_uses_only_client_credentials() {
        Day127AuthorizationServer.ClientConfig service = Day127AuthorizationServer.serviceClient("my-svc");
        assertThat(service.grantTypes()).hasSize(1);
        assertThat(service.grantTypes().get(0).toLowerCase()).contains("client_credentials");
        assertThat(service.requirePkce()).isFalse();
    }

    @Test
    void authServerEndpoints_has_six_entries() {
        List<Day127AuthorizationServer.EndpointInfo> endpoints = Day127AuthorizationServer.authServerEndpoints();
        assertThat(endpoints).hasSize(6);
    }

    @Test
    void authServerEndpoints_includes_required_endpoints() {
        List<Day127AuthorizationServer.EndpointInfo> endpoints = Day127AuthorizationServer.authServerEndpoints();
        String allPaths = endpoints.stream()
                .map(Day127AuthorizationServer.EndpointInfo::path)
                .reduce("", (a, b) -> a + " " + b)
                .toLowerCase();
        assertThat(allPaths).contains("token");
        assertThat(allPaths).contains("authorize");
        assertThat(allPaths).contains("jwks");
    }

    @Test
    void authServerEndpoints_have_non_blank_fields() {
        Day127AuthorizationServer.authServerEndpoints().forEach(e -> {
            assertThat(e.name()).isNotBlank();
            assertThat(e.path()).isNotBlank();
            assertThat(e.description()).isNotBlank();
        });
    }

    @Test
    void setupGuide_has_seven_steps() {
        List<Day127AuthorizationServer.SetupStep> steps = Day127AuthorizationServer.setupGuide();
        assertThat(steps).hasSize(7);
        steps.forEach(s -> {
            assertThat(s.order()).isPositive();
            assertThat(s.title()).isNotBlank();
            assertThat(s.description()).isNotBlank();
        });
    }

    @Test
    void setupGuide_steps_are_ordered() {
        List<Day127AuthorizationServer.SetupStep> steps = Day127AuthorizationServer.setupGuide();
        for (int i = 0; i < steps.size(); i++) {
            assertThat(steps.get(i).order()).isEqualTo(i + 1);
        }
    }

    @Test
    void issuerProperties_contains_issuer_key() {
        Map<String, String> props = Day127AuthorizationServer.issuerProperties("https://auth.example.com");
        String allKeys = String.join(" ", props.keySet()).toLowerCase();
        assertThat(allKeys).contains("issuer");
    }
}
