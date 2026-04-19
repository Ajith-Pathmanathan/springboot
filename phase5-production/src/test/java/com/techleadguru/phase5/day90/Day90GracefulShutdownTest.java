package com.techleadguru.phase5.day90;

import org.junit.jupiter.api.*;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.*;

class Day90GracefulShutdownTest {

    // ---- WorkDrainer ----

    @Test
    void drainer_accepts_tasks_initially() {
        Day90GracefulShutdown.WorkDrainer drainer = new Day90GracefulShutdown.WorkDrainer(2);
        assertThat(drainer.isAccepting()).isTrue();
    }

    @Test
    void drainer_completes_submitted_tasks() throws Exception {
        Day90GracefulShutdown.WorkDrainer drainer = new Day90GracefulShutdown.WorkDrainer(2);
        boolean[] ran = {false};
        drainer.submit(() -> ran[0] = true);
        boolean drained = drainer.drain(2000);
        assertThat(drained).isTrue();
        assertThat(ran[0]).isTrue();
    }

    @Test
    void drainer_stops_accepting_after_drain_called() throws Exception {
        Day90GracefulShutdown.WorkDrainer drainer = new Day90GracefulShutdown.WorkDrainer(2);
        drainer.drain(1000);
        assertThat(drainer.isAccepting()).isFalse();
        assertThatThrownBy(() -> drainer.submit(() -> {}))
                .isInstanceOf(IllegalStateException.class);
    }

    // ---- ShutdownConfigAuditor ----

    @Test
    void shutdownConfigAuditor_recommended_config_has_graceful_enabled() {
        Day90GracefulShutdown.ShutdownConfigAuditor auditor =
                new Day90GracefulShutdown.ShutdownConfigAuditor();
        var config = auditor.getRecommendedConfig();
        assertThat(config.gracefulEnabled()).isTrue();
        assertThat(config.asyncWaitsForTasks()).isTrue();
        assertThat(config.asyncTerminationSecs()).isEqualTo(30);
    }

    @Test
    void shutdownConfigAuditor_config_snippet_contains_key_settings() {
        Day90GracefulShutdown.ShutdownConfigAuditor auditor =
                new Day90GracefulShutdown.ShutdownConfigAuditor();
        String snippet = auditor.configSnippet();
        assertThat(snippet).contains("server.shutdown=graceful");
        assertThat(snippet).contains("timeout-per-shutdown-phase");
    }
}
