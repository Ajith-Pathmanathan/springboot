package com.techleadguru.phase4.day61;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * DAY 61 — @Async + SecurityContext Trap ⚠️
 *
 * THE TRAP:
 *   Spring Security stores the authenticated user in SecurityContextHolder,
 *   which uses a ThreadLocal by default.
 *
 *   ThreadLocals are NOT inherited by child threads.
 *   @Async spawns a task on a DIFFERENT thread → SecurityContext is NULL on that thread.
 *
 *   Result: your async method tries to call SecurityContextHolder.getContext()
 *           and gets an empty Authentication → NullPointerException or wrong behavior.
 *
 *   This is a SILENT bug: synchronous code works fine, async code crashes
 *   or returns wrong user — hard to reproduce locally, dangerous in production.
 *
 * REPRODUCTION:
 *   Thread A (HTTP thread): SecurityContextHolder.getContext().getAuthentication() = "alice"
 *   Thread B (@Async thread): SecurityContextHolder.getContext().getAuthentication() = NULL
 *
 * FIX 1 — SecurityContextTaskDecorator (recommended):
 *   Install a TaskDecorator that captures SecurityContext from caller thread
 *   and restores it on the async thread before execution.
 *
 *   executor.setTaskDecorator(new SecurityContextTaskDecorator());
 *
 * FIX 2 — Pass user explicitly:
 *   Instead of relying on SecurityContext, pass the username/userId as a parameter.
 *   Pros: explicit, testable. Cons: pollutes method signatures.
 *
 * FIX 3 — Spring Security DelegatingSecurityContextAsyncTaskExecutor:
 *   Wraps an executor to propagate SecurityContext automatically.
 *
 * SAME PROBLEM APPLIES TO:
 *   - MDC (Mapped Diagnostic Context) — Day 48 mentioned AsyncTaskDecorator for MDC
 *   - RequestContextHolder (HttpServletRequest access in async)
 *   - Tenant context in multi-tenant systems
 */
@Slf4j
public class Day61AsyncSecurityContext {

    // =========================================================================
    // TaskDecorator that propagates SecurityContext to async threads
    // =========================================================================

    public static class SecurityContextPropagatingDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(Runnable runnable) {
            // Capture SecurityContext from the CALLING thread (HTTP/main thread)
            SecurityContext callerContext = SecurityContextHolder.getContext();
            return () -> {
                try {
                    // Restore captured context on the ASYNC thread
                    SecurityContextHolder.setContext(callerContext);
                    runnable.run();
                } finally {
                    // CRITICAL: clear after task completes — thread returns to pool
                    // Without this, next task on this thread inherits wrong context
                    SecurityContextHolder.clearContext();
                }
            };
        }
    }

    // =========================================================================
    // Executor config: BROKEN vs FIXED version
    // =========================================================================

    @Configuration
    public static class AsyncSecurityConfig {

        /**
         * BROKEN: No TaskDecorator → SecurityContext is NULL on async thread.
         */
        @Bean("brokenExecutor")
        public Executor brokenExecutor() {
            ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
            exec.setCorePoolSize(4);
            exec.setThreadNamePrefix("broken-async-");
            exec.initialize();
            return exec;
        }

        /**
         * FIXED: TaskDecorator propagates SecurityContext to async threads.
         */
        @Bean("fixedExecutor")
        public Executor fixedExecutor() {
            ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
            exec.setCorePoolSize(4);
            exec.setThreadNamePrefix("fixed-async-");
            exec.setTaskDecorator(new SecurityContextPropagatingDecorator());
            exec.initialize();
            return exec;
        }
    }

    // =========================================================================
    // Service demonstrating the broken vs fixed behaviour
    // =========================================================================

    @Service
    @Slf4j
    public static class AuditService {

        /**
         * BROKEN: uses @Async without context propagation.
         * Authentication will be null here when called from an HTTP request.
         */
        @Async("brokenExecutor")
        public CompletableFuture<String> auditBroken(String action) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null) ? auth.getName() : "ANONYMOUS/NULL";
            log.warn("[Day61] BROKEN audit: user={} (should not be ANONYMOUS)", user);
            return CompletableFuture.completedFuture(user);
        }

        /**
         * FIXED: uses @Async with executor that has SecurityContextPropagatingDecorator.
         * SecurityContext is copied to the async thread — auth is correct.
         */
        @Async("fixedExecutor")
        public CompletableFuture<String> auditFixed(String action) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null) ? auth.getName() : "ANONYMOUS/NULL";
            log.info("[Day61] FIXED audit: user={}", user);
            return CompletableFuture.completedFuture(user);
        }
    }
}
