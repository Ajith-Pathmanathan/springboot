package com.techleadguru.phase1.day02;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * DAY 2 — Bean Registration: @Component vs @Bean vs XML
 *
 * THREE WAYS TO REGISTER A BEAN:
 *
 *   1. @Component (+ @Service, @Repository, @Controller)
 *      = Class-level. Spring scans and registers. Simple. Used for YOUR OWN classes.
 *      Risk: if two @Component classes of same type exist (your code + library), NoUniqueBeanDefinitionException.
 *
 *   2. @Bean inside @Configuration
 *      = Method-level. Full control over instantiation. Used for THIRD-PARTY classes you can't annotate.
 *      Safe: YOU decide what gets registered.
 *
 *   3. XML (legacy — exists in old codebases, understand it for maintenance)
 *
 * PRODUCTION SCENARIO BAKED IN:
 *   Library v2.0 adds its own @Component NotificationService.
 *   Your app also has @Component NotificationService.
 *   Deploy fails: NoUniqueBeanDefinitionException.
 *   FIX: Use @Bean in @Configuration for infrastructure beans you own. Demonstrated in test.
 */
public class Day02BeanRegistration {

    // ------------------------------------------------------------------------------------
    // Method 1: @Component — Spring component scan picks it up automatically
    // ------------------------------------------------------------------------------------

    /**
     * This interface is the contract.
     * Multiple implementations will cause the "NoUniqueBeanDefinitionException" in production.
     */
    public interface NotificationService {
        String send(String message);
    }

    @Component("emailNotification") // explicit name prevents name collision
    public static class EmailNotificationService implements NotificationService {
        @Override
        public String send(String message) {
            return "EMAIL: " + message;
        }
    }

    // ------------------------------------------------------------------------------------
    // Method 2: @Bean in @Configuration — full control, safe for 3rd-party classes
    // ------------------------------------------------------------------------------------

    @Configuration
    public static class NotificationConfig {

        /**
         * Registering SmsNotificationService via @Bean.
         * USE THIS PATTERN when:
         * - You can't add @Component to the class (third-party lib)
         * - You want full control over construction (pass args, set properties)
         * - You want to ensure only ONE instance is ever created (CGLIB proxy guarantees this)
         */
        @Bean("smsNotification")
        public NotificationService smsNotificationService() {
            return message -> "SMS: " + message;
        }
    }

    // ------------------------------------------------------------------------------------
    // ApplicationRunner: demonstrates both beans are registered and usable
    // ------------------------------------------------------------------------------------

    @Configuration
    public static class Day02Runner {

        @Bean
        public ApplicationRunner day02Runner(ApplicationContext context) {
            return args -> {
                System.out.println("=== DAY 2: Bean Registration ===");
                System.out.println("Beans of type NotificationService registered:");

                String[] names = context.getBeanNamesForType(NotificationService.class);
                for (String name : names) {
                    NotificationService svc = (NotificationService) context.getBean(name);
                    System.out.println("  Bean[" + name + "] -> " + svc.send("Hello World"));
                }

                System.out.println();
                System.out.println("RULE: Use @Component for YOUR classes. Use @Bean for 3rd-party classes.");
                System.out.println("RULE: If two @Component beans of same type collide -> use @Primary or @Qualifier.");
                System.out.println("=================================");
            };
        }
    }
}
