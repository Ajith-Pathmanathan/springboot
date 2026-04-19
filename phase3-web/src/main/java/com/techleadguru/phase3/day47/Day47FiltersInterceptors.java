package com.techleadguru.phase3.day47;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DAY 47 — Filters vs Interceptors: When to Use Which
 *
 * THREE LAYERS that can intercept every HTTP request:
 *
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │  HTTP Request                                                │
 *   │    ↓                                                         │
 *   │  [Servlet Filter]  ← javax/jakarta.servlet, wraps Request   │
 *   │    ↓                                                         │
 *   │  [DispatcherServlet]                                         │
 *   │    ↓                                                         │
 *   │  [HandlerInterceptor.preHandle]  ← Spring MVC               │
 *   │    ↓                                                         │
 *   │  [Controller Method]                                         │
 *   │    ↑                                                         │
 *   │  [HandlerInterceptor.postHandle]  ← has ModelAndView        │
 *   │    ↑                                                         │
 *   │  [HandlerInterceptor.afterCompletion]  ← always runs        │
 *   │    ↑                                                         │
 *   │  [Servlet Filter]  ← wraps entire response                  │
 *   └──────────────────────────────────────────────────────────────┘
 *
 * WHEN TO USE EACH:
 *
 *   Filter (OncePerRequestFilter):
 *     ✓ Cross-cutting: authentication, CORS, request logging, compression
 *     ✓ Access to raw Request/Response streams before Spring processes them
 *     ✓ Can apply to non-Spring requests (static resources, actuator)
 *     ✓ Can short-circuit the entire chain (return early, e.g., 401)
 *     ✗ No Spring context (no @Autowired) unless using Spring-managed filter
 *
 *   HandlerInterceptor:
 *     ✓ Spring-aware: can access handler method info, model attributes
 *     ✓ postHandle: modify ModelAndView (useful for server-side rendering)
 *     ✓ afterCompletion: cleanup after response committed (close resources)
 *     ✓ Can be wired with full Spring DI
 *     ✗ Does NOT wrap raw byte streams
 *     ✗ Does NOT apply to static resources (only DispatcherServlet requests)
 *
 *   AOP @Around (covered in Phase 1):
 *     ✓ Service/repository layer — not just HTTP
 *     ✓ Method-level granularity with @annotation pointcuts
 *     ✗ Operates on Spring proxy, NOT on raw request/response
 */
@Slf4j
public class Day47FiltersInterceptors {

    // =========================================================================
    // Execution tracker — records the call sequence for testing
    // =========================================================================

    @Component
    public static class ExecutionTracker {
        // ThreadLocal so parallel test runs don't interfere
        private final ThreadLocal<List<String>> steps = ThreadLocal.withInitial(ArrayList::new);

        public void record(String step) {
            steps.get().add(step);
            log.debug("[Day47] Recorded step: {}", step);
        }

        public List<String> getSteps() {
            return Collections.unmodifiableList(steps.get());
        }

        public void clear() {
            steps.get().clear();
        }
    }

    // =========================================================================
    // Filter — runs at Servlet container level, OUTSIDE DispatcherServlet
    // =========================================================================

    @Slf4j
    public static class TrackingFilter extends OncePerRequestFilter {

        private final ExecutionTracker tracker;

        public TrackingFilter(ExecutionTracker tracker) {
            this.tracker = tracker;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            tracker.record("filter-pre");
            log.debug("[Day47] Filter PRE-processing: {}", request.getRequestURI());
            try {
                filterChain.doFilter(request, response); // proceed to DispatcherServlet
            } finally {
                tracker.record("filter-post");
                log.debug("[Day47] Filter POST-processing complete");
            }
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            // Only apply to our Day 47 URLs
            return !request.getRequestURI().startsWith("/api/day47");
        }
    }

    // =========================================================================
    // Interceptor — runs inside DispatcherServlet, AFTER HandlerMapping
    // =========================================================================

    @Slf4j
    public static class TimingInterceptor implements HandlerInterceptor {

        private final ExecutionTracker tracker;
        private static final String START_TIME_ATTR = "day47.startTime";

        public TimingInterceptor(ExecutionTracker tracker) {
            this.tracker = tracker;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                 Object handler) {
            tracker.record("interceptor-pre");
            request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
            log.debug("[Day47] Interceptor preHandle — handler: {}", handler);
            return true; // continue processing
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response,
                               Object handler, ModelAndView modelAndView) {
            tracker.record("interceptor-post");
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            if (startTime != null) {
                log.debug("[Day47] Interceptor postHandle — elapsed: {}ms",
                        System.currentTimeMillis() - startTime);
            }
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            tracker.record("interceptor-after");
            log.debug("[Day47] Interceptor afterCompletion — response committed");
        }
    }

    // =========================================================================
    // Configuration — wires filter + interceptor (conditional on property)
    // =========================================================================

    @Configuration
    @Slf4j
    public static class TrackingConfig implements WebMvcConfigurer {

        @Autowired
        ExecutionTracker tracker;

        @Bean
        @ConditionalOnProperty("phase3.day47.tracking.enabled")
        public FilterRegistrationBean<TrackingFilter> trackingFilter() {
            var filter = new TrackingFilter(tracker);
            var reg = new FilterRegistrationBean<>(filter);
            reg.addUrlPatterns("/api/day47/*");
            reg.setOrder(1);
            log.debug("[Day47] TrackingFilter registered");
            return reg;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new TimingInterceptor(tracker))
                    .addPathPatterns("/api/day47/**");
        }
    }

    // =========================================================================
    // Demo controller
    // =========================================================================

    @RestController
    @RequestMapping("/api/day47")
    @Slf4j
    public static class ComparisonController {

        @Autowired
        ExecutionTracker tracker;

        @GetMapping("/compare")
        public ExecutionResult compareExecutionOrder() {
            tracker.record("controller");
            List<String> steps = tracker.getSteps();
            log.debug("[Day47] Controller sees steps so far: {}", steps);
            return new ExecutionResult(new ArrayList<>(steps));
        }
    }

    public record ExecutionResult(List<String> steps) {}
}
