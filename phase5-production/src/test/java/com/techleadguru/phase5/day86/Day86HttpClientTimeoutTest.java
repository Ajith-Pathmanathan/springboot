package com.techleadguru.phase5.day86;

import org.junit.jupiter.api.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;

class Day86HttpClientTimeoutTest {

    // ---- JavaHttpClientDemo ----

    @Test
    void withoutTimeout_client_has_no_timeout_set() {
        var client = Day86HttpClientTimeout.JavaHttpClientDemo.withoutTimeout();
        assertThat(client).isNotNull();
        // No connect timeout configured — just verify it's created
    }

    @Test
    void withTimeout_client_respects_duration() {
        var client = Day86HttpClientTimeout.JavaHttpClientDemo.withTimeout(Duration.ofSeconds(3));
        assertThat(client).isNotNull();
    }

    // ---- DownstreamCallerService ----

    @Test
    void callWithTimeout_succeeds_when_task_completes_in_time() throws Exception {
        Day86HttpClientTimeout.DownstreamCallerService svc =
                new Day86HttpClientTimeout.DownstreamCallerService();

        String result = svc.callWithTimeout(
                () -> "ok",
                Duration.ofSeconds(5)
        );
        assertThat(result).isEqualTo("ok");
        assertThat(svc.getSuccessCount()).isEqualTo(1);
        assertThat(svc.getTimeoutCount()).isEqualTo(0);
    }

    @Test
    void callWithTimeout_returns_fallback_on_timeout() throws Exception {
        Day86HttpClientTimeout.DownstreamCallerService svc =
                new Day86HttpClientTimeout.DownstreamCallerService();

        String result = svc.callWithTimeout(
                () -> {
                    Thread.sleep(2000); // 2s delay
                    return "too-late";
                },
                Duration.ofMillis(100) // 100ms timeout
        );
        // Returns fallback value on timeout
        assertThat(result).contains("FALLBACK");
        assertThat(svc.getTimeoutCount()).isEqualTo(1);
    }

    @Test
    void callWithTimeout_counts_successes_and_timeouts() throws Exception {
        Day86HttpClientTimeout.DownstreamCallerService svc =
                new Day86HttpClientTimeout.DownstreamCallerService();

        svc.callWithTimeout(() -> "fast", Duration.ofSeconds(5));
        svc.callWithTimeout(() -> "fast2", Duration.ofSeconds(5));
        try {
            svc.callWithTimeout(() -> { Thread.sleep(2000); return "slow"; }, Duration.ofMillis(50));
        } catch (Exception ignored) {}

        assertThat(svc.getSuccessCount()).isEqualTo(2);
        assertThat(svc.getTimeoutCount()).isEqualTo(1);
    }
}
