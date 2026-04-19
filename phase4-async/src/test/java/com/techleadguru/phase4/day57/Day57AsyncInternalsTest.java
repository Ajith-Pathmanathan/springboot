package com.techleadguru.phase4.day57;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 57 — @Async Internals Test
 *
 * Verifies:
 * 1. void @Async fire-and-forget runs on a separate thread
 * 2. CompletableFuture @Async returns result asynchronously
 * 3. Self-invocation trap is documented (not actually async)
 * 4. REST endpoints work as expected
 */
@SpringBootTest(classes = Phase4Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day57AsyncInternalsTest {

    @Autowired MockMvc mockMvc;
    @Autowired Day57AsyncInternals.NotificationService notificationService;

    @Test
    void sendSmsAsync_returns_completable_future_with_result() throws Exception {
        CompletableFuture<String> future = notificationService.sendSmsAsync("+1234567890", "Hello");
        String result = future.get(3, TimeUnit.SECONDS);
        assertThat(result).isNotBlank();
        assertThat(result).startsWith("SMS-").endsWith("-OK");
    }

    @Test
    void sendSmsAsync_runs_on_different_thread() throws Exception {
        String callerThread = Thread.currentThread().getName();
        CompletableFuture<String> future = notificationService.sendSmsAsync("+9876543210", "Hi");
        String asyncThread = future.get(3, TimeUnit.SECONDS);
        // @Async runs on a pool thread, not the caller's thread
        assertThat(asyncThread).doesNotContain(callerThread);
    }

    @Test
    void sendEmailAsync_completes_without_blocking_caller() throws Exception {
        long start = System.currentTimeMillis();
        notificationService.sendEmailAsync("user@example.com", "Welcome");
        // Should return almost immediately — async dispatched
        assertThat(System.currentTimeMillis() - start).isLessThan(500);
    }

    @Test
    void notification_email_endpoint_returns_immediately() throws Exception {
        // void @Async — controller returns immediately without waiting for email to send
        mockMvc.perform(post("/api/day57/notifications/email")
                        .param("to", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("queued")));
    }

    @Test
    void notification_sms_endpoint_returns_future_result() throws Exception {
        // CompletableFuture @Async — Spring MVC dispatches asynchronously; needs two-step pattern
        var mvcResult = mockMvc.perform(post("/api/day57/notifications/sms")
                        .param("number", "+5555555555"))
                .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SMS")));
    }

    @Test
    void thread_info_endpoint_returns_thread_name() throws Exception {
        mockMvc.perform(get("/api/day57/notifications/thread-info"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Controller")));
    }
}
