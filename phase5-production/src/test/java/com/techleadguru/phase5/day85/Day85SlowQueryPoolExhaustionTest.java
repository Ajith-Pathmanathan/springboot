package com.techleadguru.phase5.day85;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application.properties")
@Import(Day85SlowQueryPoolExhaustionTest.JpaConfig.class)
class Day85SlowQueryPoolExhaustionTest {

    @TestConfiguration
    @EnableJpaRepositories(
            basePackageClasses = Day85SlowQueryPoolExhaustion.class,
            considerNestedRepositories = true
    )
    static class JpaConfig {}

    @Autowired
    private Day85SlowQueryPoolExhaustion.ProductRepository repository;

    private Day85SlowQueryPoolExhaustion.ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new Day85SlowQueryPoolExhaustion.ProductService(repository);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void save_and_count_products() {
        productService.save(new Day85SlowQueryPoolExhaustion.Product("Widget A", "ELECTRONICS", 29.99));
        productService.save(new Day85SlowQueryPoolExhaustion.Product("Widget B", "ELECTRONICS", 49.99));
        assertThat(productService.count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void findProductsByCategory_returns_matching_products() {
        productService.save(new Day85SlowQueryPoolExhaustion.Product("Laptop", "COMPUTERS", 999.0));
        productService.save(new Day85SlowQueryPoolExhaustion.Product("Desk",   "FURNITURE", 299.0));

        var computers = productService.findProductsByCategory("COMPUTERS");
        assertThat(computers).hasSize(1);
        assertThat(computers.get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    void simulateExhaustion_small_pool_causes_timeouts() throws InterruptedException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:exhaustion-test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(2);
        // HikariCP minimum connectionTimeout = 250ms; tasks hold for 50ms each.
        // With 30 concurrent requests and pool=2: last batches wait > 600ms → definitely timeout.
        config.setConnectionTimeout(500);

        try (HikariDataSource ds = new HikariDataSource(config)) {
            // 30 concurrent tasks; with pool=2 and 50ms hold each, tasks in later batches
            // wait > 500ms → connection timeout
            var result = productService.simulateExhaustion(30, ds);
            assertThat(result.requested()).isEqualTo(30);
            assertThat(result.succeeded() + result.timedOut()).isEqualTo(30);
            assertThat(result.timedOut()).isGreaterThan(0);
        }
    }

    @Test
    void simulateExhaustion_large_pool_all_succeed() throws InterruptedException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:exhaustion-ok-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(5000);

        try (HikariDataSource ds = new HikariDataSource(config)) {
            var result = productService.simulateExhaustion(5, ds);
            assertThat(result.requested()).isEqualTo(5);
            assertThat(result.succeeded()).isEqualTo(5);
            assertThat(result.timedOut()).isEqualTo(0);
        }
    }
}
