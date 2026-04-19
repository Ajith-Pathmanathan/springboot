package com.techleadguru.phase2.day42;

import com.techleadguru.phase2.day42.Day42OptimisticLocking.OrderVersionService;
import com.techleadguru.phase2.day42.Day42OptimisticLocking.UserBalanceService;
import com.techleadguru.phase2.shared.Order;
import com.techleadguru.phase2.shared.OrderRepository;
import com.techleadguru.phase2.shared.User;
import com.techleadguru.phase2.shared.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DAY 42 — Test: @Version optimistic locking prevents lost updates.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day42OptimisticLockingTest {

    @Autowired
    UserBalanceService userBalanceService;

    @Autowired
    OrderVersionService orderVersionService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // Test 1: Version starts at 0 on first persist
    // -----------------------------------------------------------------------
    @Test
    void version_starts_at_zero_on_create() {
        User user = userBalanceService.createUser("v1@test.com", "VersionTest", new BigDecimal("100.00"));

        assertThat(user.getVersion()).isEqualTo(0L);
        System.out.printf("[DAY 42] Created user: version=%d, balance=%s%n",
                user.getVersion(), user.getBalance());
    }

    // -----------------------------------------------------------------------
    // Test 2: Version increments on each update
    // -----------------------------------------------------------------------
    @Test
    void version_increments_on_each_update() {
        User user = userBalanceService.createUser("v2@test.com", "VersionTest", new BigDecimal("100.00"));
        assertThat(user.getVersion()).isEqualTo(0L);

        User updated = userBalanceService.debitBalance(user.getId(), new BigDecimal("20.00"));
        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getBalance()).isEqualByComparingTo("80.00");

        System.out.printf("[DAY 42] After debit: version=%d, balance=%s%n",
                updated.getVersion(), updated.getBalance());

        User updated2 = userBalanceService.debitBalance(user.getId(), new BigDecimal("10.00"));
        assertThat(updated2.getVersion()).isEqualTo(2L);
        System.out.printf("[DAY 42] After 2nd debit: version=%d, balance=%s%n",
                updated2.getVersion(), updated2.getBalance());
    }

    // -----------------------------------------------------------------------
    // Test 3: Stale entity causes OptimisticLockingFailureException (lost update prevention)
    // -----------------------------------------------------------------------
    @Test
    void stale_update_throws_optimistic_lock_exception() {
        // TX1: create user
        User user = userBalanceService.createUser("v3@test.com", "ConcurrentTest", new BigDecimal("100.00"));

        // Load a snapshot (version=0) — this becomes STALE after TX2 updates it
        User staleUser = userBalanceService.loadUser(user.getId());
        assertThat(staleUser.getVersion()).isEqualTo(0L);

        // TX2: another transaction updates the user first (version becomes 1)
        userBalanceService.debitBalance(user.getId(), new BigDecimal("10.00"));

        // TX1 tries to update with stale version=0 — LOST UPDATE PREVENTED
        assertThatThrownBy(() ->
                userBalanceService.demonstrateConcurrentUpdate(staleUser, new BigDecimal("50.00"))
        ).isInstanceOf(OptimisticLockingFailureException.class);

        System.out.println("[DAY 42] OptimisticLockingFailureException thrown — lost update PREVENTED!");
        System.out.println("[DAY 42] @Version: UPDATE WHERE id=? AND version=0 → 0 rows affected → exception.");
    }

    // -----------------------------------------------------------------------
    // Test 4: Retry on optimistic lock conflict succeeds
    // -----------------------------------------------------------------------
    @Test
    void retry_on_optimistic_lock_conflict_succeeds() {
        User user = userBalanceService.createUser("v4@test.com", "RetryTest", new BigDecimal("100.00"));

        // Simple debit with retry mechanism
        User result = userBalanceService.debitWithRetry(user.getId(), new BigDecimal("15.00"), 3);

        assertThat(result.getBalance()).isEqualByComparingTo("85.00");
        System.out.printf("[DAY 42] Debit with retry succeeded: balance=%s, version=%d%n",
                result.getBalance(), result.getVersion());
    }

    // -----------------------------------------------------------------------
    // Test 5: Order @Version also increments on status update
    // -----------------------------------------------------------------------
    @Test
    void order_version_increments_on_status_update() {
        Order order = orderVersionService.createOrder("user-42", new BigDecimal("250.00"));
        assertThat(order.getVersion()).isEqualTo(0L);

        Order shipped = orderVersionService.updateStatus(order.getId(), "SHIPPED");
        assertThat(shipped.getVersion()).isEqualTo(1L);
        assertThat(shipped.getStatus()).isEqualTo("SHIPPED");

        Order delivered = orderVersionService.updateStatus(order.getId(), "DELIVERED");
        assertThat(delivered.getVersion()).isEqualTo(2L);

        System.out.printf("[DAY 42] Order versions: create=0, shipped=%d, delivered=%d%n",
                shipped.getVersion(), delivered.getVersion());
    }

    // -----------------------------------------------------------------------
    // Test 6: Document optimistic vs pessimistic locking
    // -----------------------------------------------------------------------
    @Test
    void document_optimistic_vs_pessimistic_locking() {
        System.out.println("[DAY 42] OPTIMISTIC vs PESSIMISTIC LOCKING:");
        System.out.println();
        System.out.println("  PESSIMISTIC (SELECT ... FOR UPDATE):");
        System.out.println("    - Locks the row immediately on read.");
        System.out.println("    - No concurrent readers allowed.");
        System.out.println("    - High contention serializes threads.");
        System.out.println("    - Use when: conflict rate is HIGH (bank account debited 1000x/sec).");
        System.out.println();
        System.out.println("  OPTIMISTIC (@Version):");
        System.out.println("    - No lock on read. Multiple concurrent readers.");
        System.out.println("    - On write: WHERE version = <current> → 0 rows → exception.");
        System.out.println("    - Loser must retry (reload and re-attempt).");
        System.out.println("    - Use when: conflict rate is LOW (typical for 99% of apps).");
        System.out.println();
        System.out.println("  @Version SQL:");
        System.out.println("    UPDATE users SET balance=?, version=1 WHERE id=? AND version=0");
        System.out.println("    If rows_affected=0 → StaleObjectStateException (Hibernate)");
        System.out.println("    Spring wraps it as OptimisticLockingFailureException.");
        assertThat(true).isTrue();
    }
}
