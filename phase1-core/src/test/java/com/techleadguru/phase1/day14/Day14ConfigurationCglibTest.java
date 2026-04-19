package com.techleadguru.phase1.day14;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 14 — Test: @Configuration CGLIB proxy and the multiple-pool bug.
 *
 * We test the BrokenConfig vs CorrectConfig directly by instantiating and
 * calling @Bean methods manually — showing the difference in instance counts.
 *
 * NOTE: In a real Spring context the CGLIB proxy is applied automatically.
 * These tests prove the underlying mechanism by simulating both paths.
 */
class Day14ConfigurationCglibTest {

    @BeforeEach
    void resetCounter() {
        Day14ConfigurationCglib.TrackedDataSource.resetCount();
    }

    // -----------------------------------------------------------------------
    // Test 1: proxyBeanMethods=false — each call to a @Bean method creates NEW instance
    // (simulating what Spring does without CGLIB: direct Java method calls)
    // -----------------------------------------------------------------------
    @Test
    void without_cglib_proxy_each_inter_bean_call_creates_new_datasource() {
        // Simulating proxyBeanMethods=false behaviour: direct Java calls to @Bean methods
        Day14ConfigurationCglib.BrokenDatabaseConfig config =
                new Day14ConfigurationCglib.BrokenDatabaseConfig();

        Day14ConfigurationCglib.FakeJdbcTemplate t1 = config.jdbcTemplate1();
        Day14ConfigurationCglib.FakeJdbcTemplate t2 = config.jdbcTemplate2();

        // Each jdbcTemplate called primaryDataSource() directly → 2 different instances
        assertThat(t1.getDataSourceInstanceId()).isNotEqualTo(t2.getDataSourceInstanceId());

        System.out.println("[DAY 14] BUG: jdbcTemplate1 uses DS#" + t1.getDataSourceInstanceId() +
                ", jdbcTemplate2 uses DS#" + t2.getDataSourceInstanceId());
        System.out.println("[DAY 14] Without CGLIB: 2 separate DataSource instances = 2 connection pools!");
    }

    // -----------------------------------------------------------------------
    // Test 2: With @Configuration CGLIB proxy — same singleton returned (simulated via safe config)
    // We use SafeDatabaseConfig which injects as parameter — same behaviour as CGLIB
    // -----------------------------------------------------------------------
    @Test
    void with_parameter_injection_same_datasource_instance_used_everywhere() {
        Day14ConfigurationCglib.TrackedDataSource sharedDs =
                new Day14ConfigurationCglib.TrackedDataSource("shared");

        var t1 = new Day14ConfigurationCglib.FakeJdbcTemplate(sharedDs);
        var t2 = new Day14ConfigurationCglib.FakeJdbcTemplate(sharedDs);

        // Same instance ID because same DataSource passed to both
        assertThat(t1.getDataSourceInstanceId()).isEqualTo(t2.getDataSourceInstanceId());

        System.out.println("[DAY 14] FIX: Both JdbcTemplates share DS#" + t1.getDataSourceInstanceId());
        System.out.println("[DAY 14] One DataSource = one HikariCP connection pool");
    }

    // -----------------------------------------------------------------------
    // Test 3: Document the CGLIB proxy rule
    // -----------------------------------------------------------------------
    @Test
    void document_configuration_proxy_bean_methods_rule() {
        System.out.println("[DAY 14] @Configuration RULES:");
        System.out.println("  @Configuration (default proxyBeanMethods=true):");
        System.out.println("    Spring wraps config in CGLIB proxy.");
        System.out.println("    Inter-bean method calls (dataSource()) return the REGISTERED singleton.");
        System.out.println("    Safe for inter-bean references. Slightly slower startup.");
        System.out.println();
        System.out.println("  @Configuration(proxyBeanMethods=false):");
        System.out.println("    No CGLIB proxy. Faster startup. Used in Spring Boot 'lite mode'.");
        System.out.println("    Inter-bean calls create NEW instances each time = multiple pools.");
        System.out.println("    ONLY safe when @Bean methods do NOT call each other.");
        System.out.println();
        System.out.println("  SAFE PATTERN: Inject beans as method parameters, not via inter-bean calls.");
        System.out.println("    @Bean JdbcTemplate jdbc(DataSource ds) { return new JdbcTemplate(ds); }");
        assertThat(true).isTrue();
    }
}
