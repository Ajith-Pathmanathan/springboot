package com.techleadguru.phase4.day57;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * DAY 57 — @Async Internals: AOP Proxy + TaskExecutor
 *
 * HOW @Async WORKS (under the hood):
 *   1. Spring wraps the calling bean in a CGLIB proxy at startup.
 *   2. When external code calls proxy.doWork(), the proxy intercepts the call.
 *   3. The proxy submits a Runnable to Spring's TaskExecutor (thread pool).
 *   4. The Runnable calls the REAL method on the target object.
 *   5. The original caller thread returns immediately with a Future/void.
 *
 * REQUIREMENTS:
 *   a) @EnableAsync on a @Configuration class (or @SpringBootApplication)
 *   b) The calling code must call the method THROUGH THE PROXY.
 *      → calling this.doWork() from another method in the SAME class = bypasses proxy!
 *      → This is the same self-invocation trap as @Transactional (see Phase 1 Day 19)
 *   c) Method must be on a Spring bean (@Component, @Service, etc.)
 *   d) Method must be public (CGLIB cannot proxy private/package-private)
 *
 * RETURN TYPES:
 *   void                   → fire-and-forget; exceptions silently swallowed (Day 60!)
 *   Future<T>              → legacy, use AsyncResult.forValue(x)
 *   CompletableFuture<T>   → modern, composable, recommended
 *   ListenableFuture<T>    → deprecated in Spring 6
 *
 * DEFAULT EXECUTOR:
 *   Spring uses SimpleAsyncTaskExecutor by default — creates a NEW THREAD per call!
 *   → For production: define a ThreadPoolTaskExecutor bean (see Day 58)
 *   → Or configure spring.task.execution.* properties (auto-configured by Boot)
 */
@Slf4j
public class Day57AsyncInternals {

    // =========================================================================
    // Async service: demonstrates @Async with different return types
    // =========================================================================

    @Service
    @Slf4j
    public static class NotificationService {

        /**
         * void @Async — fire and forget.
         * TRAP: exceptions thrown here are silently dropped unless you handle them.
         */
        @Async
        public void sendEmailAsync(String to, String subject) {
            log.info("[Day57] Sending email to {} on thread {}", to, Thread.currentThread().getName());
            try {
                Thread.sleep(100); // simulate SMTP latency
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("[Day57] Email sent to {}", to);
        }

        /**
         * CompletableFuture @Async — caller can chain, join, or get().
         * Spring wraps this in a CompletableFuture automatically.
         * If the method throws, future.get() throws ExecutionException wrapping it.
         */
        @Async
        public CompletableFuture<String> sendSmsAsync(String number, String message) {
            log.info("[Day57] Sending SMS to {} on thread {}", number, Thread.currentThread().getName());
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return CompletableFuture.completedFuture("SMS-" + number + "-OK");
        }

        /**
         * SELF-INVOCATION TRAP — this method calls sendEmailAsync() on 'this'.
         * Since 'this' is the real object (not the CGLIB proxy), @Async is bypassed!
         * The sendEmailAsync call runs SYNCHRONOUSLY on the calling thread.
         * Solution: inject NotificationService into itself, or refactor to separate bean.
         */
        public void triggerEmailSelfInvocation(String to) {
            log.warn("[Day57] SELF-INVOCATION: @Async will be IGNORED here!");
            sendEmailAsync(to, "Self-invocation test"); // runs on THIS thread, not async
        }
    }

    // =========================================================================
    // REST controller to trigger async operations
    // =========================================================================

    @RestController
    @RequestMapping("/api/day57/notifications")
    @Slf4j
    public static class NotificationController {

        private final NotificationService notificationService;

        public NotificationController(NotificationService service) {
            this.notificationService = service;
        }

        @PostMapping("/email")
        public String triggerEmail(@RequestParam String to) {
            log.info("[Day57] Controller thread: {} — returning immediately", Thread.currentThread().getName());
            notificationService.sendEmailAsync(to, "Welcome!");
            return "Email queued for " + to; // returns before email is sent
        }

        @PostMapping("/sms")
        public CompletableFuture<String> triggerSms(@RequestParam String number) {
            // Returns CompletableFuture to Spring MVC — it will await the result
            return notificationService.sendSmsAsync(number, "Your OTP is 1234");
        }

        @GetMapping("/thread-info")
        public String threadInfo() {
            return "Controller running on: " + Thread.currentThread().getName()
                    + " | virtual=" + Thread.currentThread().isVirtual();
        }
    }
}
