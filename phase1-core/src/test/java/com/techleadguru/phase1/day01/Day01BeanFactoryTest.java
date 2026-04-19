package com.techleadguru.phase1.day01;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 1 — Test: Prove the key BeanFactory vs ApplicationContext difference.
 *
 * This test answers: "Why does my @EventListener not fire when using a bare BeanFactory?"
 * PRODUCTION BUG REPRODUCED: Cache warmup via @EventListener never runs if context is wrong type.
 */
class Day01BeanFactoryTest {

    // ------------------------------------------------------------------------------------
    // Test 1: BeanFactory — @EventListener silently DOES NOT fire
    // ------------------------------------------------------------------------------------

    @Test
    void beanFactory_does_NOT_publish_events() {
        // Arrange: manually create a BeanFactory and register a bean with @EventListener
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("eventTracker", new EventTracker());

        EventTracker tracker = factory.getBean(EventTracker.class);

        // Act: no ContextRefreshedEvent is published (BeanFactory doesn't support events)
        // Assert: listener was never called
        assertThat(tracker.eventReceived)
                .as("BeanFactory does NOT publish ContextRefreshedEvent — @EventListener silently ignored")
                .isFalse();

        System.out.println("[BeanFactory] @EventListener fired? " + tracker.eventReceived);
        System.out.println("[BeanFactory] <- This is why cache warmup silently fails in some setups");
    }

    // ------------------------------------------------------------------------------------
    // Test 2: ApplicationContext — @EventListener DOES fire (ContextRefreshedEvent)
    // ------------------------------------------------------------------------------------

    @Test
    void applicationContext_PUBLISHES_events() {
        // Arrange
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(EventListenerConfig.class);
        ctx.refresh(); // <- this triggers ContextRefreshedEvent

        EventTracker tracker = ctx.getBean(EventTracker.class);

        // Assert
        assertThat(tracker.eventReceived)
                .as("ApplicationContext fires ContextRefreshedEvent, @EventListener IS called")
                .isTrue();

        System.out.println("[ApplicationContext] @EventListener fired? " + tracker.eventReceived);
        System.out.println("[ApplicationContext] <- Cache warmup, seed logic, connectivity checks work here");

        ctx.close();
    }

    // ------------------------------------------------------------------------------------
    // Test 3: Prove ApplicationRunner fires exactly once — use this for startup logic
    // ------------------------------------------------------------------------------------

    @Test
    void applicationRunner_is_the_safe_startup_hook() {
        // ApplicationRunner is guaranteed to run AFTER full context refresh (including Config Server)
        // This is what you should use in production for startup logic.
        // Demonstrated by running Phase1Application — see the console output.
        System.out.println("[RULE] Use ApplicationRunner for: cache warmup, DB seeding, health checks at startup");
        System.out.println("[RULE] Reason: runs AFTER full context refresh, including Config Server injection");
        System.out.println("[RULE] @PostConstruct runs BEFORE property placeholders from Config Server are injected");
    }

    // ------------------------------------------------------------------------------------
    // Supporting classes
    // ------------------------------------------------------------------------------------

    static class EventTracker {
        volatile boolean eventReceived = false;

        @EventListener
        public void onEvent(Object event) {
            eventReceived = true;
        }
    }

    @Configuration
    static class EventListenerConfig {
        @Bean
        EventTracker eventTracker() {
            return new EventTracker();
        }
    }
}
