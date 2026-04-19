package com.techleadguru.phase4.day69;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * DAY 69 — Dynamic Scheduling at Runtime
 *
 * @SCHEDULED LIMITATION:
 *   The cron expression is fixed at compile time (or read once from properties at startup).
 *   You CANNOT change a cron schedule without restarting the application.
 *
 * DYNAMIC SCHEDULING:
 *   Use TaskScheduler programmatically — schedule, cancel, and reschedule at runtime.
 *   TaskScheduler is the low-level API backing @Scheduled.
 *
 *   Common use cases:
 *   - User-configurable schedules ("send report every Monday at 8am" — user can change)
 *   - A/B testing: different schedules per environment
 *   - Feature flags: enable/disable a task without restart
 *   - Adaptive scheduling: slow down polling when backlog is low
 *
 * KEY APIs:
 *   taskScheduler.schedule(Runnable, Instant)                   → run once at instant
 *   taskScheduler.scheduleAtFixedRate(Runnable, Duration)       → fixed rate from now
 *   taskScheduler.scheduleWithFixedDelay(Runnable, Duration)    → fixed delay from now
 *   taskScheduler.schedule(Runnable, Trigger)                   → cron trigger
 *
 * CANCELLATION:
 *   ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(...);
 *   future.cancel(false); // false = don't interrupt if running
 *
 * RESCHEDULE PATTERN:
 *   1. Cancel existing future.
 *   2. Schedule new future with updated interval/cron.
 *   3. Store the new future reference.
 */
@Slf4j
public class Day69DynamicScheduling {

    // =========================================================================
    // TaskScheduler bean
    // =========================================================================

    @Configuration
    @EnableScheduling
    public static class SchedulerConfig {
        @org.springframework.context.annotation.Bean
        public TaskScheduler taskScheduler() {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(4);
            scheduler.setThreadNamePrefix("dyn-sched-");
            scheduler.initialize();
            return scheduler;
        }
    }

    // =========================================================================
    // Dynamic scheduling service
    // =========================================================================

    @Service
    @Slf4j
    public static class DynamicReportScheduler {

        private final TaskScheduler taskScheduler;
        // Map of jobId → ScheduledFuture (allows cancellation)
        private final Map<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

        public DynamicReportScheduler(TaskScheduler taskScheduler) {
            this.taskScheduler = taskScheduler;
        }

        /**
         * Schedule a recurring task at a fixed rate (run immediately, then every N seconds).
         * Returns the jobId which can be used to cancel later.
         */
        public String scheduleFixedRate(String jobId, Runnable task, Duration interval) {
            // Cancel existing job with same ID if present
            cancel(jobId);
            ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(task, interval);
            scheduledJobs.put(jobId, future);
            log.info("[Day69] Scheduled job '{}' at fixedRate={}", jobId, interval);
            return jobId;
        }

        /**
         * Schedule using a cron expression — can be changed at runtime by calling this again.
         */
        public String scheduleCron(String jobId, Runnable task, String cronExpression) {
            cancel(jobId);
            ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(cronExpression));
            scheduledJobs.put(jobId, future);
            log.info("[Day69] Scheduled job '{}' with cron='{}'", jobId, cronExpression);
            return jobId;
        }

        /**
         * Schedule a one-time task at a specific instant.
         */
        public String scheduleOnce(String jobId, Runnable task, Instant at) {
            cancel(jobId);
            ScheduledFuture<?> future = taskScheduler.schedule(task, at);
            scheduledJobs.put(jobId, future);
            log.info("[Day69] Scheduled job '{}' once at {}", jobId, at);
            return jobId;
        }

        /**
         * Cancel a scheduled job. Returns true if it was found and cancelled.
         */
        public boolean cancel(String jobId) {
            ScheduledFuture<?> existing = scheduledJobs.remove(jobId);
            if (existing != null && !existing.isCancelled() && !existing.isDone()) {
                existing.cancel(false);
                log.info("[Day69] Cancelled job '{}'", jobId);
                return true;
            }
            return false;
        }

        public boolean isScheduled(String jobId) {
            ScheduledFuture<?> f = scheduledJobs.get(jobId);
            return f != null && !f.isCancelled() && !f.isDone();
        }

        public int activeJobCount() {
            return (int) scheduledJobs.values().stream()
                    .filter(f -> !f.isCancelled() && !f.isDone())
                    .count();
        }
    }

    // =========================================================================
    // REST API for dynamic scheduling management
    // =========================================================================

    @RestController
    @RequestMapping("/api/day69/scheduler")
    @Slf4j
    public static class SchedulerController {

        private final DynamicReportScheduler scheduler;
        private final Map<String, Integer> runCounts = new ConcurrentHashMap<>();

        public SchedulerController(DynamicReportScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @PostMapping("/jobs/{id}/start")
        public Map<String, Object> startJob(@PathVariable String id,
                                            @RequestParam(defaultValue = "5") long intervalSeconds) {
            runCounts.putIfAbsent(id, 0);
            scheduler.scheduleFixedRate(id, () -> {
                runCounts.merge(id, 1, Integer::sum);
                log.info("[Day69] Job '{}' run #{}", id, runCounts.get(id));
            }, Duration.ofSeconds(intervalSeconds));
            return Map.of("jobId", id, "status", "STARTED", "intervalSeconds", intervalSeconds);
        }

        @DeleteMapping("/jobs/{id}")
        public Map<String, Object> cancelJob(@PathVariable String id) {
            boolean cancelled = scheduler.cancel(id);
            return Map.of("jobId", id, "cancelled", cancelled);
        }

        @GetMapping("/jobs/{id}/status")
        public Map<String, Object> jobStatus(@PathVariable String id) {
            return Map.of(
                    "jobId", id,
                    "active", scheduler.isScheduled(id),
                    "runCount", runCounts.getOrDefault(id, 0)
            );
        }

        @GetMapping("/active-count")
        public Map<String, Integer> activeCount() {
            return Map.of("activeJobs", scheduler.activeJobCount());
        }
    }
}
