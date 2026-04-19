package com.techleadguru.phase2.day29;

import com.techleadguru.phase2.shared.User;
import com.techleadguru.phase2.shared.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAY 29 — EntityManager & Persistence Context (1st Level Cache)
 *
 * PERSISTENCE CONTEXT:
 *   - A "unit of work" managed by the EntityManager.
 *   - Tracks ALL entities loaded during the current transaction.
 *   - Acts as a 1st-level cache: load the same entity twice → same Java object, no 2nd DB query.
 *   - Automatically detects changes to managed entities (dirty checking).
 *   - Flushes changes to DB before queries or at TX commit.
 *
 * ENTITY STATES:
 *   Transient:  Created with `new`. Not in persistence context. Not in DB.
 *   Managed:    Associated with a persistence context. Changes auto-flushed.
 *   Detached:   Was managed, TX ended. Still has data but not tracked.
 *   Removed:    Marked for deletion. Will be deleted on flush.
 *
 * 1ST-LEVEL CACHE BENEFIT AND TRAP:
 *   BENEFIT: No 2x DB hits when the same entity is used in multiple places in one TX.
 *   TRAP: The cache is scoped to the TX. In a new TX or a new request, the cache is empty.
 *         Bulk updates (UPDATE orders SET status='X' WHERE ...) bypass the cache.
 *         After a bulk update, stale data may be returned from the cache.
 *         FIX: entityManager.clear() or entityManager.refresh(entity) after bulk updates.
 *
 * DIRTY CHECKING:
 *   Hibernate snapshots the entity state when it is loaded (or saved).
 *   At flush time: compares current state with snapshot.
 *   If different: generates UPDATE SQL automatically.
 *   No need to call save() again after modifying a managed entity.
 *   BUT: if you call save() unnecessarily in a loop → Hibernate merges each time → performance hit.
 *
 * PRODUCTION SCENARIO — 1st-level cache stale reads after bulk update:
 *   Admin runs SQL: UPDATE users SET active=false WHERE last_login < 2024-01-01
 *   Spring @Transactional service then calls userRepository.findById(userId) — returns a cached
 *   entity with active=true (loaded before the bulk SQL) — stale!
 *   UserService allows deactivated user to proceed.
 *   FIX: After bulk operations, call entityManager.clear() to empty the 1st-level cache.
 */
@Slf4j
public class Day29EntityManagerAndCache {

    // ===================================================================================
    // Uses shared.User / shared.UserRepository
    // ===================================================================================

    // ===================================================================================
    // Service demonstrating 1st-level cache and dirty checking
    // ===================================================================================

    @Service
    @Slf4j
    public static class UserService {

        private final UserRepository userRepository;
        private final jakarta.persistence.EntityManager entityManager;

        public UserService(UserRepository userRepository,
                           jakarta.persistence.EntityManager entityManager) {
            this.userRepository = userRepository;
            this.entityManager = entityManager;
        }

        @Transactional
        public User createUser(String email, String name) {
            return userRepository.save(new User(email, name));
        }

        /**
         * 1st-level cache: loading same entity twice returns same object reference.
         * Only ONE SQL SELECT is issued for both findById calls.
         */
        @Transactional
        public boolean demonstrateFirstLevelCache(String userId) {
            User first = userRepository.findById(userId).orElseThrow();
            User second = userRepository.findById(userId).orElseThrow(); // hits CACHE, no SQL!

            boolean sameInstance = (first == second); // same Java object from 1st-level cache
            log.info("[Day29] Same instance? {} (first SQL hit, second from cache)", sameInstance);
            return sameInstance;
        }

        /**
         * Dirty checking: modifying a managed entity auto-generates UPDATE.
         * No explicit save() required.
         */
        @Transactional
        public String updateNameWithDirtyChecking(String userId, String newName) {
            User user = userRepository.findById(userId).orElseThrow();
            user.setName(newName); // Changed on managed entity — auto-flushed at TX commit
            // NO save() here! Hibernate dirty checks and generates UPDATE automatically.
            log.info("[Day29] Dirty checking: name changed to '{}' — no save() needed", newName);
            return user.getId();
        }

        /**
         * After entityManager.clear(): 1st-level cache is empty, next findById hits DB.
         */
        @Transactional
        public boolean demonstrateClearInvalidatesCache(String userId) {
            User first = userRepository.findById(userId).orElseThrow(); // hits DB
            entityManager.clear(); // 1st-level cache emptied
            User second = userRepository.findById(userId).orElseThrow(); // hits DB again (cache miss)

            boolean sameInstance = (first == second); // FALSE — different objects
            log.info("[Day29] After clear(): same instance? {} — cache was invalidated", sameInstance);
            return sameInstance;
        }
    }
}
