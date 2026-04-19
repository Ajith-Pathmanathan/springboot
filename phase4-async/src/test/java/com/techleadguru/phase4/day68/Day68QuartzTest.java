package com.techleadguru.phase4.day68;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 68 — Quartz Scheduler Test
 *
 * Verifies:
 * 1. Quartz Scheduler bean is created (spring-boot-starter-quartz auto-configures it)
 * 2. JobDetail and Trigger beans are registered in the scheduler
 * 3. Job can be manually triggered for testing (bypassing cron schedule)
 * 4. ReportGenerationJob reads reportType from JobDataMap
 *
 * Note: spring.quartz.auto-startup=false in test properties prevents the entire
 * scheduler from starting automatically. We start/stop it manually if needed.
 */
@SpringBootTest(classes = Phase4Application.class)
class Day68QuartzTest {

    @Autowired Scheduler quartzScheduler;
    @Autowired JobDetail reportJobDetail;
    @Autowired Day68Quartz.ReportGenerationJob reportGenerationJob;

    @Test
    void quartz_scheduler_bean_is_created() {
        assertThat(quartzScheduler).isNotNull();
    }

    @Test
    void report_job_detail_is_registered() {
        assertThat(reportJobDetail).isNotNull();
        assertThat(reportJobDetail.getKey().getName()).isEqualTo("reportJob");
        assertThat(reportJobDetail.getKey().getGroup()).isEqualTo("reports");
    }

    @Test
    void report_job_detail_has_correct_job_data() {
        assertThat(reportJobDetail.getJobDataMap().getString("reportType"))
                .isEqualTo("DAILY_SALES");
    }

    @Test
    void report_job_detail_is_stored_durably() {
        // storeDurably() means the job survives even if trigger is unscheduled
        assertThat(reportJobDetail.isDurable()).isTrue();
    }

    @Test
    void job_can_be_manually_triggered_via_scheduler() throws Exception {
        // Start scheduler to allow manual trigger
        quartzScheduler.start();
        try {
            int countBefore = reportGenerationJob.getExecutionCount();

            // Trigger the job immediately (bypasses the cron schedule)
            quartzScheduler.triggerJob(
                    JobKey.jobKey("reportJob", "reports"),
                    new JobDataMap(java.util.Map.of("reportType", "MANUAL_TEST"))
            );

            // Wait for job execution
            Thread.sleep(500);
            assertThat(reportGenerationJob.getExecutionCount()).isGreaterThan(countBefore);
        } finally {
            quartzScheduler.standby(); // suspend without shutting down
        }
    }

    @Test
    void document_quartz_vs_scheduled_differences() {
        // @Scheduled (Day 64):
        //   + Simple — just annotate the method
        //   + No DB required
        //   - Not clustered (need ShedLock — Day 67)
        //   - No job history, retry, or pause/resume
        //   - Configuration is in code (no UI)
        //
        // Quartz:
        //   + Fully distributed when using JDBC JobStore
        //   + Job history, retry on failure, pause/resume
        //   + Survives application restart (job state in DB)
        //   + Can be managed via Actuator or dedicated UI
        //   - Requires many Quartz tables in DB
        //   - More complex setup and configuration
        //
        // Use @Scheduled + ShedLock for simple cases
        // Use Quartz when you need history, retry, or complex job management
        assertThat(quartzScheduler).isNotNull();
    }
}
