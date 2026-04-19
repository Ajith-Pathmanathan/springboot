package com.techleadguru.phase2.day29;

import com.techleadguru.phase2.day29.Day29EntityManagerAndCache.UserService;
import com.techleadguru.phase2.shared.User;
import com.techleadguru.phase2.shared.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 29 — Test: 1st-level cache and dirty checking.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day29EntityManagerAndCacheTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    private String userId;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        User user = userService.createUser("alice@test.com", "Alice");
        userId = user.getId();
    }

    // -----------------------------------------------------------------------
    // Test 1: 1st-level cache returns same object reference for same entity
    // -----------------------------------------------------------------------
    @Test
    void first_level_cache_returns_same_instance_within_transaction() {
        boolean sameInstance = userService.demonstrateFirstLevelCache(userId);

        assertThat(sameInstance).isTrue();
        System.out.println("[DAY 29] 1st-level cache: same object reference for repeated findById = " + sameInstance);
    }

    // -----------------------------------------------------------------------
    // Test 2: Dirty checking auto-updates without explicit save()
    // -----------------------------------------------------------------------
    @Test
    void dirty_checking_auto_flushes_changed_managed_entity() {
        userService.updateNameWithDirtyChecking(userId, "Alice Updated");

        User updated = userRepository.findById(userId).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Alice Updated");

        System.out.println("[DAY 29] Dirty check: name updated to '" + updated.getName() + "' without save()");
    }

    // -----------------------------------------------------------------------
    // Test 3: entityManager.clear() invalidates 1st-level cache
    // -----------------------------------------------------------------------
    @Test
    void clear_invalidates_first_level_cache() {
        boolean sameInstance = userService.demonstrateClearInvalidatesCache(userId);

        assertThat(sameInstance).isFalse(); // different instances after clear()
        System.out.println("[DAY 29] After clear(): different instances — cache was invalidated: " + sameInstance);
    }

    // -----------------------------------------------------------------------
    // Test 4: Document entity states
    // -----------------------------------------------------------------------
    @Test
    void document_entity_states() {
        System.out.println("[DAY 29] ENTITY STATES:");
        System.out.println("  Transient:  new User() — not in PC, not in DB. save()/persist() → Managed.");
        System.out.println("  Managed:    In persistence context. Changes auto-dirty-checked.");
        System.out.println("  Detached:   TX ended or clear(). Was managed but no longer tracked.");
        System.out.println("              save()/merge() → re-attaches. findById() → fresh managed copy.");
        System.out.println("  Removed:    entityManager.remove(). Will be deleted on flush.");
        System.out.println();
        System.out.println("  1st-LEVEL CACHE RULES:");
        System.out.println("  - Scoped to one EntityManager (one TX in default Spring config).");
        System.out.println("  - findById(id) twice = 1 SQL. Second returns cached instance.");
        System.out.println("  - After bulk UPDATE (JPQL/native): cache is STALE. Call clear() or refresh().");
        System.out.println("  - At TX commit: dirty checking generates UPDATE for all changed managed entities.");
        assertThat(true).isTrue();
    }
}
