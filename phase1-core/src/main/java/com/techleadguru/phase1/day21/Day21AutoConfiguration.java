package com.techleadguru.phase1.day21;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * DAY 21 — Auto-configuration: How Spring Boot Wires Itself
 *
 * THE CHAIN:
 *   @SpringBootApplication
 *     └── @EnableAutoConfiguration
 *           └── AutoConfigurationImportSelector
 *                 └── reads META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *                       └── loads all @AutoConfiguration classes
 *                             └── each uses @ConditionalOn* to decide whether to activate
 *
 * YOUR TASK TODAY:
 *   Write a custom AutoConfiguration class that wires an AuditLogger bean ONLY when:
 *   1. The property "app.audit.enabled=true" is present
 *   2. No other AuditLogger bean has been registered (user override support)
 *
 * PRODUCTION SCENARIO — Two security configs, unpredictable behaviour:
 *   Two libraries both auto-configure a SecurityFilterChain without @ConditionalOnMissingBean.
 *   Spring Boot 3.x throws: "expected a single SecurityFilterChain but found 2".
 *   In older versions it silently picked one — security rules were unpredictable.
 *   Certain endpoints that should be protected were open.
 *   FIX: Your SecurityFilterChain must always be @Order(1) or use @ConditionalOnMissingBean.
 *
 * HOW TO ACTIVATE THIS AUTO-CONFIGURATION:
 *   Create file: src/main/resources/META-INF/spring/
 *     org.springframework.boot.autoconfigure.AutoConfiguration.imports
 *   Add line:    com.techleadguru.phase1.day21.AuditAutoConfiguration
 *   Set property: app.audit.enabled=true in application.properties
 */
@Slf4j
public class Day21AutoConfiguration {
    // This class is a container for the auto-configuration components
}

// ===================================================================================
// Step 1: Configuration properties bound to "app.audit" prefix
// ===================================================================================

@ConfigurationProperties(prefix = "app.audit")
class AuditProperties {
    private boolean enabled = false;
    private String logLevel = "INFO";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
}

// ===================================================================================
// Step 2: The bean that the auto-configuration provides
// ===================================================================================

interface AuditLogger {
    void log(String action, String userId);
}

@Slf4j
class DefaultAuditLogger implements AuditLogger {

    private final AuditProperties properties;

    DefaultAuditLogger(AuditProperties properties) {
        this.properties = properties;
        log.info("[AutoConfig] DefaultAuditLogger created (level={})", properties.getLogLevel());
    }

    @Override
    public void log(String action, String userId) {
        log.atLevel(org.slf4j.event.Level.valueOf(properties.getLogLevel()))
                .log("[AUDIT] action={} userId={}", action, userId);
    }
}

// ===================================================================================
// Step 3: The @AutoConfiguration class — the entry point
// ===================================================================================

@AutoConfiguration
@ConditionalOnProperty(name = "app.audit.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(AuditProperties.class)
@Slf4j
class AuditAutoConfiguration {

    /**
     * @ConditionalOnMissingBean: If the user registers their OWN AuditLogger bean,
     * this default one is NOT created. User config always wins over auto-config.
     * THIS IS THE RULE all good auto-configurations follow.
     */
    @Bean
    @ConditionalOnMissingBean(AuditLogger.class)
    public AuditLogger auditLogger(AuditProperties properties) {
        log.info("[AutoConfig] app.audit.enabled=true -> activating DefaultAuditLogger");
        return new DefaultAuditLogger(properties);
    }
}
