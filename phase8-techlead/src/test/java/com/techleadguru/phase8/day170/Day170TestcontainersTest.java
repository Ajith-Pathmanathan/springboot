package com.techleadguru.phase8.day170;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Day170TestcontainersTest {

    @Test
    void postgresConfigHasCorrectImageAndPort() {
        var config = Day170Testcontainers.postgresConfig();
        assertEquals("postgres:16-alpine", config.image());
        assertEquals(5432,                  config.exposedPort());
        assertTrue(config.envVars().containsKey("POSTGRES_DB"));
    }

    @Test
    void kafkaConfigHasCorrectImageAndPort() {
        var config = Day170Testcontainers.kafkaConfig();
        assertTrue(config.image().contains("kafka"));
        assertEquals(9092, config.exposedPort());
    }

    @Test
    void redisConfigHasCorrectImageAndPort() {
        var config = Day170Testcontainers.redisConfig();
        assertTrue(config.image().contains("redis"));
        assertEquals(6379, config.exposedPort());
    }

    @Test
    void containerTypesListIncludesPostgresAndKafka() {
        var types = Day170Testcontainers.containerTypes();
        assertFalse(types.isEmpty());
        boolean hasPostgres = types.stream().anyMatch(t -> t.name().equals("PostgreSQL"));
        boolean hasKafka    = types.stream().anyMatch(t -> t.name().equals("Kafka"));
        assertTrue(hasPostgres);
        assertTrue(hasKafka);
    }

    @Test
    void lifecycleGuideContainsAllModes() {
        var guide = Day170Testcontainers.lifecycleGuide();
        assertEquals(4, guide.size());
    }

    @Test
    void propertyOverridePatternIncludesDataSourceUrl() {
        var overrides = Day170Testcontainers.propertyOverridePattern();
        boolean hasUrl = overrides.stream()
                .anyMatch(o -> o.springProperty().equals("spring.datasource.url"));
        assertTrue(hasUrl);
    }

    @Test
    void setupStepsAreSix() {
        assertEquals(6, Day170Testcontainers.setupSteps().size());
    }
}
