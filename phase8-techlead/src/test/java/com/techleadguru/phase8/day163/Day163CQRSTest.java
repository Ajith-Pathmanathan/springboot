package com.techleadguru.phase8.day163;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day163CQRSTest {

    private java.util.Map<String, java.util.Map<String, Object>> store = new java.util.HashMap<>();

    private Day163CQRS.CommandBus buildBus() {
        return new Day163CQRS.CommandBus()
                .register(new Day163CQRS.CreateOrderCommandHandler(store))
                .register(new Day163CQRS.UpdateOrderCommandHandler(store));
    }

    @Test
    void createOrderCommandGeneratesIdAndStoresPending() {
        var bus = buildBus();
        var cmd = Day163CQRS.Command.of(Day163CQRS.CommandType.CREATE_ORDER,
                Map.of("customerId", "c1", "total", 99.0));

        String orderId = bus.dispatch(cmd);

        assertNotNull(orderId);
        assertEquals("PENDING", store.get(orderId).get("status"));
    }

    @Test
    void updateOrderCommandModifiesExistingEntry() {
        var bus = buildBus();
        String orderId = bus.dispatch(Day163CQRS.Command.of(
                Day163CQRS.CommandType.CREATE_ORDER, Map.of("customerId", "c1", "total", 50.0)));

        bus.dispatch(Day163CQRS.Command.of(Day163CQRS.CommandType.UPDATE_ORDER,
                Map.of("orderId", orderId, "status", "CONFIRMED")));

        assertEquals("CONFIRMED", store.get(orderId).get("status"));
    }

    @Test
    void commandBusThrowsForMissingHandler() {
        var bus = buildBus();
        var cmd = Day163CQRS.Command.of(Day163CQRS.CommandType.CANCEL_ORDER, Map.of());
        assertThrows(IllegalStateException.class, () -> bus.dispatch(cmd));
    }

    @Test
    void queryStoreStoresAndRetrievesModels() {
        var queryStore = new Day163CQRS.OrderQueryStore();
        var model = new Day163CQRS.OrderQueryModel("o1", "PENDING", 200.0, "Alice",
                java.time.Instant.now());
        queryStore.save(model);

        assertTrue(queryStore.findById("o1").isPresent());
        assertEquals("Alice", queryStore.findById("o1").get().customerName());
        assertEquals(1, queryStore.findByStatus("PENDING").size());
    }

    @Test
    void cqrsBenefitsListIsNonEmpty() {
        var list = Day163CQRS.cqrsBenefits();
        assertFalse(list.isEmpty());
        list.forEach(b -> assertNotNull(b.description()));
    }
}
