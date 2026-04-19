package com.techleadguru.phase5.day103;

import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class Day103BulkheadPatternTest {

    @Test
    void fastLane_completes_tasks_when_submitted() throws Exception {
        var service = new Day103BulkheadPattern.BulkheadService(2, 2, 10);
        var future = service.executeInFastLane(() -> "fast-result");
        assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("fast-result");
        service.shutdown();
    }

    @Test
    void slowLane_completes_tasks_when_submitted() throws Exception {
        var service = new Day103BulkheadPattern.BulkheadService(2, 2, 10);
        var future = service.executeInSlowLane(() -> "slow-result");
        assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("slow-result");
        service.shutdown();
    }

    @Test
    void bulkheadStats_tracks_succeeded_counts() throws Exception {
        var service = new Day103BulkheadPattern.BulkheadService(2, 2, 10);
        service.executeInFastLane(() -> "a").get(5, TimeUnit.SECONDS);
        service.executeInFastLane(() -> "b").get(5, TimeUnit.SECONDS);
        service.executeInSlowLane(() -> "c").get(5, TimeUnit.SECONDS);

        var stats = service.getStats();
        assertThat(stats.fastSucceeded()).isEqualTo(2);
        assertThat(stats.slowSucceeded()).isEqualTo(1);
        service.shutdown();
    }

    @Test
    void fastLane_rejects_when_pool_and_queue_full() throws InterruptedException {
        // Very small pool + queue to force rejection quickly
        var service = new Day103BulkheadPattern.BulkheadService(1, 1, 1);
        CountDownLatch blockLatch = new CountDownLatch(1);
        CountDownLatch startedLatch = new CountDownLatch(1);

        // Fill the pool thread
        service.executeInFastLane(() -> {
            startedLatch.countDown();
            try { blockLatch.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            return "held";
        });
        startedLatch.await(5, TimeUnit.SECONDS); // ensure pool thread is running

        // Fill the queue
        service.executeInFastLane(() -> "queue-filler");

        // Next task should be rejected
        var rejectedFuture = service.executeInFastLane(() -> "rejected");
        assertThat(rejectedFuture.isCompletedExceptionally() ||
                rejectedFuture.exceptionNow() != null ||
                isExceptional(rejectedFuture)).isTrue();

        blockLatch.countDown();
        service.shutdown();
    }

    @Test
    void bulkheadSimulation_fastLane_isolated_when_slow_saturated() throws Exception {
        var service = new Day103BulkheadPattern.BulkheadService(4, 1, 2);
        var result = Day103BulkheadPattern.BulkheadSimulation.run(service, 5, 4, 200);
        assertThat(result.fastLaneIsolated()).isTrue();
        assertThat(result.fastCompleted()).isGreaterThan(0);
        service.shutdown();
    }

    private boolean isExceptional(CompletableFuture<?> future) {
        try {
            future.get(2, TimeUnit.SECONDS);
            return future.isCompletedExceptionally();
        } catch (Exception e) {
            return true;
        }
    }
}
