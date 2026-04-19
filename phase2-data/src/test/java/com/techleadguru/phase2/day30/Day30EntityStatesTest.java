package com.techleadguru.phase2.day30;

import com.techleadguru.phase2.day30.Day30EntityStates.CustomerService;
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
 * DAY 30 — Test: Entity state transitions.
 */
@SpringBootTest(classes = com.techleadguru.phase2.Phase2Application.class)
@ActiveProfiles("test")
class Day30EntityStatesTest {

    @Autowired
    CustomerService customerService;

    @Autowired
    UserRepository customerRepository;

    private String customerId;

    @BeforeEach
    void setup() {
        customerRepository.deleteAll();
        User c = customerService.createCustomer("test@day30.com", "OriginalName");
        customerId = c.getId();
    }

    // -----------------------------------------------------------------------
    // Test 1: Modifying detached entity without save() has NO effect
    // -----------------------------------------------------------------------
    @Test
    void modifying_detached_entity_without_save_has_no_effect() {
        User detached = customerService.loadForDetach(customerId);

        // Modify detached entity in another TX — no save()
        customerService.updateNameWithoutSave(detached, "ShouldNotBeSaved");

        // Reload from DB — name should still be original
        User reloaded = customerRepository.findById(customerId).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("OriginalName"); // unchanged!

        System.out.println("[DAY 30] BUG confirmed: detached entity change lost. Name=" + reloaded.getName());
    }

    // -----------------------------------------------------------------------
    // Test 2: Calling save() on detached entity persists the change
    // -----------------------------------------------------------------------
    @Test
    void calling_save_on_detached_entity_persists_change() {
        User detached = customerService.loadForDetach(customerId);
        customerService.updateNameWithSave(detached, "UpdatedName");

        User reloaded = customerRepository.findById(customerId).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("UpdatedName");

        System.out.println("[DAY 30] FIX: save() on detached entity updated name to " + reloaded.getName());
    }

    // -----------------------------------------------------------------------
    // Test 3: detach() + merge() flow
    // -----------------------------------------------------------------------
    @Test
    void explicit_detach_and_merge_re_attaches_entity() {
        String newName = customerService.demonstrateDetachAndMerge(customerId);

        assertThat(newName).isEqualTo("Detached Name Update");
        System.out.println("[DAY 30] merge() returned updated name: " + newName);
    }

    // -----------------------------------------------------------------------
    // Test 4: Document entity state transitions
    // -----------------------------------------------------------------------
    @Test
    void document_entity_state_transitions() {
        System.out.println("[DAY 30] ENTITY STATE TRANSITIONS:");
        System.out.println("  new Entity()         → TRANSIENT (not tracked, not in DB)");
        System.out.println("  persist(entity)       → MANAGED   (tracked, INSERT on flush)");
        System.out.println("  TX ends              → DETACHED  (has data, not tracked)");
        System.out.println("  detach(entity)        → DETACHED  (explicit)");
        System.out.println("  merge(detached)       → MANAGED   (new managed copy returned)");
        System.out.println("  save(detached)        → MANAGED   (Spring JPA calls merge internally)");
        System.out.println("  remove(managed)       → REMOVED   (DELETE on flush)");
        System.out.println();
        System.out.println("  PRODUCTION RULES:");
        System.out.println("  1. NEVER modify a detached entity and expect auto-save.");
        System.out.println("  2. Call save() after modifying detached entities.");
        System.out.println("  3. Prefer loading entities fresh within the TX boundary.");
        System.out.println("  4. Use DTOs to avoid passing managed/detached entities across layers.");
        assertThat(true).isTrue();
    }
}
