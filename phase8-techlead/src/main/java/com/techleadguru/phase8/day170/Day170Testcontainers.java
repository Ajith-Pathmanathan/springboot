package com.techleadguru.phase8.day170;

import java.util.*;

/**
 * Day 170 — Testcontainers: Real PostgreSQL + Kafka in CI
 *
 * Testcontainers starts real Docker containers in tests, giving you
 * production-identical databases and message brokers.
 *
 * Two lifecycle strategies:
 *  Per-test:  @Container on non-static field — fresh container per test class
 *  Shared:    @Container on static field — one container for entire test suite
 */
public class Day170Testcontainers {

    // ─────────────────────────────────────────────────────────────────────────
    // Container configuration descriptor
    // ─────────────────────────────────────────────────────────────────────────

    public record ContainerConfig(
            String              image,
            int                 exposedPort,
            Map<String, String> envVars,
            List<String>        waitStrategy) {}

    public static ContainerConfig postgresConfig() {
        return new ContainerConfig(
            "postgres:16-alpine",
            5432,
            Map.of("POSTGRES_DB",       "testdb",
                   "POSTGRES_USER",     "test",
                   "POSTGRES_PASSWORD", "test"),
            List.of("Wait for port 5432", "Wait for log: database system is ready")
        );
    }

    public static ContainerConfig kafkaConfig() {
        return new ContainerConfig(
            "confluentinc/cp-kafka:7.5.0",
            9092,
            Map.of("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:9092"),
            List.of("Wait for port 9092", "Wait for Kafka readiness log")
        );
    }

    public static ContainerConfig redisConfig() {
        return new ContainerConfig(
            "redis:7-alpine",
            6379,
            Map.of(),
            List.of("Wait for port 6379", "Wait for log: Ready to accept connections")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Container types guide
    // ─────────────────────────────────────────────────────────────────────────

    public record ContainerType(
            String name,
            String testcontainersClass,
            String springBoot3Support) {}

    public static List<ContainerType> containerTypes() {
        return List.of(
            new ContainerType("PostgreSQL",
                "PostgreSQLContainer",
                "@ServiceConnection auto-wires DataSource (Spring Boot 3.1+)"),
            new ContainerType("Kafka",
                "KafkaContainer",
                "Use @DynamicPropertySource to override spring.kafka.bootstrap-servers"),
            new ContainerType("Redis",
                "GenericContainer (redis:7-alpine)",
                "Use @DynamicPropertySource to override spring.data.redis.host/port"),
            new ContainerType("LocalStack (AWS)",
                "LocalStackContainer",
                "Configure endpoint override for S3/SQS/SNS via @DynamicPropertySource"),
            new ContainerType("MySQL",
                "MySQLContainer",
                "@ServiceConnection auto-wires DataSource"),
            new ContainerType("MongoDB",
                "MongoDBContainer",
                "@ServiceConnection auto-wires MongoClient (Spring Boot 3.1+)")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle guide
    // ─────────────────────────────────────────────────────────────────────────

    public record LifecycleInfo(
            String mode,
            String annotation,
            String when,
            String tradeoff) {}

    public static List<LifecycleInfo> lifecycleGuide() {
        return List.of(
            new LifecycleInfo(
                "Per test class",
                "@Container (non-static field)",
                "New container per test class; started before all tests in class",
                "Isolated state; slower (start cost per class)"),
            new LifecycleInfo(
                "Shared across suite",
                "@Container (static field) + Ryuk cleanup",
                "Single container for all tests; faster startup",
                "Tests must not leave state; use @BeforeEach to clean tables"),
            new LifecycleInfo(
                "Singleton pattern",
                "Abstract base class with static container + @DynamicPropertySource",
                "One container for all subclassing test classes in JVM",
                "Fastest; requires careful state management"),
            new LifecycleInfo(
                "Spring Boot 3.1 @ServiceConnection",
                "@ServiceConnection on @Bean or @Container field",
                "Auto-registers container properties; zero manual DynamicPropertySource",
                "Requires Spring Boot 3.1+ and matching Testcontainers module")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DynamicPropertySource pattern
    // ─────────────────────────────────────────────────────────────────────────

    public record PropertyOverride(String springProperty, String containerExpression) {}

    public static List<PropertyOverride> propertyOverridePattern() {
        return List.of(
            new PropertyOverride(
                "spring.datasource.url",
                "postgres.getJdbcUrl()"),
            new PropertyOverride(
                "spring.datasource.username",
                "postgres.getUsername()"),
            new PropertyOverride(
                "spring.datasource.password",
                "postgres.getPassword()"),
            new PropertyOverride(
                "spring.kafka.bootstrap-servers",
                "kafka.getBootstrapServers()"),
            new PropertyOverride(
                "spring.data.redis.host",
                "redis.getHost()"),
            new PropertyOverride(
                "spring.data.redis.port",
                "String.valueOf(redis.getMappedPort(6379))")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup steps
    // ─────────────────────────────────────────────────────────────────────────

    public record SetupStep(int order, String action) {}

    public static List<SetupStep> setupSteps() {
        return List.of(
            new SetupStep(1, "Add testcontainers-bom to dependencyManagement"),
            new SetupStep(2, "Add testcontainers:junit-jupiter, :postgresql, :kafka to test scope"),
            new SetupStep(3, "Annotate test class with @Testcontainers"),
            new SetupStep(4, "Declare @Container static PostgreSQLContainer / KafkaContainer"),
            new SetupStep(5, "Use @DynamicPropertySource or @ServiceConnection to wire properties"),
            new SetupStep(6, "Run @SpringBootTest(classes=...) or @DataJpaTest with @AutoConfigureTestDatabase(replace=NONE)")
        );
    }
}
