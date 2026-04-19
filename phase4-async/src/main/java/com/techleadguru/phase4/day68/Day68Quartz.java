package com.techleadguru.phase4.day68;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 68 — Quartz: JDBC JobStore, Survives Restart
 *
 * @SCHEDULED vs QUARTZ:
 *
 *   @Scheduled:
 *     ✅ Simple, zero config, Spring native
 *     ❌ Jobs defined in code (no runtime changes)
 *     ❌ State is in-memory (lost on restart — if app restarts at 3am, the "3am job" is missed)
 *     ❌ No built-in retry, no job history, no admin UI
 *     ❌ Cluster duplicate execution problem (fix with ShedLock)
 *
 *   Quartz with JDBC JobStore:
 *     ✅ Persistent: jobs survive restarts, missed fires are recovered
 *     ✅ Cluster-aware: JDBC table used as distributed coordination (misfire handling)
 *     ✅ Dynamic: add/pause/resume/delete jobs at runtime
 *     ✅ Rich trigger types: simple, cron, calendar-based, misfire policies
 *     ✅ Job history and execution tracking
 *     ❌ Complex setup: needs 11 Quartz DB tables
 *     ❌ More dependencies, steeper learning curve
 *
 * QUARTZ CORE CONCEPTS:
 *   Job:     What to execute (class implementing Job interface)
 *   JobDetail: Descriptor of a job (class + key + data map)
 *   Trigger: When to execute (simple interval, cron, or custom)
 *   Scheduler: The engine orchestrating jobs and triggers
 *   JobDataMap: Key-value store passed to job at execution time
 *
 * MISFIRE HANDLING (what Quartz does when a scheduled fire is missed):
 *   MISFIRE_INSTRUCTION_FIRE_NOW     → fire immediately when app restarts
 *   MISFIRE_INSTRUCTION_DO_NOTHING   → skip missed fires, resume normal schedule
 *   MISFIRE_INSTRUCTION_RESCHEDULE   → reschedule from now
 *
 * JOBSTORE TYPES:
 *   RAMJobStore    → in-memory, lost on restart (demo/test use)
 *   JDBCJobStore   → persisted in DB, cluster-safe (production use)
 *   TerracottaJobStore → Terracotta-based (enterprise)
 *
 * SPRING BOOT CONFIGURATION:
 *   spring.quartz.job-store-type=jdbc          → use JDBC store
 *   spring.quartz.jdbc.initialize-schema=always → auto-create Quartz tables
 *   spring.quartz.auto-startup=true            → start scheduler at context refresh
 */
@Slf4j
public class Day68Quartz {

    // =========================================================================
    // A simple Quartz Job
    // =========================================================================

    /**
     * QuartzJobBean is Spring's adapter for Quartz Job.
     * It auto-injects Spring beans into the job before executing.
     * Spring creates a new instance for each execution → not a singleton.
     */
    @Component
    @Slf4j
    public static class ReportGenerationJob extends QuartzJobBean {

        private static final AtomicInteger executionCount = new AtomicInteger();

        @Override
        protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
            int run = executionCount.incrementAndGet();
            JobDataMap data = context.getMergedJobDataMap();
            String reportType = data.getString("reportType");
            log.info("[Day68] ReportGenerationJob run #{}: reportType={}, thread={}",
                    run, reportType, Thread.currentThread().getName());
            // In production: generate report, save to S3, send notification
        }

        public int getExecutionCount() { return executionCount.get(); }
    }

    // =========================================================================
    // Quartz configuration: register JobDetail + Trigger
    // =========================================================================

    @Configuration
    @Slf4j
    public static class QuartzJobConfig {

        @Bean
        public JobDetail reportJobDetail() {
            return JobBuilder.newJob(ReportGenerationJob.class)
                    .withIdentity("reportJob", "reports")
                    .withDescription("Daily sales report generation")
                    .usingJobData("reportType", "DAILY_SALES")
                    // storeDurably: keep job even if no triggers attached
                    .storeDurably()
                    .build();
        }

        @Bean
        public Trigger reportJobTrigger(JobDetail reportJobDetail) {
            // CronSchedule: fires every day at 2am (in production)
            // For tests, auto-startup=false so this never actually fires
            return TriggerBuilder.newTrigger()
                    .forJob(reportJobDetail)
                    .withIdentity("reportTrigger", "reports")
                    .withDescription("Fires daily at 02:00")
                    .withSchedule(
                            CronScheduleBuilder.cronSchedule("0 0 2 * * ?")
                                    // On misfire (app was down at 2am): fire once on restart
                                    .withMisfireHandlingInstructionFireAndProceed()
                    )
                    .build();
        }
    }
}
