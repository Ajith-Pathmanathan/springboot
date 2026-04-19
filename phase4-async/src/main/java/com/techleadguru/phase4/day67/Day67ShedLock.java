package com.techleadguru.phase4.day67;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 67 — ShedLock: Distributed Lock for @Scheduled
 *
 * HOW SHEDLOCK WORKS:
 *   1. Each task has a name and a lock-at-most duration.
 *   2. Before executing, ShedLock tries to INSERT a row into the shedlock DB table.
 *      The row key = task name. INSERT succeeds only if row doesn't exist or lock has expired.
 *   3. Only ONE node gets the INSERT → only that node executes the task.
 *   4. Other nodes: INSERT fails → they skip the task silently.
 *   5. After execution: the row is updated with lock_until = now (effectively released).
 *
 * SHEDLOCK TABLE:
 *   CREATE TABLE shedlock (
 *     name       VARCHAR(64) PRIMARY KEY,
 *     lock_until TIMESTAMP   NOT NULL,
 *     locked_at  TIMESTAMP   NOT NULL,
 *     locked_by  VARCHAR(255) NOT NULL
 *   );
 *
 * CONFIGURATION PARAMETERS:
 *   lockAtMostFor:  maximum time a lock can be held (safety net for crashed nodes).
 *                   Set to max_expected_task_duration × safety_factor (e.g., 2×).
 *                   If node crashes while holding the lock, lock auto-expires so others can take over.
 *   lockAtLeastFor: minimum time a lock is held even if task completes quickly.
 *                   Prevents another node from grabbing the lock immediately (on fast tasks).
 *                   Useful when you want "run at most once per N minutes" semantics.
 *
 * PROVIDERS:
 *   JdbcTemplateLockProvider  → any JDBC DB (PostgreSQL, MySQL, H2) — simplest
 *   MongoLockProvider         → MongoDB
 *   RedisLockProvider         → Redis (via Jedis/Redisson)
 *   ZookeeperLockProvider     → ZooKeeper
 *   HazelcastLockProvider     → Hazelcast
 *
 * GOTCHA: ShedLock does NOT prevent you from calling the task method directly.
 *   LockAssert.assertLocked() inside the task verifies the lock is held.
 *   Without this check: accidental direct calls bypass the distributed lock.
 */
@Slf4j
public class Day67ShedLock {

    // =========================================================================
    // ShedLock configuration
    // =========================================================================

    @Configuration
    @EnableSchedulerLock(defaultLockAtMostFor = "10m")
    public static class ShedLockConfig {

        @Bean
        public JdbcTemplateLockProvider lockProvider(DataSource dataSource) {
            return new JdbcTemplateLockProvider(
                    JdbcTemplateLockProvider.Configuration.builder()
                            .withJdbcTemplate(new JdbcTemplate(dataSource))
                            .usingDbTime() // use DB server time to avoid clock skew between nodes
                            .build()
            );
        }
    }

    // =========================================================================
    // Locked scheduled tasks
    // =========================================================================

    @Component
    @Slf4j
    public static class PromotionalEmailJob {

        private final AtomicInteger runCount = new AtomicInteger();

        /**
         * @SchedulerLock ensures ONLY ONE instance in the cluster executes this.
         * Other instances see the lock row and skip.
         * lockAtMostFor = 5m: if this node crashes mid-task, lock auto-expires after 5min.
         * lockAtLeastFor = 1m: prevents rapid re-execution even if task finishes quickly.
         */
        @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
        @SchedulerLock(name = "promotionalEmailJob",
                lockAtMostFor = "5m",
                lockAtLeastFor = "1m")
        public void sendEmails() {
            // Verifies that the lock is actually held — throws if called without lock
            LockAssert.assertLocked();

            int run = runCount.incrementAndGet();
            log.info("[Day67] Sending promotional emails — run #{} (only 1 node in cluster runs this)", run);
            // ... email sending logic ...
        }

        /**
         * Version callable from tests (bypasses @Scheduled but DOES verify lock).
         * In tests: call via Spring proxy to trigger @SchedulerLock.
         */
        @SchedulerLock(name = "testEmailJob", lockAtMostFor = "1m", lockAtLeastFor = "100ms")
        @org.springframework.scheduling.annotation.Scheduled(fixedDelay = Long.MAX_VALUE, initialDelay = Long.MAX_VALUE)
        public void sendEmailsLocked() {
            LockAssert.assertLocked();
            int run = runCount.incrementAndGet();
            log.info("[Day67] sendEmailsLocked run #{}", run);
        }

        public int getRunCount() { return runCount.get(); }
    }
}
