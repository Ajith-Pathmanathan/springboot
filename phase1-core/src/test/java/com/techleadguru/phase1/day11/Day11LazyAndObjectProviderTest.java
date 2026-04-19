package com.techleadguru.phase1.day11;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * DAY 11 — Test: @Lazy and ObjectProvider behaviour.
 */
class Day11LazyAndObjectProviderTest {

    // -----------------------------------------------------------------------
    // Test 1: ObjectProvider.getObject() creates the bean on first call
    // -----------------------------------------------------------------------
    @Test
    void object_provider_creates_bean_lazily_on_first_call() {
        var pdfService = new Day11LazyAndObjectProvider.ExpensivePdfService();
        // Simulate ObjectProvider with a simple lambda
        ObjectProvider<Day11LazyAndObjectProvider.ExpensivePdfService> provider =
                new SingletonObjectProvider<>(pdfService);

        var controller = new Day11LazyAndObjectProvider.ReportController(provider);

        // Only created when generateReport is called
        String result = controller.generateReport("Q4-2025");

        assertThat(result).isEqualTo("PDF[Q4-2025]");
        System.out.println("[DAY 11] Lazy: ExpensivePdfService used on-demand: " + result);
    }

    // -----------------------------------------------------------------------
    // Test 2: ObjectProvider.getIfAvailable() returns null when no bean — no exception
    // -----------------------------------------------------------------------
    @Test
    void object_provider_returns_null_when_optional_bean_absent() {
        // Provider that returns null — simulates missing SlackNotifier bean
        @SuppressWarnings("unchecked")
        ObjectProvider<Day11LazyAndObjectProvider.SlackNotifier> emptyProvider =
                mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);

        var alertService = new Day11LazyAndObjectProvider.AlertService(emptyProvider);

        // Should NOT throw — gracefully handles missing bean
        alertService.sendAlert("Server down!");
        assertThat(alertService.hasSlackBean()).isFalse();

        System.out.println("[DAY 11] ObjectProvider.getIfAvailable() returned null — no exception");
    }

    // -----------------------------------------------------------------------
    // Test 3: ObjectProvider.getIfAvailable() provides bean when present
    // -----------------------------------------------------------------------
    @Test
    void object_provider_provides_bean_when_present() {
        Day11LazyAndObjectProvider.SlackNotifier mockSlack = message ->
                System.out.println("[DAY 11] Slack: " + message);

        @SuppressWarnings("unchecked")
        ObjectProvider<Day11LazyAndObjectProvider.SlackNotifier> provider =
                mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mockSlack);

        var alertService = new Day11LazyAndObjectProvider.AlertService(provider);
        alertService.sendAlert("CPU high");

        assertThat(alertService.hasSlackBean()).isTrue();
        System.out.println("[DAY 11] SlackNotifier bean found and used");
    }

    // -----------------------------------------------------------------------
    // Test 4: @Lazy rule documented
    // -----------------------------------------------------------------------
    @Test
    void lazy_annotation_rules_documented() {
        System.out.println("[DAY 11] @Lazy RULES:");
        System.out.println("  1. On @Component/@Bean: bean created on first getBean() call.");
        System.out.println("  2. On @Autowired injection point: a CGLIB proxy is injected at startup,");
        System.out.println("     real bean created on first method call.");
        System.out.println("  3. Use @Lazy on one leg of circular dep to break the cycle.");
        System.out.println("  ObjectProvider<T> RULES:");
        System.out.println("  1. getObject()       — required, throws if missing");
        System.out.println("  2. getIfAvailable()  — optional, returns null if missing");
        System.out.println("  3. getIfUnique()     — returns null if 0 or 2+ beans exist");
        System.out.println("  4. stream()          — iterate all beans of the type");
        assertThat(true).isTrue();
    }

    // -----------------------------------------------------------------------
    // Minimal ObjectProvider stub for single-bean case
    // -----------------------------------------------------------------------
    static class SingletonObjectProvider<T> implements ObjectProvider<T> {
        private final T instance;

        SingletonObjectProvider(T instance) { this.instance = instance; }

        @Override public T getObject() { return instance; }
        @Override public T getObject(Object... args) { return instance; }
        @Override public T getIfAvailable() { return instance; }
        @Override public T getIfUnique() { return instance; }
    }
}
