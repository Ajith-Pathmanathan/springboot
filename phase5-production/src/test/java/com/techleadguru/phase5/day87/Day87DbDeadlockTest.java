package com.techleadguru.phase5.day87;

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
@Import(Day87DbDeadlockTest.JpaConfig.class)
class Day87DbDeadlockTest {

    @TestConfiguration
    @EnableJpaRepositories(
            basePackageClasses = Day87DbDeadlock.class,
            considerNestedRepositories = true
    )
    static class JpaConfig {}

    @Autowired
    private Day87DbDeadlock.BankAccountRepository repository;

    private Day87DbDeadlock.SafeTransferService safeService;

    @BeforeEach
    void setUp() {
        safeService = new Day87DbDeadlock.SafeTransferService(repository);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void safeTransfer_transfers_amount_between_accounts() {
        Day87DbDeadlock.BankAccount a1 = repository.save(
                new Day87DbDeadlock.BankAccount(1L, 1000.0));
        Day87DbDeadlock.BankAccount a2 = repository.save(
                new Day87DbDeadlock.BankAccount(2L, 500.0));

        safeService.transfer(a1.getId(), a2.getId(), 200.0);

        Day87DbDeadlock.BankAccount updated1 = repository.findById(a1.getId()).orElseThrow();
        Day87DbDeadlock.BankAccount updated2 = repository.findById(a2.getId()).orElseThrow();

        assertThat(updated1.getBalance()).isEqualTo(800.0);
        assertThat(updated2.getBalance()).isEqualTo(700.0);
    }

    @Test
    void safeTransfer_preserves_total_balance() {
        Day87DbDeadlock.BankAccount a1 = repository.save(
                new Day87DbDeadlock.BankAccount(3L, 300.0));
        Day87DbDeadlock.BankAccount a2 = repository.save(
                new Day87DbDeadlock.BankAccount(4L, 400.0));

        double totalBefore = 300.0 + 400.0;
        safeService.transfer(a1.getId(), a2.getId(), 100.0);

        double totalAfter = repository.findById(a1.getId()).orElseThrow().getBalance()
                + repository.findById(a2.getId()).orElseThrow().getBalance();

        assertThat(totalAfter).isEqualTo(totalBefore);
    }

    @Test
    void safeTransfer_reverse_direction_also_works() {
        Day87DbDeadlock.BankAccount a1 = repository.save(
                new Day87DbDeadlock.BankAccount(5L, 50.0));
        Day87DbDeadlock.BankAccount a2 = repository.save(
                new Day87DbDeadlock.BankAccount(6L, 200.0));

        // Transfer from higher-ID to lower-ID (tests reversed lock order path)
        safeService.transfer(a2.getId(), a1.getId(), 50.0);

        double newBalance2 = repository.findById(a2.getId()).orElseThrow().getBalance();
        double newBalance1 = repository.findById(a1.getId()).orElseThrow().getBalance();
        assertThat(newBalance2).isEqualTo(150.0);
        assertThat(newBalance1).isEqualTo(100.0);
    }
}
