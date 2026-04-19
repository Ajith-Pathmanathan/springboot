package com.techleadguru.phase1.day02;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 2 — Test: Reproduce and fix the NoUniqueBeanDefinitionException production incident.
 *
 * PRODUCTION INCIDENT REPRODUCED:
 *   Library adds its own @Component NotificationService.
 *   Your app also has one. Deploy fails in staging. Here is the exact error.
 */
class Day02BeanRegistrationTest {

    // ------------------------------------------------------------------------------------
    // Test 1: REPRODUCE the production incident — two @Component beans of same type
    // ------------------------------------------------------------------------------------

    @Test
    void reproduces_NoUniqueBeanDefinitionException_when_two_components_of_same_type() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(AppNotificationService.class, LibraryNotificationService.class, Consumer.class);

        // This DOES NOT throw on registration — only throws when Spring tries to autowire
        ctx.refresh();

        // Trying to get bean by type: FAILS because 2 beans match
        assertThatThrownBy(() -> ctx.getBean(Day02BeanRegistration.NotificationService.class))
                .isInstanceOf(NoUniqueBeanDefinitionException.class)
                .hasMessageContaining("expected single matching bean but found 2");

        System.out.println("[DAY 2 INCIDENT] NoUniqueBeanDefinitionException reproduced!");
        System.out.println("[DAY 2 INCIDENT] This is what breaks your production deploy after a library upgrade.");

        ctx.close();
    }

    // ------------------------------------------------------------------------------------
    // Test 2: FIX — use @Primary to designate which bean wins
    // ------------------------------------------------------------------------------------

    @Test
    void fix_with_primary_resolves_ambiguity() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(PrimaryNotificationConfig.class);
        ctx.refresh();

        Day02BeanRegistration.NotificationService svc =
                ctx.getBean(Day02BeanRegistration.NotificationService.class);

        assertThat(svc.send("test")).startsWith("EMAIL");
        System.out.println("[DAY 2 FIX] @Primary resolves to: " + svc.send("test"));

        ctx.close();
    }

    // ------------------------------------------------------------------------------------
    // Supporting classes simulating "your code" vs "library code"
    // ------------------------------------------------------------------------------------

    @Component
    static class AppNotificationService implements Day02BeanRegistration.NotificationService {
        public String send(String msg) { return "APP-EMAIL: " + msg; }
    }

    @Component
    static class LibraryNotificationService implements Day02BeanRegistration.NotificationService {
        public String send(String msg) { return "LIBRARY-SMS: " + msg; }
    }

    static class Consumer {
        // Consumer would have: @Autowired NotificationService notificationService;
        // This is what fails to start when two beans of the same type exist
    }

    @Configuration
    static class PrimaryNotificationConfig {
        @Bean
        @org.springframework.context.annotation.Primary
        public Day02BeanRegistration.NotificationService emailService() {
            return msg -> "EMAIL: " + msg;
        }

        @Bean
        public Day02BeanRegistration.NotificationService smsService() {
            return msg -> "SMS: " + msg;
        }
    }
}
