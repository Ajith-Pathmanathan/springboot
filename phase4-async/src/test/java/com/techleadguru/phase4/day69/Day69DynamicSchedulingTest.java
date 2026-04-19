package com.techleadguru.phase4.day69;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 69 — Dynamic Scheduling Test
 *
 * Verifies:
 * 1. Jobs can be scheduled and cancelled programmatically at runtime
 * 2. isScheduled() correctly reflects active/cancelled state
 * 3. Active job count reflects current state
 * 4. REST endpoints manage jobs correctly
 */
@SpringBootTest(classes = Phase4Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day69DynamicSchedulingTest {

    @Autowired MockMvc mockMvc;
    @Autowired Day69DynamicScheduling.DynamicReportScheduler dynamicScheduler;

    @Test
    void scheduleFixedRate_creates_active_job() {
        dynamicScheduler.scheduleFixedRate("test-job-1", () -> {}, Duration.ofHours(1));
        assertThat(dynamicScheduler.isScheduled("test-job-1")).isTrue();
        dynamicScheduler.cancel("test-job-1");
    }

    @Test
    void cancel_makes_job_inactive() {
        dynamicScheduler.scheduleFixedRate("test-job-2", () -> {}, Duration.ofHours(1));
        assertThat(dynamicScheduler.isScheduled("test-job-2")).isTrue();

        boolean cancelled = dynamicScheduler.cancel("test-job-2");
        assertThat(cancelled).isTrue();
        assertThat(dynamicScheduler.isScheduled("test-job-2")).isFalse();
    }

    @Test
    void cancel_nonexistent_job_returns_false() {
        boolean result = dynamicScheduler.cancel("nonexistent-job-xyz");
        assertThat(result).isFalse();
    }

    @Test
    void scheduleOnce_executes_task_at_given_instant() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Instant fireAt = Instant.now().plusMillis(200);

        dynamicScheduler.scheduleOnce("once-job", latch::countDown, fireAt);
        boolean fired = latch.await(2, TimeUnit.SECONDS);
        assertThat(fired).as("One-shot task should have fired").isTrue();
    }

    @Test
    void scheduleFixedRate_task_actually_runs() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch at_least_one = new CountDownLatch(1);

        // Schedule at 100ms interval so it fires quickly in tests
        dynamicScheduler.scheduleFixedRate("fast-job", () -> {
            counter.incrementAndGet();
            at_least_one.countDown();
        }, Duration.ofMillis(100));

        boolean ran = at_least_one.await(2, TimeUnit.SECONDS);
        assertThat(ran).isTrue();
        assertThat(counter.get()).isGreaterThanOrEqualTo(1);

        dynamicScheduler.cancel("fast-job");
    }

    @Test
    void rest_start_job_returns_started_status() throws Exception {
        mockMvc.perform(post("/api/day69/scheduler/jobs/my-report/start")
                        .param("intervalSeconds", "3600")) // 1 hour — won't fire during test
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("my-report"))
                .andExpect(jsonPath("$.status").value("STARTED"));

        // Clean up
        dynamicScheduler.cancel("my-report");
    }

    @Test
    void rest_cancel_job_returns_cancelled_true() throws Exception {
        dynamicScheduler.scheduleFixedRate("cancel-test", () -> {}, Duration.ofHours(1));

        mockMvc.perform(delete("/api/day69/scheduler/jobs/cancel-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled").value(true));
    }

    @Test
    void rest_job_status_reflects_active_state() throws Exception {
        dynamicScheduler.scheduleFixedRate("status-test", () -> {}, Duration.ofHours(1));

        mockMvc.perform(get("/api/day69/scheduler/jobs/status-test/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        dynamicScheduler.cancel("status-test");
    }

    @Test
    void rest_active_count_reflects_current_jobs() throws Exception {
        mockMvc.perform(get("/api/day69/scheduler/active-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeJobs").isNumber());
    }
}
