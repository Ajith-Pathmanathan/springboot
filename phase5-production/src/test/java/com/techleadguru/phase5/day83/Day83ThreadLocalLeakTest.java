package com.techleadguru.phase5.day83;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day83ThreadLocalLeakTest {

    @BeforeEach
    void cleanUp() {
        Day83ThreadLocalLeak.RequestContextLeaky.clear();
    }

    // ---- RequestContextLeaky ----

    @Test
    void leaky_setContext_makes_context_available() {
        Day83ThreadLocalLeak.RequestContextLeaky.setContext("user-1", "tenant-A");
        assertThat(Day83ThreadLocalLeak.RequestContextLeaky.getUserId()).isEqualTo("user-1");
        assertThat(Day83ThreadLocalLeak.RequestContextLeaky.getTenantId()).isEqualTo("tenant-A");
    }

    @Test
    void leaky_hasContext_true_after_set() {
        Day83ThreadLocalLeak.RequestContextLeaky.setContext("u", "t");
        assertThat(Day83ThreadLocalLeak.RequestContextLeaky.hasContext()).isTrue();
    }

    @Test
    void leaky_clear_removes_context() {
        Day83ThreadLocalLeak.RequestContextLeaky.setContext("u", "t");
        Day83ThreadLocalLeak.RequestContextLeaky.clear();
        assertThat(Day83ThreadLocalLeak.RequestContextLeaky.hasContext()).isFalse();
    }

    @Test
    void leaky_getUserId_returns_sentinel_when_no_context() {
        // getUserId() returns "NOT_SET" (not null) when no context is set
        assertThat(Day83ThreadLocalLeak.RequestContextLeaky.getUserId()).isEqualTo("NOT_SET");
    }

    // ---- RequestContextSafe ----

    @Test
    void safe_withContext_executes_task() throws Exception {
        String result = Day83ThreadLocalLeak.RequestContextSafe.withContext(
                "user-2", "tenant-B", () -> {
                    assertThat(Day83ThreadLocalLeak.RequestContextSafe.getUserId()).isEqualTo("user-2");
                    assertThat(Day83ThreadLocalLeak.RequestContextSafe.getTenantId()).isEqualTo("tenant-B");
                    return "done";
                });
        assertThat(result).isEqualTo("done");
    }

    @Test
    void safe_withContext_cleans_up_after_task() throws Exception {
        Day83ThreadLocalLeak.RequestContextSafe.withContext("u", "t", () -> "x");
        assertThat(Day83ThreadLocalLeak.RequestContextSafe.hasContext()).isFalse();
    }

    @Test
    void safe_withContext_cleans_up_even_on_exception() {
        assertThatThrownBy(() ->
                Day83ThreadLocalLeak.RequestContextSafe.withContext("u", "t", () -> {
                    throw new RuntimeException("boom");
                })
        ).hasMessageContaining("boom");

        assertThat(Day83ThreadLocalLeak.RequestContextSafe.hasContext()).isFalse();
    }
}
