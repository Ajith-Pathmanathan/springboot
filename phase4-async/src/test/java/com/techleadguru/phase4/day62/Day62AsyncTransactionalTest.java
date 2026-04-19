package com.techleadguru.phase4.day62;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 62 — @Async + @Transactional Separate Transaction Test
 *
 * Verifies:
 * 1. @Async @Transactional method runs in its OWN transaction (not caller's)
 * 2. isTransactionActiveAsync() reports true (proves TX is active on async thread)
 * 3. placeOrderCorrectPattern triggers the afterCommit hook
 */
@SpringBootTest(classes = Phase4Application.class)
class Day62AsyncTransactionalTest {

    @Autowired Day62AsyncTransactional.OrderProcessingService orderService;

    @Test
    void processOrderAsync_has_own_transaction() throws Exception {
        CompletableFuture<String> future = orderService.processOrderAsync("ORD-100");
        String result = future.get(5, TimeUnit.SECONDS);
        // inTx=true proves the @Transactional annotation created a new TX on the async thread
        assertThat(result).contains("inTx=true");
    }

    @Test
    void processOrderAsync_runs_on_different_thread() throws Exception {
        String callerThread = Thread.currentThread().getName();
        CompletableFuture<String> future = orderService.processOrderAsync("ORD-101");
        String result = future.get(5, TimeUnit.SECONDS);
        // Result contains the async thread name
        assertThat(result).doesNotContain(callerThread);
    }

    @Test
    void isTransactionActiveAsync_returns_true() throws Exception {
        Boolean active = orderService.isTransactionActiveAsync().get(5, TimeUnit.SECONDS);
        // @Transactional on @Async method creates a new TX on the async thread
        assertThat(active).isTrue();
    }

    @Test
    void placeOrderBadPattern_executes_async_within_open_transaction() throws Exception {
        // Bad pattern: async is called before caller's TX commits
        // The returned future will complete (async TX is independent), but the
        // async thread won't see caller's uncommitted data
        CompletableFuture<String> future = orderService.placeOrderBadPattern("ORD-BAD");
        String result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).contains("processed-ORD-BAD");
    }

    @Test
    void placeOrderCorrectPattern_fires_afterCommit_hook() throws Exception {
        AtomicBoolean hookFired = new AtomicBoolean(false);
        CompletableFuture<Void> hookDone = new CompletableFuture<>();

        // The afterCommit hook is triggered AFTER the @Transactional method commits
        orderService.placeOrderCorrectPattern("ORD-CORRECT", () -> {
            hookFired.set(true);
            hookDone.complete(null);
        });

        // Wait for hook to fire (it fires asynchronously after commit)
        hookDone.get(5, TimeUnit.SECONDS);
        assertThat(hookFired.get()).as("afterCommit hook should have fired").isTrue();
    }
}
