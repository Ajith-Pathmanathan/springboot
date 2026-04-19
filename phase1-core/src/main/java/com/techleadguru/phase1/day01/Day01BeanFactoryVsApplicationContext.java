package com.techleadguru.phase1.day01;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * DAY 1 — BeanFactory vs ApplicationContext
 *
 * KEY DIFFERENCE YOU MUST KNOW:
 *   BeanFactory     = bare-bones container. Only creates beans on demand (lazy by default).
 *                     NO event publishing, NO @EventListener, NO AOP auto-proxy, NO i18n.
 *   ApplicationContext = full-featured container. Eager singleton init, events, AOP, i18n, etc.
 *   Spring Boot always gives you an ApplicationContext (AnnotationConfigServletWebServerApplicationContext).
 *
 * PRODUCTION SCENARIO BAKED IN:
 *   If you ever use BeanFactory directly (e.g., inside a library), @EventListener won't fire.
 *   This demo shows EXACTLY why — so you recognise it in production logs.
 *
 * HOW TO RUN:
 *   Run Phase1Application. Watch the console. You will see:
 *   1. ApplicationContext bean count printed.
 *   2. The ApplicationRunner executes — proving ApplicationContext is wired correctly.
 *   The BeanFactory part is demonstrated in the test (Day1BeanFactoryTest).
 */
@Configuration
public class Day01BeanFactoryVsApplicationContext {

    /**
     * This ApplicationRunner fires AFTER the full ApplicationContext is refreshed.
     * This is the SAFE place for startup logic (not @PostConstruct, not ContextRefreshedEvent).
     *
     * PRODUCTION RULE: Use ApplicationRunner for cache warmup, DB seeding, connectivity checks.
     */
    @Bean
    public ApplicationRunner day01Runner(ApplicationContext context) {
        return args -> {
            System.out.println("=== DAY 1: BeanFactory vs ApplicationContext ===");
            System.out.println("[ApplicationContext] Type : " + context.getClass().getSimpleName());
            System.out.println("[ApplicationContext] Total beans registered: " + context.getBeanDefinitionCount());

            // Show a few bean names to illustrate how many beans Spring registers automatically
            String[] beanNames = context.getBeanDefinitionNames();
            System.out.println("[ApplicationContext] First 5 beans: "
                    + Arrays.toString(Arrays.copyOf(beanNames, Math.min(5, beanNames.length))));

            System.out.println();
            System.out.println("  BeanFactory  = lazy, no events, no AOP auto-proxy");
            System.out.println("  ApplicationContext = eager singletons + events + AOP + i18n");
            System.out.println("  Spring Boot gives you ApplicationContext every time.");
            System.out.println("  NEVER use BeanFactory in your own code — only exists inside framework internals.");
            System.out.println("=================================================");
        };
    }
}
