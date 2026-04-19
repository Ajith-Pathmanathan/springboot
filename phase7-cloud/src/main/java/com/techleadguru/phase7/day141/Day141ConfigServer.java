package com.techleadguru.phase7.day141;

import java.util.*;

/**
 * Day 141 — Spring Cloud Config Server
 *
 * Config Server externalises configuration from Git/Filesystem/Vault/JDBC.
 * Clients bootstrap by fetching their config before the application context is
 * fully started (Spring Cloud 2020+ uses spring.config.import instead of
 * bootstrap.yml).
 *
 * URL pattern: {serverUri}/{application}/{profile}/{label}
 */
public class Day141ConfigServer {

    // ─────────────────────────────────────────────────────────────────────────
    // Config source types
    // ─────────────────────────────────────────────────────────────────────────

    public enum ConfigSource { GIT, FILE_SYSTEM, VAULT, JDBC }

    public record ConfigSourceInfo(
            ConfigSource source,
            String       serverAnnotation,
            String       uriProperty,
            String       example) {}

    public static List<ConfigSourceInfo> configSources() {
        return List.of(
            new ConfigSourceInfo(ConfigSource.GIT,
                "@EnableConfigServer",
                "spring.cloud.config.server.git.uri",
                "https://github.com/myorg/config-repo"),
            new ConfigSourceInfo(ConfigSource.FILE_SYSTEM,
                "@EnableConfigServer",
                "spring.cloud.config.server.native.search-locations",
                "classpath:/config"),
            new ConfigSourceInfo(ConfigSource.VAULT,
                "@EnableConfigServer",
                "spring.cloud.config.server.vault.host",
                "vault.example.com"),
            new ConfigSourceInfo(ConfigSource.JDBC,
                "@EnableConfigServer",
                "spring.datasource.url",
                "jdbc:postgresql://localhost/configdb")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Server properties (application.yml / properties on the server itself)
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> serverProperties(String gitUri) {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("spring.cloud.config.server.git.uri",             gitUri);
        props.put("spring.cloud.config.server.git.default-label",   "main");
        props.put("spring.cloud.config.server.git.clone-on-start",  "true");
        props.put("spring.cloud.config.server.git.search-paths",    "{application}");
        props.put("server.port",                                     "8888");
        return props;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Client bootstrap properties (spring.config.import style — Spring Cloud 2020+)
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> clientBootstrapProperties(
            String serverUri, String appName, String profile) {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("spring.application.name",         appName);
        props.put("spring.profiles.active",          profile);
        props.put("spring.config.import",            "optional:configserver:" + serverUri);
        props.put("spring.cloud.config.uri",         serverUri);
        props.put("spring.cloud.config.fail-fast",   "true");
        return props;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Config-refresh trigger properties
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> refreshProperties() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("management.endpoints.web.exposure.include",   "refresh,health,info");
        props.put("management.endpoint.refresh.enabled",         "true");
        return props;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Encryption
    // ─────────────────────────────────────────────────────────────────────────

    public record EncryptionInfo(
            String keyProperty,
            String encryptEndpoint,
            String decryptEndpoint,
            String encryptedValuePrefix) {}

    public static EncryptionInfo encryptionInfo() {
        return new EncryptionInfo(
            "encrypt.key",
            "/encrypt",
            "/decrypt",
            "{cipher}"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup checklist
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> setupSteps() {
        return List.of(
            "1. Add spring-cloud-config-server dependency to server module",
            "2. Annotate @SpringBootApplication class with @EnableConfigServer",
            "3. Configure spring.cloud.config.server.git.uri in application.properties",
            "4. Add spring-cloud-starter-config to each client module",
            "5. Set spring.config.import=optional:configserver:http://localhost:8888 in client",
            "6. Name client with spring.application.name matching file in Git repo",
            "7. Expose refresh actuator endpoint; POST /actuator/refresh to reload"
        );
    }
}
