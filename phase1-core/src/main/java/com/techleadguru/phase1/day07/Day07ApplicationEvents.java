package com.techleadguru.phase1.day07;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * DAY 7 — ApplicationContext Events + Custom Domain Events
 *
 * SPRING BUILT-IN EVENTS:
 *   ContextRefreshedEvent   — fired when context is refreshed/re-loaded (can fire TWICE in MVC!)
 *   ContextStartedEvent     — rarely used
 *   ContextStoppedEvent     — rarely used
 *   ContextClosedEvent      — fired on shutdown (use for cleanup)
 *   ApplicationReadyEvent   — fired once after full startup (safest for startup logic!)
 *
 * CUSTOM EVENTS:
 *   Extend ApplicationEvent (old way) or use any POJO (Spring 4.2+)
 *   Published via ApplicationEventPublisher.publishEvent()
 *
 * PRODUCTION SCENARIO — The double-fire disaster:
 *   @EventListener(ContextRefreshedEvent.class) sends a Slack alert + seeds initial DB data.
 *   In a Spring MVC app with parent+child contexts, it fires TWICE.
 *   DB gets a duplicate unique constraint violation. Slack gets 2 messages. Support gets paged.
 *   FIX: Use ApplicationReadyEvent (fires exactly once) or guard with parent-context check.
 *
 * RULE: For startup logic that must run exactly once: use ApplicationReadyEvent or ApplicationRunner.
 */
@Slf4j
public class Day07ApplicationEvents {

    // ===================================================================================
    // Custom Domain Event — POJO (no need to extend ApplicationEvent since Spring 4.2)
    // ===================================================================================

    public record UserRegisteredEvent(String userId, String email, long timestamp) {
        public UserRegisteredEvent(String userId, String email) {
            this(userId, email, System.currentTimeMillis());
        }
    }

    public record OrderPlacedEvent(String orderId, String userId, double totalAmount) {}

    // ===================================================================================
    // Service that publishes events — decoupled from listeners
    // ===================================================================================

    @Component
    @Slf4j
    public static class UserService {

        private final ApplicationEventPublisher publisher;

        public UserService(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        public void registerUser(String userId, String email) {
            log.info("[UserService] Registering user: {}", userId);
            // Business logic here...
            // Publish event — UserService does NOT know who listens. Zero coupling.
            publisher.publishEvent(new UserRegisteredEvent(userId, email));
            log.info("[UserService] Event published for user: {}", userId);
        }
    }

    // ===================================================================================
    // Listeners — each does one thing, no coupling to UserService
    // ===================================================================================

    @Component
    @Slf4j
    public static class EmailNotificationListener {
        @EventListener
        public void onUserRegistered(UserRegisteredEvent event) {
            log.info("[EMAIL LISTENER] Sending welcome email to: {}", event.email());
        }
    }

    @Component
    @Slf4j
    public static class AuditListener {
        @EventListener
        public void onUserRegistered(UserRegisteredEvent event) {
            log.info("[AUDIT LISTENER] Audit: user {} registered at {}", event.userId(), event.timestamp());
        }
    }

    // ===================================================================================
    // THE BUG: ContextRefreshedEvent fires TWICE in MVC (parent + child context)
    // ===================================================================================

    @Component
    @Slf4j
    public static class DangerousStartupListener {

        private int fireCount = 0;

        /**
         * DANGEROUS in a Spring MVC app with parent+child contexts.
         * This fires once for the root context and once for the servlet context.
         * Use ApplicationReadyEvent instead.
         */
        @EventListener(ContextRefreshedEvent.class)
        public void onContextRefreshed(ContextRefreshedEvent event) {
            fireCount++;
            log.warn("[DANGEROUS] ContextRefreshedEvent fired! Count={} | Context={}",
                    fireCount, event.getApplicationContext().getDisplayName());
            if (fireCount > 1) {
                log.error("[DANGEROUS] Fired {} times! Would cause duplicate data/Slack notifications!", fireCount);
            }
        }
    }

    @Component
    @Slf4j
    public static class SafeStartupListener {

        /**
         * SAFE: ApplicationReadyEvent fires exactly once after full application startup.
         * Use this for: cache warmup, DB seeding, sending "system started" notifications.
         */
        @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
        public void onApplicationReady() {
            log.info("[SAFE] ApplicationReadyEvent fired — exactly once — safe for startup logic!");
        }
    }

    // ===================================================================================
    // Demo runner: publish events and observe the listener chain
    // ===================================================================================

    @Configuration
    static class Day07Config {
        @Bean
        public ApplicationRunner day07Runner(UserService userService) {
            return args -> {
                System.out.println("=== DAY 7: Custom Domain Events ===");
                userService.registerUser("U-001", "alice@example.com");
                userService.registerUser("U-002", "bob@example.com");
                System.out.println("===================================");
                System.out.println("RULE: For startup side effects, use ApplicationReadyEvent.");
                System.out.println("RULE: ContextRefreshedEvent fires N times (once per context in MVC).");
            };
        }
    }
}
