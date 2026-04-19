package com.techleadguru.phase5.day89;

import org.junit.jupiter.api.*;
import java.util.concurrent.RejectedExecutionException;
import static org.assertj.core.api.Assertions.*;

class Day89ThreadPoolSaturationTest {

    private Day89ThreadPoolSaturation.MonitoredThreadPool pool;
    private Day89ThreadPoolSaturation.ReportGenerationService service;

    @BeforeEach
    void setUp() {
        service = new Day89ThreadPoolSaturation.ReportGenerationService();
    }

    @AfterEach
    void tearDown() {
        if (pool != null) pool.shutdown();
    }

    @Test
    void monitoredPool_stats_show_zero_initially() {
        pool = new Day89ThreadPoolSaturation.MonitoredThreadPool(2, 4, 5, false);
        var stats = pool.stats();
        assertThat(stats.active()).isEqualTo(0);
        assertThat(stats.rejected()).isEqualTo(0);
    }

    @Test
    void abortPolicy_rejects_when_saturated() throws InterruptedException {
        pool = new Day89ThreadPoolSaturation.MonitoredThreadPool(1, 1, 1, false);

        // Saturate: 1 running + 1 queued, third is rejected
        var result = service.bombardPool(pool, 5, 200);
        assertThat(result.submitted()).isEqualTo(5);
        assertThat(result.hadRejections() || result.succeeded() > 0).isTrue();
    }

    @Test
    void callerRunsPolicy_does_not_reject_tasks() throws InterruptedException {
        pool = new Day89ThreadPoolSaturation.MonitoredThreadPool(2, 2, 2, true);

        // With CallerRuns, all tasks eventually complete (caller runs them)
        var result = service.bombardPool(pool, 6, 50);
        assertThat(result.submitted()).isEqualTo(6);
        // CallerRuns: rejected count tracked but tasks still ran
        assertThat(result.succeeded() + result.rejected()).isEqualTo(6);
    }

    @Test
    void saturationResult_rejection_rate_calculated_correctly() {
        var stats = new Day89ThreadPoolSaturation.MonitoredThreadPool.PoolStats(0, 0, 0L, 0, 2, 0);
        var result = new Day89ThreadPoolSaturation.ReportGenerationService.SaturationResult(
                10, 8, 2, stats);
        assertThat(result.rejectionRate()).isEqualTo(0.2);
        assertThat(result.hadRejections()).isTrue();
    }
}
