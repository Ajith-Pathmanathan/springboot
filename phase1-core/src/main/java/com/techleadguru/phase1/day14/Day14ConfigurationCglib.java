package com.techleadguru.phase1.day14;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DAY 14 — @Configuration CGLIB Proxy: The Multiple Connection Pool Bug
 *
 * THE RULE:
 *   @Configuration classes are enhanced by Spring via CGLIB proxy.
 *   When one @Bean method calls another @Bean method inside the same @Configuration class,
 *   the call goes through the CGLIB proxy — so Spring returns the REGISTERED singleton bean,
 *   not a brand-new instance.
 *
 * THE BUG (multiple connection pools created):
 *   class DatabaseConfig {
 *       @Bean DataSource dataSource() { return new HikariDataSource(config()); }
 *       @Bean EntityManagerFactory emf() { return new LocalContainerEntityManagerFactoryBean(config()); }
 *       @Bean TransactionManager tm() { return new JpaTransactionManager(emf(), dataSource()); }
 *       @Bean JdbcTemplate jdbc() { return new JdbcTemplate(dataSource()); }  // ← WRONG WITHOUT CGLIB!
 *   }
 *   Without @Configuration CGLIB proxy: dataSource() called 3 times → 3 separate pools created.
 *   With    @Configuration CGLIB proxy: dataSource() called 3 times → same singleton returned.
 *
 * THE TRAP — @Configuration(proxyBeanMethods=false):
 *   This disables CGLIB proxy for performance (used in Spring Boot lite-mode @Configuration).
 *   Inter-bean calls now create NEW instances each time — multiple connection pools!
 *   RULE: ONLY use proxyBeanMethods=false when your @Bean methods do NOT call each other.
 *
 * PRODUCTION SCENARIO:
 *   Microservice migrated to native image (GraalVM). CGLIB proxy not supported.
 *   Team adds @Configuration(proxyBeanMethods=false) to all configs.
 *   DataSource bean called 4 times in one @Configuration.
 *   Production: HikariCP opens 4×10=40 connections per pod, pool exhausted under load.
 *   FIX: Inject DataSource as method parameter instead of calling dataSource() directly.
 */
@Slf4j
public class Day14ConfigurationCglib {

    // ===================================================================================
    // THE BUG: proxyBeanMethods=false — each inter-bean call creates a NEW instance
    // ===================================================================================

    @Configuration(proxyBeanMethods = false) // disables CGLIB proxy — DANGEROUS for inter-bean calls
    public static class BrokenDatabaseConfig {

        @Bean("brokenPrimaryDataSource")
        public TrackedDataSource primaryDataSource() {
            return new TrackedDataSource("primary");
        }

        @Bean("brokenJdbcTemplate1")
        public FakeJdbcTemplate jdbcTemplate1() {
            // Without CGLIB proxy: primaryDataSource() creates a NEW TrackedDataSource each call!
            return new FakeJdbcTemplate(primaryDataSource());
        }

        @Bean("brokenJdbcTemplate2")
        public FakeJdbcTemplate jdbcTemplate2() {
            // Another call → yet another NEW TrackedDataSource!
            return new FakeJdbcTemplate(primaryDataSource());
        }
    }

    // ===================================================================================
    // THE FIX: @Configuration (default proxyBeanMethods=true) — CGLIB returns singleton
    // ===================================================================================

    @Configuration // proxyBeanMethods=true (default) — CGLIB proxy enabled
    public static class CorrectDatabaseConfig {

        @Bean("correctPrimaryDataSource")
        public TrackedDataSource primaryDataSource() {
            return new TrackedDataSource("primary");
        }

        @Bean("correctJdbcTemplate1")
        public FakeJdbcTemplate jdbcTemplate1() {
            // With CGLIB: primaryDataSource() returns the REGISTERED singleton — same instance!
            return new FakeJdbcTemplate(primaryDataSource());
        }

        @Bean("correctJdbcTemplate2")
        public FakeJdbcTemplate jdbcTemplate2() {
            // Same singleton returned again
            return new FakeJdbcTemplate(primaryDataSource());
        }
    }

    // ===================================================================================
    // THE RIGHT WAY: inject as method parameter (works in both proxy modes)
    // ===================================================================================

    @Configuration(proxyBeanMethods = false) // safe: no inter-bean calls
    public static class SafeDatabaseConfig {

        @Bean("safePrimaryDataSource")
        public TrackedDataSource primaryDataSource() {
            return new TrackedDataSource("safe-primary");
        }

        @Bean("safeJdbcTemplate1")
        public FakeJdbcTemplate jdbcTemplate1(@org.springframework.beans.factory.annotation.Qualifier("safePrimaryDataSource") TrackedDataSource primaryDataSource) {
            // Spring INJECTS the registered singleton — same as CGLIB approach
            return new FakeJdbcTemplate(primaryDataSource);
        }

        @Bean("safeJdbcTemplate2")
        public FakeJdbcTemplate jdbcTemplate2(@org.springframework.beans.factory.annotation.Qualifier("safePrimaryDataSource") TrackedDataSource primaryDataSource) {
            return new FakeJdbcTemplate(primaryDataSource);
        }
    }

    // ===================================================================================
    // Supporting classes
    // ===================================================================================

    @Slf4j
    public static class TrackedDataSource {
        private static int creationCount = 0;
        private final int instanceId;
        private final String name;

        public TrackedDataSource(String name) {
            this.name = name;
            instanceId = ++creationCount;
            log.info("[Day14] TrackedDataSource #{} created: {}", instanceId, name);
        }

        public int getInstanceId() { return instanceId; }
        public String getName() { return name; }

        public static void resetCount() { creationCount = 0; }
    }

    public static class FakeJdbcTemplate {
        private final TrackedDataSource dataSource;

        public FakeJdbcTemplate(TrackedDataSource dataSource) {
            this.dataSource = dataSource;
        }

        public int getDataSourceInstanceId() { return dataSource.getInstanceId(); }
    }
}
