package com.techleadguru.phase3.day47;

import com.techleadguru.phase3.day47.Day47FiltersInterceptors.ExecutionTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 47 — Test: Filter runs before interceptor; interceptor wraps controller.
 * Uses phase3.day47.tracking.enabled=true to activate the TrackingFilter.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "phase3.day47.tracking.enabled=true")
@AutoConfigureMockMvc
class Day47FiltersInterceptorsTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ExecutionTracker tracker;

    @BeforeEach
    void clearTracker() {
        tracker.clear();
    }

    // -----------------------------------------------------------------------
    // Test 1: filter-pre runs before interceptor-pre
    // -----------------------------------------------------------------------
    @Test
    void filter_runs_before_interceptor() throws Exception {
        mockMvc.perform(get("/api/day47/compare"))
                .andExpect(status().isOk());

        List<String> steps = tracker.getSteps();
        System.out.println("[DAY 47] Execution order: " + steps);

        assertThat(steps.indexOf("filter-pre"))
                .as("filter-pre should run before interceptor-pre")
                .isLessThan(steps.indexOf("interceptor-pre"));
    }

    // -----------------------------------------------------------------------
    // Test 2: interceptor-pre runs before controller
    // -----------------------------------------------------------------------
    @Test
    void interceptor_runs_before_controller() throws Exception {
        mockMvc.perform(get("/api/day47/compare"))
                .andExpect(status().isOk());

        List<String> steps = tracker.getSteps();

        assertThat(steps.indexOf("interceptor-pre"))
                .as("interceptor-pre should run before controller")
                .isLessThan(steps.indexOf("controller"));
    }

    // -----------------------------------------------------------------------
    // Test 3: full execution order is filter → interceptor → controller → interceptor → filter
    // -----------------------------------------------------------------------
    @Test
    void full_execution_order_is_correct() throws Exception {
        mockMvc.perform(get("/api/day47/compare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps").isArray());

        List<String> steps = tracker.getSteps();
        System.out.println("[DAY 47] Full execution order: " + steps);

        // Verify the ordering
        assertThat(steps).contains("filter-pre", "interceptor-pre", "controller");
        assertThat(steps.indexOf("filter-pre")).isLessThan(steps.indexOf("interceptor-pre"));
        assertThat(steps.indexOf("interceptor-pre")).isLessThan(steps.indexOf("controller"));
        assertThat(steps.indexOf("controller")).isLessThan(steps.indexOf("interceptor-post"));
        assertThat(steps.indexOf("interceptor-post")).isLessThan(steps.indexOf("filter-post"));
    }

    // -----------------------------------------------------------------------
    // Test 4: Without tracking enabled, filter does NOT run for other URLs
    // -----------------------------------------------------------------------
    @Test
    void filter_applies_only_to_day47_urls() throws Exception {
        // TrackingFilter has shouldNotFilter() returning true for non-day47 paths
        tracker.clear();
        mockMvc.perform(get("/api/day43/orders"))
                .andExpect(status().isOk());

        List<String> steps = tracker.getSteps();
        // Filter should NOT have been invoked for /api/day43/...
        assertThat(steps).doesNotContain("filter-pre");

        System.out.println("[DAY 47] TrackingFilter scoped to /api/day47/* only");
    }

    // -----------------------------------------------------------------------
    // Test 5: Document the comparison
    // -----------------------------------------------------------------------
    @Test
    void document_filter_vs_interceptor_vs_aop() {
        System.out.println("[DAY 47] FILTER vs INTERCEPTOR vs AOP:");
        System.out.println();
        System.out.println("  Filter                   Interceptor              AOP @Around");
        System.out.println("  ──────────────────────   ──────────────────────   ──────────────────────");
        System.out.println("  Servlet container level  Spring MVC level         Spring proxy level");
        System.out.println("  Raw Request/Response     Handler method info      Any Spring bean method");
        System.out.println("  Before DispatcherServlet After HandlerMapping      Wraps method invocation");
        System.out.println("  Static resources too     Controller only          Service/repo layers");
        System.out.println("  CORS, auth, compression  Logging, timing          @Transactional, caching");
        System.out.println();
        System.out.println("  EXECUTION ORDER (innermost last):");
        System.out.println("    Filter → Interceptor.preHandle → AOP → Controller → AOP → Interceptor.postHandle → Filter");
        assertThat(true).isTrue();
    }
}
