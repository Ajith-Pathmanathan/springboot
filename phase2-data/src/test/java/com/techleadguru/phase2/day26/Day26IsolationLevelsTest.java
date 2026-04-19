package com.techleadguru.phase2.day26;

import com.techleadguru.phase2.day26.Day26IsolationLevels.BankService;
import com.techleadguru.phase2.shared.User;
import com.techleadguru.phase2.shared.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAY 26 — Test: Isolation levels — correct balance after transfer.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day26IsolationLevelsTest {

    @Autowired
    BankService bankService;

    @Autowired
    UserRepository accountRepository;

    private String aliceId;
    private String bobId;

    @BeforeEach
    @Transactional
    void setup() {
        accountRepository.deleteAll();
        User alice = bankService.createAccount("alice@bank.com", "Alice", new BigDecimal("1000"));
        User bob = bankService.createAccount("bob@bank.com", "Bob", new BigDecimal("500"));
        aliceId = alice.getId();
        bobId = bob.getId();
    }

    // -----------------------------------------------------------------------
    // Test 1: READ_COMMITTED transfer — balances update correctly
    // -----------------------------------------------------------------------
    @Test
    void read_committed_transfer_updates_balances_correctly() {
        bankService.transferReadCommitted(aliceId, bobId, new BigDecimal("200"));

        assertThat(bankService.getBalance(aliceId)).isEqualByComparingTo("800");
        assertThat(bankService.getBalance(bobId)).isEqualByComparingTo("700");

        System.out.println("[DAY 26] READ_COMMITTED: Alice=800, Bob=700");
    }

    // -----------------------------------------------------------------------
    // Test 2: SERIALIZABLE transfer — same correct result
    // -----------------------------------------------------------------------
    @Test
    void serializable_transfer_updates_balances_correctly() {
        bankService.transferSerializable(aliceId, bobId, new BigDecimal("300"));

        assertThat(bankService.getBalance(aliceId)).isEqualByComparingTo("700");
        assertThat(bankService.getBalance(bobId)).isEqualByComparingTo("800");

        System.out.println("[DAY 26] SERIALIZABLE: Alice=700, Bob=800");
    }

    // -----------------------------------------------------------------------
    // Test 3: Insufficient funds throws
    // -----------------------------------------------------------------------
    @Test
    void transfer_throws_when_insufficient_funds() {
        assertThatThrownBy(() ->
                bankService.transferReadCommitted(aliceId, bobId, new BigDecimal("2000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient funds");
    }

    // -----------------------------------------------------------------------
    // Test 4: Document isolation levels
    // -----------------------------------------------------------------------
    @Test
    void document_isolation_levels() {
        System.out.println("[DAY 26] ISOLATION LEVEL COMPARISON:");
        System.out.println("  Level             | Dirty Read | Non-Repeatable | Phantom");
        System.out.println("  READ_UNCOMMITTED  |    YES     |      YES       |   YES");
        System.out.println("  READ_COMMITTED    |    No      |      YES       |   YES (PostgreSQL default)");
        System.out.println("  REPEATABLE_READ   |    No      |      No        |   YES (MySQL InnoDB default)");
        System.out.println("  SERIALIZABLE      |    No      |      No        |   No  (slowest)");
        System.out.println();
        System.out.println("  RULES:");
        System.out.println("  1. Never use READ_UNCOMMITTED in production.");
        System.out.println("  2. READ_COMMITTED is the safe PostgreSQL default for most operations.");
        System.out.println("  3. For financial/inventory: add @Version (optimistic lock) or SERIALIZABLE.");
        System.out.println("  4. SERIALIZABLE under high load can cause 'could not serialize access' errors.");
        assertThat(true).isTrue();
    }
}
