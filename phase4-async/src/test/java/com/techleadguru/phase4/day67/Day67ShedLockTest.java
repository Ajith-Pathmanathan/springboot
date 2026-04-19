package com.techleadguru.phase4.day67;

import com.techleadguru.phase4.Phase4Application;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 67 — ShedLock: Distributed Lock for @Scheduled Test
 *
 * Verifies:
 * 1. ShedLock configuration beans are created (JdbcTemplateLockProvider)
 * 2. PromotionalEmailJob bean is created
 * 3. The shedlock table was created by schema.sql at startup
 * 4. The lock provider can be used to describe the locking mechanism
 *
 * Note: We do NOT directly invoke the @SchedulerLock-annotated method here
 * because LockAssert.assertLocked() would throw if called outside a ShedLock context.
 * In production, ShedLock intercepts the @Scheduled invocation via AOP.
 */
@SpringBootTest(classes = Phase4Application.class)
class Day67ShedLockTest {

    @Autowired(required = false) LockProvider lockProvider;
    @Autowired(required = false) Day67ShedLock.PromotionalEmailJob promotionalEmailJob;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void shedlock_lock_provider_bean_is_created() {
        assertThat(lockProvider).isNotNull();
    }

    @Test
    void promotional_email_job_bean_is_created() {
        assertThat(promotionalEmailJob).isNotNull();
    }

    @Test
    void shedlock_table_was_created_by_schema_sql() {
        // schema.sql in test resources creates the shedlock table at startup
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shedlock", Integer.class);
        // table exists and is empty (no locks have been acquired yet)
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void run_count_starts_at_zero() {
        assertThat(promotionalEmailJob.getRunCount()).isEqualTo(0);
    }

    @Test
    void document_shedlock_mechanism() {
        // HOW SHEDLOCK WORKS:
        //
        // 1. @EnableSchedulerLock on a @Configuration class enables ShedLock AOP
        //
        // 2. @Scheduled + @SchedulerLock("taskName") on a method:
        //    a. Before execution: INSERT INTO shedlock(name, ...) WHERE lock_until < now()
        //       → If INSERT succeeds: this node "owns" the lock
        //       → If INSERT fails (another node holds lock): SKIP this execution
        //    b. During execution: task runs normally
        //    c. After execution: UPDATE shedlock SET lock_until = now() + lockAtLeastFor
        //       → Lock stays held for at least 1 minute (prevents rapid re-execution)
        //
        // 3. lockAtMostFor: safety timeout — if node crashes mid-task, lock auto-expires after 5min
        //    lockAtLeastFor: prevents another node from grabbing lock immediately after quick task
        //
        // 4. usingDbTime() — use DB server's timestamp instead of app server's clock
        //    → Avoids clock-skew issues in distributed environments
        assertThat(lockProvider).isNotNull(); // lock provider presence confirms configuration
    }
}
