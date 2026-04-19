package com.techleadguru.phase5.day82;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DAY 82 — Listener/Callback Registration Leak
 *
 * PATTERN: object registers itself to receive callbacks but never de-registers.
 * When the object goes out of scope, the registry still holds a reference → no GC.
 *
 * COMMON FRAMEWORKS WHERE THIS HAPPENS:
 *   - Spring ApplicationEventPublisher / ApplicationListener
 *   - Guava EventBus (subscribe vs unregister)
 *   - Java Observable (addObserver vs deleteObserver)
 *   - Swing: button.addActionListener() — no matching removeActionListener
 *   - RxJava / Reactor: Disposable not cancelled → subscriber leak
 *
 * HOW TO DETECT:
 *   - listener count grows with each request / user session
 *   - MAT: anonymous inner class instances > expected count
 *   - Heap grows linearly with user count
 *
 * HOW TO FIX:
 *   - Implement DisposableBean / @PreDestroy to de-register
 *   - Track subscriptions with Disposable; cancel() on cleanup
 *   - Use WeakReference for subscriber tracking
 *   - Prefer @EventListener on @Component (Spring manages lifecycle)
 */
@Slf4j
public class Day82ListenerLeak {

    // =========================================================================
    // Custom callback-registry demonstrating register/deregister pattern
    // =========================================================================

    public interface PriceUpdateListener {
        void onPriceUpdate(String symbol, double price);
    }

    @Slf4j
    public static class PriceUpdateService {

        private final List<PriceUpdateListener> listeners = new CopyOnWriteArrayList<>();

        public void register(PriceUpdateListener l) {
            listeners.add(l);
            log.debug("[Day82] Registered listener. Total: {}", listeners.size());
        }

        public void deregister(PriceUpdateListener l) {
            listeners.remove(l);
            log.debug("[Day82] Deregistered listener. Total: {}", listeners.size());
        }

        public void publish(String symbol, double price) {
            for (PriceUpdateListener l : listeners) {
                l.onPriceUpdate(symbol, price);
            }
        }

        public int listenerCount() { return listeners.size(); }
    }

    // =========================================================================
    // BROKEN: Dashboard registers on creation but never de-registers
    // =========================================================================

    @Slf4j
    public static class LeakyDashboard implements PriceUpdateListener {

        private final String dashboardId;
        private final List<String> receivedUpdates = new ArrayList<>();
        private final byte[] uiState = new byte[512 * 1024]; // 512KB of fake UI state

        public LeakyDashboard(String id, PriceUpdateService service) {
            this.dashboardId = id;
            service.register(this); // BUG: never deregistered → LeakyDashboard never GC'd
        }

        @Override
        public void onPriceUpdate(String symbol, double price) {
            receivedUpdates.add(symbol + "=" + price);
        }

        public int getUpdateCount() { return receivedUpdates.size(); }
        public String getId()        { return dashboardId; }
    }

    // =========================================================================
    // FIXED: Dashboard implements AutoCloseable and de-registers on close
    // =========================================================================

    @Slf4j
    public static class SafeDashboard implements PriceUpdateListener, AutoCloseable {

        private final String id;
        private final PriceUpdateService service;
        private final List<String> receivedUpdates = new ArrayList<>();
        private boolean closed = false;

        public SafeDashboard(String id, PriceUpdateService service) {
            this.id = id;
            this.service = service;
            service.register(this);
            log.debug("[Day82] SafeDashboard {} registered", id);
        }

        @Override
        public void onPriceUpdate(String symbol, double price) {
            if (!closed) receivedUpdates.add(symbol + "=" + price);
        }

        @Override
        public void close() {
            service.deregister(this);
            closed = true;
            log.debug("[Day82] SafeDashboard {} deregistered", id);
        }

        public int getUpdateCount() { return receivedUpdates.size(); }
        public boolean isClosed()   { return closed; }
    }

    // =========================================================================
    // Spring @EventListener approach — Spring manages lifecycle automatically
    // No need to manually register/deregister when using @EventListener on @Component
    // =========================================================================

    public record OrderEvent(String orderId, String status) {}

    @Component
    @Slf4j
    public static class OrderAuditListener {

        private final List<String> auditLog = new CopyOnWriteArrayList<>();

        /**
         * Spring registers this automatically and cleans up when context closes.
         * This is the SAFE pattern — no manual register/deregister needed.
         */
        @EventListener
        public void onOrderEvent(OrderEvent event) {
            auditLog.add("[Day82] Order " + event.orderId() + " → " + event.status());
            log.debug("[Day82] Audit: {}", auditLog.get(auditLog.size() - 1));
        }

        public List<String> getAuditLog() { return List.copyOf(auditLog); }
        public void clearLog()            { auditLog.clear(); }
    }

    @Component
    @Slf4j
    public static class OrderService {
        private final ApplicationEventPublisher publisher;

        public OrderService(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        public void placeOrder(String orderId) {
            log.info("[Day82] Placing order {}", orderId);
            publisher.publishEvent(new OrderEvent(orderId, "PLACED"));
        }
    }
}
