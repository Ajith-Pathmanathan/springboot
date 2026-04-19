package com.techleadguru.phase7.day141;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day141ConfigServerTest {

    @Test
    void testConfigSourcesCount() {
        List<Day141ConfigServer.ConfigSourceInfo> sources = Day141ConfigServer.configSources();
        assertEquals(4, sources.size());
    }

    @Test
    void testServerProperties() {
        Map<String, String> props = Day141ConfigServer.serverProperties(
                "https://github.com/myorg/config-repo");
        assertTrue(props.containsKey("spring.cloud.config.server.git.uri"));
        assertEquals("https://github.com/myorg/config-repo",
                props.get("spring.cloud.config.server.git.uri"));
        assertEquals("8888", props.get("server.port"));
    }

    @Test
    void testClientBootstrapProperties() {
        Map<String, String> props = Day141ConfigServer.clientBootstrapProperties(
                "http://config-server:8888", "order-service", "prod");
        assertEquals("order-service", props.get("spring.application.name"));
        assertEquals("prod",          props.get("spring.profiles.active"));
        assertTrue(props.get("spring.config.import").contains("configserver:"));
    }

    @Test
    void testRefreshProperties() {
        Map<String, String> props = Day141ConfigServer.refreshProperties();
        assertTrue(props.get("management.endpoints.web.exposure.include").contains("refresh"));
    }

    @Test
    void testEncryptionInfo() {
        Day141ConfigServer.EncryptionInfo info = Day141ConfigServer.encryptionInfo();
        assertEquals("{cipher}", info.encryptedValuePrefix());
        assertEquals("/encrypt", info.encryptEndpoint());
    }

    @Test
    void testSetupSteps() {
        List<String> steps = Day141ConfigServer.setupSteps();
        assertFalse(steps.isEmpty());
        assertTrue(steps.get(0).contains("spring-cloud-config-server"));
    }
}
