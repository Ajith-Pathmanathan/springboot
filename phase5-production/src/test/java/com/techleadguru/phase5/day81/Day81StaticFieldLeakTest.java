package com.techleadguru.phase5.day81;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class Day81StaticFieldLeakTest {

    @BeforeEach
    void setUp() {
        Day81StaticFieldLeak.ReportService.clearCache();
        Day81StaticFieldLeak.EventBus.clearAll();
        Day81StaticFieldLeak.SafeEventBus.clearAll();
    }

    // ---- ReportService ----

    @Test
    void reportService_cache_grows_with_each_report() {
        Day81StaticFieldLeak.ReportService svc = new Day81StaticFieldLeak.ReportService();
        svc.generateAndCache("order-1");
        svc.generateAndCache("order-2");
        assertThat(svc.getCacheSize()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void reportService_clearCache_empties_static_map() {
        Day81StaticFieldLeak.ReportService svc = new Day81StaticFieldLeak.ReportService();
        svc.generateAndCache("x");
        Day81StaticFieldLeak.ReportService.clearCache();
        assertThat(svc.getCacheSize()).isEqualTo(0);
    }

    // ---- EventBus (leaky) ----

    @Test
    void eventBus_register_increase_listener_count() {
        Day81StaticFieldLeak.EventBus.EventListener listener = e -> {};
        Day81StaticFieldLeak.EventBus.register(listener);
        assertThat(Day81StaticFieldLeak.EventBus.listenerCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void eventBus_publish_notifies_listeners() {
        AtomicInteger count = new AtomicInteger();
        Day81StaticFieldLeak.EventBus.register(e -> count.incrementAndGet());
        Day81StaticFieldLeak.EventBus.publish("test-event");
        assertThat(count.get()).isEqualTo(1);
    }

    // ---- SafeEventBus (WeakReference) ----

    @Test
    void safeEventBus_registers_listener_successfully() {
        Day81StaticFieldLeak.EventBus.EventListener listener = e -> {};
        Day81StaticFieldLeak.SafeEventBus.register(listener);
        assertThat(Day81StaticFieldLeak.SafeEventBus.listenerCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void safeEventBus_publish_notifies_live_listeners() {
        AtomicInteger count = new AtomicInteger();
        Day81StaticFieldLeak.EventBus.EventListener listener = e -> count.incrementAndGet();
        Day81StaticFieldLeak.SafeEventBus.register(listener);
        Day81StaticFieldLeak.SafeEventBus.publish("safe-event");
        // listener is still in scope, so it should be notified
        assertThat(count.get()).isEqualTo(1);
    }

    // ---- InnerClassLeakDemo ----

    @Test
    void innerClassLeakDemo_size_is_positive() {
        Day81StaticFieldLeak.InnerClassLeakDemo demo =
                new Day81StaticFieldLeak.InnerClassLeakDemo("test");
        assertThat(demo.getDataSize()).isPositive();
    }

    @Test
    void innerClassLeakDemo_createSafeTask_creates_runnable() {
        Day81StaticFieldLeak.InnerClassLeakDemo demo =
                new Day81StaticFieldLeak.InnerClassLeakDemo("safe-test");
        Runnable task = demo.createSafeTask();
        assertThat(task).isNotNull();
        assertThatCode(task::run).doesNotThrowAnyException();
    }
}
