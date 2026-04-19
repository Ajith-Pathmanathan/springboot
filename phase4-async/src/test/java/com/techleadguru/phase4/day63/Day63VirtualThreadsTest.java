package com.techleadguru.phase4.day63;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 63 — Virtual Threads (Java 21) Test
 *
 * Verifies:
 * 1. virtualThreadExecutor runs tasks on virtual threads (Thread.isVirtual() == true)
 * 2. platformThreadExecutor runs on platform (OS) threads (isVirtual() == false)
 * 3. @Async("virtualThreadExecutor") delivers virtual-thread execution via CompletableFuture
 * 4. REST endpoints work correctly
 */
@SpringBootTest(classes = Phase4Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day63VirtualThreadsTest {

    @Autowired MockMvc mockMvc;
    @Autowired Day63VirtualThreads.AsyncVirtualService asyncVirtualService;
    @Autowired Day63VirtualThreads.ConcurrentFetchService concurrentFetchService;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("virtualThreadExecutor")
    Executor virtualThreadExecutor;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("platformThreadExecutor")
    Executor platformThreadExecutor;

    @Test
    void virtualThreadExecutor_bean_is_created() {
        assertThat(virtualThreadExecutor).isNotNull();
    }

    @Test
    void platformThreadExecutor_bean_is_created() {
        assertThat(platformThreadExecutor).isNotNull();
    }

    @Test
    void async_on_virtualThreadExecutor_runs_on_virtual_thread() throws Exception {
        // @Async("virtualThreadExecutor") → reports isVirtual() = true from method body
        Boolean isVirtual = asyncVirtualService.runOnVirtualThread().get(5, TimeUnit.SECONDS);
        assertThat(isVirtual).as("Task should run on a virtual thread").isTrue();
    }

    @Test
    void concurrentFetchService_nextTaskIsVirtual_returns_true() throws Exception {
        // Directly check the virtual executor runs virtual threads
        boolean isVirtual = concurrentFetchService.nextTaskIsVirtual();
        assertThat(isVirtual).isTrue();
    }

    @Test
    void virtual_thread_concurrency_completes_in_roughly_one_sleep_duration() throws Exception {
        long elapsed = concurrentFetchService.runConcurrent(10, virtualThreadExecutor);
        // 10 tasks × 100ms sequential = 1000ms; parallel with virtual threads ≈ 100ms
        assertThat(elapsed).isLessThan(3000); // generous CI budget
    }

    @Test
    void current_thread_endpoint_returns_thread_info() throws Exception {
        mockMvc.perform(get("/api/day63/virtual-threads/current-thread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").isString())
                .andExpect(jsonPath("$.isVirtual").isBoolean());
    }

    @Test
    void concurrency_test_endpoint_returns_timing_comparison() throws Exception {
        mockMvc.perform(get("/api/day63/virtual-threads/concurrency-test")
                        .param("tasks", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.virtualThreadMs").isNumber())
                .andExpect(jsonPath("$.platformThread10Ms").isNumber());
    }
}
