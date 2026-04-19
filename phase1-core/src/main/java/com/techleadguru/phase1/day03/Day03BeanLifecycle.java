package com.techleadguru.phase1.day03;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DAY 3 — Bean Lifecycle: The exact order of init and destroy callbacks.
 *
 * INIT ORDER (most important to memorise):
 *   1. Constructor
 *   2. Setter / @Autowired field injection
 *   3. @PostConstruct
 *   4. InitializingBean.afterPropertiesSet()
 *   5. Custom init-method (declared in @Bean(initMethod="..."))
 *
 * DESTROY ORDER (reverse):
 *   1. @PreDestroy
 *   2. DisposableBean.destroy()
 *   3. Custom destroy-method
 *
 * PRODUCTION SCENARIO — The Config Server injection trap:
 *   @Value("${db.url}") is injected at step 2.
 *   If Spring Cloud Config Server is used, properties arrive AFTER the local context is built.
 *   @PostConstruct fires in step 3 — BEFORE Config Server properties are available in some setups.
 *   Using @PostConstruct to open a DB connection -> connection fails with literal "${db.url}".
 *   FIX: Use ApplicationRunner (fires after full context refresh including config bootstrap).
 *
 * HOW TO RUN:
 *   Starting Phase1Application prints the full lifecycle sequence in the console.
 */
@Slf4j
public class Day03BeanLifecycle {

    @Configuration
    public static class LifecycleConfig {

        @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
        public DatabaseConnectionBean databaseConnectionBean() {
            return new DatabaseConnectionBean();
        }
    }

    @Slf4j
    public static class DatabaseConnectionBean implements InitializingBean, DisposableBean {

        // Step 1: Constructor
        public DatabaseConnectionBean() {
            log.info("[LIFECYCLE Step 1] Constructor called — properties NOT yet injected");
        }

        // Step 3: @PostConstruct
        @PostConstruct
        public void postConstruct() {
            log.info("[LIFECYCLE Step 3] @PostConstruct — dependencies injected, safe to use them");
            log.warn("[LIFECYCLE DANGER]  If you open a DB connection here, Config Server props may not be injected yet!");
            log.warn("[LIFECYCLE DANGER]  Use ApplicationRunner.run() for startup I/O instead.");
        }

        // Step 4: InitializingBean
        @Override
        public void afterPropertiesSet() {
            log.info("[LIFECYCLE Step 4] InitializingBean.afterPropertiesSet() — runs after @PostConstruct");
        }

        // Step 5: custom init-method (declared in @Bean above)
        public void customInit() {
            log.info("[LIFECYCLE Step 5] Custom init-method (from @Bean(initMethod=...)) — last init hook");
        }

        // Destroy Step 1
        @PreDestroy
        public void preDestroy() {
            log.info("[LIFECYCLE Destroy 1] @PreDestroy — release resources, close connections");
        }

        // Destroy Step 2
        @Override
        public void destroy() {
            log.info("[LIFECYCLE Destroy 2] DisposableBean.destroy() — final Spring cleanup");
        }

        // Destroy Step 3
        public void customDestroy() {
            log.info("[LIFECYCLE Destroy 3] Custom destroy-method — very last hook");
        }
    }
}
