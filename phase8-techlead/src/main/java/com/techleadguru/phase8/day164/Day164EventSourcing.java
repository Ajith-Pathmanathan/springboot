package com.techleadguru.phase8.day164;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Day 164 — Event Sourcing
 *
 * Instead of storing current state, store the full history of events.
 * Current state is derived by replaying all events in order.
 *
 * Key concepts:
 *  - EventStore  : append-only log of domain events
 *  - Aggregate   : replays events to rebuild its state
 *  - Snapshot    : periodic state snapshot to avoid replaying all events
 */
public class Day164EventSourcing {

    // ─────────────────────────────────────────────────────────────────────────
    // Domain event
    // ─────────────────────────────────────────────────────────────────────────

    public record DomainEvent(
            String  eventId,
            String  aggregateId,
            String  eventType,
            Map<String, Object> payload,
            Instant occurredAt,
            int     version) {

        public static DomainEvent of(String aggregateId, String type,
                                     Map<String, Object> payload, int version) {
            return new DomainEvent(
                    UUID.randomUUID().toString(), aggregateId, type, payload, Instant.now(), version);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Event store
    // ─────────────────────────────────────────────────────────────────────────

    public static class EventStore {

        private final Map<String, List<DomainEvent>> store = new LinkedHashMap<>();

        public void append(DomainEvent event) {
            store.computeIfAbsent(event.aggregateId(), k -> new ArrayList<>()).add(event);
        }

        public List<DomainEvent> loadEvents(String aggregateId) {
            return Collections.unmodifiableList(
                    store.getOrDefault(aggregateId, List.of()));
        }

        public int eventCount(String aggregateId) {
            return store.getOrDefault(aggregateId, List.of()).size();
        }

        public int totalEvents() {
            return store.values().stream().mapToInt(List::size).sum();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Order aggregate
    // ─────────────────────────────────────────────────────────────────────────

    public enum OrderStatus { PENDING, CONFIRMED, SHIPPED, CANCELLED }

    public static class OrderAggregate {

        private String      orderId;
        private OrderStatus status;
        private double      total;
        private String      customerId;
        private List<String> items = new ArrayList<>();
        private int         version = 0;

        /** Rebuild state from events. */
        public static OrderAggregate rehydrate(List<DomainEvent> events) {
            OrderAggregate aggregate = new OrderAggregate();
            events.forEach(aggregate::apply);
            return aggregate;
        }

        private void apply(DomainEvent event) {
            version = event.version();
            switch (event.eventType()) {
                case "ORDER_CREATED" -> {
                    orderId    = event.aggregateId();
                    customerId = (String) event.payload().get("customerId");
                    status     = OrderStatus.PENDING;
                    total      = ((Number) event.payload().get("total")).doubleValue();
                    @SuppressWarnings("unchecked")
                    List<String> it = (List<String>) event.payload().get("items");
                    items = new ArrayList<>(it != null ? it : List.of());
                }
                case "ORDER_CONFIRMED" -> status = OrderStatus.CONFIRMED;
                case "ORDER_SHIPPED"   -> status = OrderStatus.SHIPPED;
                case "ORDER_CANCELLED" -> status = OrderStatus.CANCELLED;
            }
        }

        // Convenience factory methods — in real code these would emit events
        public static List<DomainEvent> createOrder(
                String orderId, String customerId, double total, List<String> items) {
            return List.of(DomainEvent.of(orderId, "ORDER_CREATED",
                    Map.of("customerId", customerId, "total", total, "items", items), 1));
        }

        public static List<DomainEvent> confirmOrder(String orderId, int nextVersion) {
            return List.of(DomainEvent.of(orderId, "ORDER_CONFIRMED", Map.of(), nextVersion));
        }

        public static List<DomainEvent> shipOrder(String orderId, int nextVersion) {
            return List.of(DomainEvent.of(orderId, "ORDER_SHIPPED", Map.of(), nextVersion));
        }

        public static List<DomainEvent> cancelOrder(String orderId, int nextVersion) {
            return List.of(DomainEvent.of(orderId, "ORDER_CANCELLED", Map.of(), nextVersion));
        }

        public String      orderId()    { return orderId; }
        public OrderStatus status()     { return status; }
        public double      total()      { return total; }
        public String      customerId() { return customerId; }
        public List<String> items()     { return Collections.unmodifiableList(items); }
        public int         version()    { return version; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comparison guide
    // ─────────────────────────────────────────────────────────────────────────

    public record Comparison(String aspect, String traditional, String eventSourcing) {}

    public static List<Comparison> eventSourcingVsTraditional() {
        return List.of(
            new Comparison("Storage",
                "Only current state stored",
                "Full event log stored — current state derived"),
            new Comparison("Audit trail",
                "Need separate audit table",
                "Audit trail is the primary store — free"),
            new Comparison("Time travel",
                "Not possible",
                "Replay to any point in time"),
            new Comparison("Debug",
                "Hard — state was overwritten",
                "Easy — replay and inspect each event"),
            new Comparison("Complexity",
                "Simple CRUD",
                "Higher — need event versioning, schema evolution")
        );
    }

    public static List<String> snapshotStrategy() {
        return List.of(
            "Take snapshot every N events (e.g., every 50 events) to avoid full replay",
            "Load latest snapshot first, then replay only events after snapshot version",
            "Store snapshot alongside event stream in EventStore",
            "Invalidate old snapshots when aggregate logic changes",
            "Use async snapshot writer to avoid blocking command path"
        );
    }
}
