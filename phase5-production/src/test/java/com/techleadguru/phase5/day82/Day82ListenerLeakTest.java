package com.techleadguru.phase5.day82;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Day82ListenerLeakTest {

    private Day82ListenerLeak.PriceUpdateService service;

    @BeforeEach
    void setUp() {
        service = new Day82ListenerLeak.PriceUpdateService();
    }

    // ---- PriceUpdateService ----

    @Test
    void service_starts_with_no_listeners() {
        assertThat(service.listenerCount()).isEqualTo(0);
    }

    @Test
    void register_increases_listener_count() {
        // Constructor auto-registers; just verify the count increases
        new Day82ListenerLeak.LeakyDashboard("d1", service);
        assertThat(service.listenerCount()).isEqualTo(1);
    }

    @Test
    void deregister_decreases_listener_count() {
        Day82ListenerLeak.LeakyDashboard d = new Day82ListenerLeak.LeakyDashboard("d2", service);
        service.deregister(d);
        assertThat(service.listenerCount()).isEqualTo(0);
    }

    @Test
    void publish_notifies_registered_listeners() {
        Day82ListenerLeak.LeakyDashboard d = new Day82ListenerLeak.LeakyDashboard("d3", service);
        service.publish("AAPL", 175.50);
        assertThat(d.getUpdateCount()).isEqualTo(1);
    }

    // ---- LeakyDashboard — registers but never deregisters ----

    @Test
    void leakyDashboard_registers_in_constructor() {
        int before = service.listenerCount();
        new Day82ListenerLeak.LeakyDashboard("leaky", service);
        assertThat(service.listenerCount()).isEqualTo(before + 1);
    }

    // ---- SafeDashboard — deregisters on close ----

    @Test
    void safeDashboard_deregisters_on_close() {
        int before = service.listenerCount();
        try (Day82ListenerLeak.SafeDashboard safe = new Day82ListenerLeak.SafeDashboard("safe1", service)) {
            assertThat(service.listenerCount()).isEqualTo(before + 1);
        }
        assertThat(service.listenerCount()).isEqualTo(before);
    }

    @Test
    void safeDashboard_isClosed_after_close() {
        Day82ListenerLeak.SafeDashboard safe = new Day82ListenerLeak.SafeDashboard("safe2", service);
        assertThat(safe.isClosed()).isFalse();
        safe.close();
        assertThat(safe.isClosed()).isTrue();
    }

    @Test
    void safeDashboard_receives_updates_before_close() {
        try (Day82ListenerLeak.SafeDashboard safe = new Day82ListenerLeak.SafeDashboard("safe3", service)) {
            service.publish("GOOG", 140.0);
            assertThat(safe.getUpdateCount()).isEqualTo(1);
        }
    }

    // ---- OrderAuditListener ----

    @Test
    void orderAuditListener_records_event_in_audit_log() {
        Day82ListenerLeak.OrderAuditListener listener = new Day82ListenerLeak.OrderAuditListener();
        listener.onOrderEvent(new Day82ListenerLeak.OrderEvent("ord-1", "PLACED"));
        assertThat(listener.getAuditLog()).hasSize(1);
        assertThat(listener.getAuditLog().get(0)).contains("ord-1");
    }
}
