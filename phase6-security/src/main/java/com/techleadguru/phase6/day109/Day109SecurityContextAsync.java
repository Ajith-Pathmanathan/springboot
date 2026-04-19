package com.techleadguru.phase6.day109;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Day 109 — SecurityContextHolder in @Async ⚠️
 *
 * The problem:
 *   SecurityContextHolder defaults to MODE_THREADLOCAL.
 *   @Async methods run in a different thread → SecurityContext is LOST.
 *
 * Solutions:
 *   1. MODE_INHERITABLETHREADLOCAL  — inherits from parent thread (works for simple cases)
 *   2. DelegatingSecurityContextExecutor — wraps TaskExecutor to propagate context
 *   3. SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL) — global setting
 *
 * Best practice: use DelegatingSecurityContextExecutor in @Async configuration.
 */
public class Day109SecurityContextAsync {

    // ─────────────────────────────────────────────────────────────────────────
    // SecurityContextMode enum
    // ─────────────────────────────────────────────────────────────────────────

    public enum SecurityContextMode {
        /** Default — each thread gets its own SecurityContext (breaks @Async). */
        MODE_THREADLOCAL,
        /** Child threads inherit the parent's SecurityContext (fragile with thread pools). */
        MODE_INHERITABLETHREADLOCAL,
        /** Wraps Executor to explicitly copy SecurityContext before task runs (recommended). */
        DELEGATING_SECURITY_CONTEXT_EXECUTOR
    }

    /** Returns the recommended mode for @Async methods. */
    public static SecurityContextMode recommendedAsyncMode() {
        return SecurityContextMode.DELEGATING_SECURITY_CONTEXT_EXECUTOR;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SecurityContextCapture — snapshot the current context into another thread
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Wraps an Executor so each submitted task runs with the current thread's
     * SecurityContext. Equivalent to what DelegatingSecurityContextExecutor does.
     */
    public static Executor createSecurityAwareExecutor(Executor delegate) {
        // In real code: return new DelegatingSecurityContextExecutor(delegate);
        // We replicate the idea here for visibility:
        return runnable -> {
            SecurityContext ctx = SecurityContextHolder.getContext();
            delegate.execute(() -> {
                SecurityContextHolder.setContext(ctx);
                try {
                    runnable.run();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ContextPropagationDemo — shows the context-loss problem
    // ─────────────────────────────────────────────────────────────────────────

    public static class ContextPropagationDemo {

        /**
         * Runs a task in the given executor and returns the username the task saw.
         * With a MODE_THREADLOCAL executor, the task gets null username.
         * With a security-aware executor, the task sees the caller's username.
         */
        public String getUsernameInTask(Executor executor, Authentication auth) throws Exception {
            // Set up caller context
            SecurityContext callerCtx = SecurityContextHolder.createEmptyContext();
            callerCtx.setAuthentication(auth);
            SecurityContextHolder.setContext(callerCtx);

            try {
                Future<String>[] result = new Future[1];
                // Run task
                executor.execute(() -> {
                    Authentication a = SecurityContextHolder.getContext().getAuthentication();
                    result[0] = java.util.concurrent.CompletableFuture.completedFuture(
                            a != null ? a.getName() : "NO_CONTEXT"
                    );
                });
                Thread.sleep(50); // let task run
                return result[0] != null ? result[0].get() : "NO_CONTEXT";
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Configuration guide
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns the configuration steps to fix SecurityContext propagation in @Async. */
    public static List<String> fixSteps() {
        return List.of(
            "1. Create a DelegatingSecurityContextExecutor bean that wraps your TaskExecutor",
            "2. Annotate @AsyncConfigurer and return the wrapping executor from getAsyncExecutor()",
            "3. Alternatively set SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL)",
            "4. In tests inject mocked authentication with @WithMockUser or @WithSecurityContext"
        );
    }
}
