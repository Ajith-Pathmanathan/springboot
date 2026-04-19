package com.techleadguru.phase2.day30;

import com.techleadguru.phase2.shared.User;
import com.techleadguru.phase2.shared.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAY 30 — Entity States: Transient → Managed → Detached → Removed
 *
 * UNDERSTANDING THE FULL LIFECYCLE:
 *
 *   TRANSIENT → MANAGED:
 *     entityManager.persist(entity) OR repository.save(entity) when id is null.
 *     Entity is tracked by the persistence context. INSERT on flush.
 *
 *   MANAGED → DETACHED:
 *     entityManager.detach(entity) — removes entity from PC without removing from DB.
 *     entityManager.clear() — detaches ALL entities.
 *     TX ends — all entities become detached.
 *
 *   DETACHED → MANAGED:
 *     entityManager.merge(entity) — re-attaches a detached entity.
 *     Hibernate copies the detached state into a new managed instance.
 *     The ORIGINAL detached object is NOT the merged one — merge returns new managed instance.
 *
 *   MANAGED → REMOVED:
 *     entityManager.remove(entity) OR repository.delete(entity).
 *     DELETE SQL on flush. Entity removed from PC after flush.
 *
 * THE DETACHED ENTITY TRAP:
 *   Developer loads entity in TX1 (entity is managed → TX1 commits → entity is now DETACHED).
 *   In TX2, same object is modified. Changes are NOT auto-flush — entity is DETACHED.
 *   Developer expects dirty check to save — IT DOESN'T.
 *   No exception, no error. Data silently NOT saved.
 *   FIX: Call repository.save(detachedEntity) — triggers merge() internally.
 *        OR reload entity inside the new TX.
 *
 * PRODUCTION SCENARIO — Lost updates in REST controllers:
 *   @RestController method uses @Transactional for GET. Returns User entity as response.
 *   Response serialization: entity is returned AFTER TX commits. Entity is now detached.
 *   Client sends PUT. Controller receives the JSON, builds a User object.
 *   Calls user.setName(); — detached entity! No managed, no dirty check.
 *   Does not call save(). Returns 200 OK. Name NOT updated.
 *   FIX: Always call save() after modifying entities received as request bodies.
 *        Better: use DTOs for request/response. Never return managed entities from APIs.
 */
@Slf4j
public class Day30EntityStates {

    // ===================================================================================
    // Uses shared.User / shared.UserRepository (Customer concept = User entity here)
    // ===================================================================================

    @Service
    @Slf4j
    public static class CustomerService {

        private final UserRepository customerRepository;
        private final EntityManager entityManager;

        public CustomerService(UserRepository customerRepository, EntityManager entityManager) {
            this.customerRepository = customerRepository;
            this.entityManager = entityManager;
        }

        @Transactional
        public User createCustomer(String email, String name) {
            return customerRepository.save(new User(email, name));
        }

        /**
         * Returns a User that is DETACHED (TX ends after this method returns).
         */
        @Transactional
        public User loadForDetach(String id) {
            return customerRepository.findById(id).orElseThrow();
            // TX ends here → entity becomes DETACHED
        }

        /**
         * THE BUG: entity is detached. Modification has NO effect on DB without save().
         */
        @Transactional
        public void updateNameWithoutSave(User detachedCustomer, String newName) {
            detachedCustomer.setName(newName); // detached — Hibernate ignores this change!
            // No save() — detached entity changes are NOT persisted
            log.warn("[Day30] BUG: modified detached entity without save() — change lost!");
        }

        /**
         * THE FIX: call save() (which calls merge() internally) to re-attach and persist.
         */
        @Transactional
        public User updateNameWithSave(User detachedCustomer, String newName) {
            detachedCustomer.setName(newName);
            User merged = customerRepository.save(detachedCustomer); // merge() into managed state
            log.info("[Day30] FIX: merged detached entity — new name: {}", merged.getName());
            return merged;
        }

        /**
         * Shows explicit detach() and merge() flow.
         */
        @Transactional
        public String demonstrateDetachAndMerge(String id) {
            User managed = customerRepository.findById(id).orElseThrow();
            log.info("[Day30] Before detach: isManaged={}", entityManager.contains(managed));

            entityManager.detach(managed);
            log.info("[Day30] After detach: isManaged={}", entityManager.contains(managed));

            managed.setName("Detached Name Update");

            User reManaged = entityManager.merge(managed); // merge returns NEW managed instance
            log.info("[Day30] After merge: original isManaged={}, reManaged isManaged={}",
                    entityManager.contains(managed), entityManager.contains(reManaged));

            return reManaged.getName();
        }
    }
}
