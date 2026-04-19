package com.techleadguru.phase6.day109;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class Day109SecurityContextAsyncTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recommendedAsyncMode_is_delegating_security_context_executor() {
        assertThat(Day109SecurityContextAsync.recommendedAsyncMode())
                .isEqualTo(Day109SecurityContextAsync.SecurityContextMode.DELEGATING_SECURITY_CONTEXT_EXECUTOR);
    }

    @Test
    void fixSteps_has_4_steps() {
        assertThat(Day109SecurityContextAsync.fixSteps()).hasSize(4);
    }

    @Test
    void fixSteps_first_step_mentions_delegating() {
        assertThat(Day109SecurityContextAsync.fixSteps().getFirst())
                .containsIgnoringCase("Delegating");
    }

    @Test
    void securityContextMode_enum_has_3_values() {
        assertThat(Day109SecurityContextAsync.SecurityContextMode.values()).hasSize(3);
    }

    @Test
    void createSecurityAwareExecutor_propagates_context_to_task() throws Exception {
        // Set up a SecurityContext in the current thread
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        ExecutorService raw = Executors.newSingleThreadExecutor();
        var safeExecutor = Day109SecurityContextAsync.createSecurityAwareExecutor(raw);

        AtomicReference<String> capturedUsername = new AtomicReference<>("NOT_SET");

        safeExecutor.execute(() -> {
            var taskAuth = SecurityContextHolder.getContext().getAuthentication();
            capturedUsername.set(taskAuth != null ? taskAuth.getName() : "NO_CONTEXT");
        });

        raw.shutdown();
        raw.awaitTermination(2, TimeUnit.SECONDS);

        assertThat(capturedUsername.get()).isEqualTo("alice");
    }

    @Test
    void naive_executor_loses_context() throws Exception {
        // Set up a SecurityContext in the current thread
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "bob", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        ExecutorService raw = Executors.newSingleThreadExecutor();
        AtomicReference<String> capturedUsername = new AtomicReference<>("NOT_SET");

        // Submit WITHOUT context propagation — the task won't see the context
        raw.execute(() -> {
            var taskAuth = SecurityContextHolder.getContext().getAuthentication();
            capturedUsername.set(taskAuth != null ? taskAuth.getName() : "NO_CONTEXT");
        });

        raw.shutdown();
        raw.awaitTermination(2, TimeUnit.SECONDS);

        // Without propagation the task sees NO_CONTEXT (demonstrates the problem)
        assertThat(capturedUsername.get()).isEqualTo("NO_CONTEXT");
    }
}
