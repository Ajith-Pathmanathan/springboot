package com.techleadguru.phase4.day64;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * DAY 64 — fixedRate vs fixedDelay vs cron Test
 *
 * Verifies:
 * 1. ScheduledTasksDemo bean is created and methods run without error
 * 2. Manual trigger methods work correctly
 * 3. Counter increments on each execution
 *
 * Note: Actual scheduling is NOT tested (delay=300_000ms prevents auto-fire).
 *       Tests call the methods directly via Spring proxy to verify behavior.
 */
@SpringBootTest(classes = Phase4Application.class)
class Day64ScheduledInternalsTest {

    @Autowired Day64ScheduledInternals.ScheduledTasksDemo scheduledTasksDemo;

    @Test
    void scheduled_tasks_demo_bean_is_created() {
        assertThat(scheduledTasksDemo).isNotNull();
    }

    @Test
    void runFixedRateManually_executes_without_error() {
        int before = scheduledTasksDemo.getFixedRateCount();
        assertThatNoException().isThrownBy(scheduledTasksDemo::runFixedRateManually);
        assertThat(scheduledTasksDemo.getFixedRateCount()).isEqualTo(before + 1);
    }

    @Test
    void runFixedDelayManually_executes_without_error() {
        int before = scheduledTasksDemo.getFixedDelayCount();
        assertThatNoException().isThrownBy(scheduledTasksDemo::runFixedDelayManually);
        assertThat(scheduledTasksDemo.getFixedDelayCount()).isEqualTo(before + 1);
    }

    @Test
    void runCronManually_executes_without_error() {
        int before = scheduledTasksDemo.getCronCount();
        assertThatNoException().isThrownBy(scheduledTasksDemo::runCronManually);
        assertThat(scheduledTasksDemo.getCronCount()).isEqualTo(before + 1);
    }

    @Test
    void all_three_tasks_are_independent_counters() {
        int fixedRateBefore = scheduledTasksDemo.getFixedRateCount();
        int fixedDelayBefore = scheduledTasksDemo.getFixedDelayCount();
        int cronBefore = scheduledTasksDemo.getCronCount();

        scheduledTasksDemo.runFixedRateManually();
        scheduledTasksDemo.runFixedRateManually(); // run twice

        assertThat(scheduledTasksDemo.getFixedRateCount()).isEqualTo(fixedRateBefore + 2);
        // Others unchanged
        assertThat(scheduledTasksDemo.getFixedDelayCount()).isEqualTo(fixedDelayBefore);
        assertThat(scheduledTasksDemo.getCronCount()).isEqualTo(cronBefore);
    }

    @Test
    void document_fixed_rate_vs_delay_difference() {
        // fixedRate interval = time from START of task to START of next task
        //   → overlapping can happen if task takes longer than fixedRate!
        //   → Day 65 shows how to prevent overlapping

        // fixedDelay interval = time from END of task to START of next task
        //   → never overlaps (next starts AFTER current finishes)

        // cron expression = absolute schedule (e.g., "0 0 2 * * ?" = daily at 2am)
        //   → fired at wall-clock time, independent of previous execution time

        assertThat(true).isTrue(); // documentation test
    }
}
