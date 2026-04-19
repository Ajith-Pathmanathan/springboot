package com.techleadguru.phase1.day05;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * DAY 5 — BeanFactoryPostProcessor: Modify bean DEFINITIONS before any bean is created.
 *
 * KEY DIFFERENCE vs BeanPostProcessor (Day 4):
 *   BeanPostProcessor    = modifies BEAN INSTANCES after they are created
 *   BeanFactoryPostProcessor = modifies BEAN DEFINITIONS before any instance is created
 *
 * MOST IMPORTANT BFPP you use every day without knowing it:
 *   PropertySourcesPlaceholderConfigurer = the BFPP that resolves @Value("${...}") placeholders.
 *   If it's missing, your @Value fields contain the literal string "${db.url}".
 *
 * PRODUCTION SCENARIO — The @Value literal string bug:
 *   Developer moves PropertySourcesPlaceholderConfigurer @Bean to a @Component class
 *   (or creates it as a non-static @Bean in @Configuration).
 *   It registers too late (after BFPPs should have run).
 *   All @Value injections contain literal "${...}" strings.
 *   App starts fine. DB calls fail with "Cannot load driver class: ${db.url}".
 *   FIX: PropertySourcesPlaceholderConfigurer must ALWAYS be a static @Bean in @Configuration.
 *
 * In Spring Boot you almost never see this because Boot auto-configures it.
 * But in Spring Framework or custom starters, this is a real trap.
 */
@Slf4j
public class Day05BeanFactoryPostProcessor {

    // ------------------------------------------------------------------------------------
    // BFPP #1: Print all registered bean definitions BEFORE any bean is created
    // This is how you can introspect the entire container at the definition level
    // ------------------------------------------------------------------------------------

    @Configuration
    public static class BFPPConfig {

        @Bean
        public static BeanDefinitionPrinter beanDefinitionPrinter() {
            return new BeanDefinitionPrinter();
        }

        /**
         * CRITICAL: This must be a static @Bean.
         * If non-static, Spring creates the @Configuration instance first (which needs
         * other beans), creating a chicken-and-egg problem.
         * PropertySourcesPlaceholderConfigurer has this same requirement.
         */
        @Bean
        public static ScopeModifierBFPP scopeModifierBFPP() {
            return new ScopeModifierBFPP();
        }
    }

    // ------------------------------------------------------------------------------------
    // BFPP #1: Audit all bean definitions before context starts
    // ------------------------------------------------------------------------------------

    @Slf4j
    public static class BeanDefinitionPrinter implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            log.info("[BFPP] ==============================================");
            log.info("[BFPP] Bean definitions registered: {}", beanFactory.getBeanDefinitionCount());
            log.info("[BFPP] Printing all definition names:");
            for (String name : beanFactory.getBeanDefinitionNames()) {
                BeanDefinition bd = beanFactory.getBeanDefinition(name);
                log.debug("[BFPP]   {} | scope={} | class={}",
                        name,
                        bd.getScope().isEmpty() ? "singleton" : bd.getScope(),
                        bd.getBeanClassName() != null ? bd.getBeanClassName() : "anonymous");
            }
            log.info("[BFPP] ==============================================");
            log.info("[BFPP] NOTE: Beans are NOT created yet at this point.");
            log.info("[BFPP] BeanFactoryPostProcessor runs BEFORE any bean instantiation.");
        }
    }

    // ------------------------------------------------------------------------------------
    // BFPP #2: Modify a bean's scope from singleton to prototype at container startup
    // Use case: environment-specific scope changes without touching the source classes
    // ------------------------------------------------------------------------------------

    @Slf4j
    public static class ScopeModifierBFPP implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            // Check if the target bean definition exists before modifying
            if (beanFactory.containsBeanDefinition("reportingService")) {
                BeanDefinition bd = beanFactory.getBeanDefinition("reportingService");
                String originalScope = bd.getScope().isEmpty() ? "singleton" : bd.getScope();
                bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
                log.info("[BFPP SCOPE CHANGE] 'reportingService' scope: {} -> prototype", originalScope);
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Demo bean whose scope gets modified by the BFPP above at startup
    // ------------------------------------------------------------------------------------

    @Configuration
    static class Day05Config {

        @Bean
        @Scope("singleton") // BFPP will change this to prototype before first use
        public ReportingService reportingService() {
            return new ReportingService();
        }
    }

    @Slf4j
    public static class ReportingService {
        public ReportingService() {
            log.info("[ReportingService] New instance created — if prototype, one per getBean() call");
        }
    }
}
