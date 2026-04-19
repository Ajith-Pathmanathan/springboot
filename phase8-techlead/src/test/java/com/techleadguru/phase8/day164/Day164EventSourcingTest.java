package com.techleadguru.phase8.day164;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day164EventSourcingTest {

    @Test
    void eventStoreAppendsAndLoadsEvents() {
        var store = new Day164EventSourcing.EventStore();
        var e1 = Day164EventSourcing.DomainEvent.of("o1", "ORDER_CREATED",
                Map.of("customerId", "c1", "total", 100.0, "items", List.of("SKU-1")), 1);
        var e2 = Day164EventSourcing.DomainEvent.of("o1", "ORDER_CONFIRMED", Map.of(), 2);

        store.append(e1);
        store.append(e2);

        assertEquals(2, store.eventCount("o1"));
        assertEquals(2, store.loadEvents("o1").size());
        assertEquals(0, store.eventCount("o2"));
    }

    @Test
    void orderAggregateRehydratesFromEvents() {
        var store = new Day164EventSourcing.EventStore();
        String orderId = "order-1";

        Day164EventSourcing.OrderAggregate.createOrder(orderId, "cust-1", 199.99,
                List.of("SKU-A", "SKU-B")).forEach(store::append);
        Day164EventSourcing.OrderAggregate.confirmOrder(orderId, 2).forEach(store::append);

        var aggregate = Day164EventSourcing.OrderAggregate.rehydrate(store.loadEvents(orderId));

        assertEquals(orderId, aggregate.orderId());
        assertEquals(Day164EventSourcing.OrderStatus.CONFIRMED, aggregate.status());
        assertEquals(199.99, aggregate.total(), 0.001);
        assertEquals("cust-1", aggregate.customerId());
        assertEquals(2, aggregate.version());
    }

    @Test
    void orderAggregateHandlesCancellation() {
        String orderId = "order-2";
        var events = new java.util.ArrayList<Day164EventSourcing.DomainEvent>();
        events.addAll(Day164EventSourcing.OrderAggregate.createOrder(orderId, "c2", 50.0, List.of()));
        events.addAll(Day164EventSourcing.OrderAggregate.cancelOrder(orderId, 2));

        var aggregate = Day164EventSourcing.OrderAggregate.rehydrate(events);
        assertEquals(Day164EventSourcing.OrderStatus.CANCELLED, aggregate.status());
    }

    @Test
    void eventSourcingVsTraditionalComparisonIsNonEmpty() {
        var list = Day164EventSourcing.eventSourcingVsTraditional();
        assertFalse(list.isEmpty());
        list.forEach(c -> assertNotNull(c.aspect()));
    }

    @Test
    void snapshotStrategyReturnsAdvice() {
        var advice = Day164EventSourcing.snapshotStrategy();
        assertFalse(advice.isEmpty());
        assertTrue(advice.get(0).toLowerCase().contains("snapshot"));
    }
}
