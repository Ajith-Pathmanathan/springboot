package com.techleadguru.phase7.day153;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day153TransactionalOutboxTest {

    @Test
    void testSaveCreatesUnpublishedEvent() {
        Day153TransactionalOutbox.OutboxStore store =
                new Day153TransactionalOutbox.OutboxStore();
        store.save("Order", "ORD-1", "OrderCreated", "{id:1}");
        List<Day153TransactionalOutbox.OutboxEvent> unpublished =
                store.findUnpublished(10);
        assertEquals(1, unpublished.size());
        assertFalse(unpublished.get(0).isPublished());
    }

    @Test
    void testMarkPublishedChangesState() {
        Day153TransactionalOutbox.OutboxStore store =
                new Day153TransactionalOutbox.OutboxStore();
        Day153TransactionalOutbox.OutboxEvent event =
                store.save("Order", "ORD-2", "OrderCreated", "{}");
        store.markPublished(event.id());
        assertEquals(0, store.findUnpublished(10).size());
        assertEquals(1, store.publishedCount());
    }

    @Test
    void testMultipleEventsPartialPublish() {
        Day153TransactionalOutbox.OutboxStore store =
                new Day153TransactionalOutbox.OutboxStore();
        Day153TransactionalOutbox.OutboxEvent e1 = store.save("Order", "1", "Created", "{}");
        store.save("Order", "2", "Created", "{}");
        store.markPublished(e1.id());
        assertEquals(1, store.findUnpublished(10).size());
        assertEquals(1, store.publishedCount());
    }

    @Test
    void testFindUnpublishedRespectLimit() {
        Day153TransactionalOutbox.OutboxStore store =
                new Day153TransactionalOutbox.OutboxStore();
        for (int i = 0; i < 5; i++) {
            store.save("Order", "ORD-" + i, "Created", "{}");
        }
        List<Day153TransactionalOutbox.OutboxEvent> batch = store.findUnpublished(3);
        assertEquals(3, batch.size());
    }

    @Test
    void testOutboxPatternSteps() {
        List<String> steps = Day153TransactionalOutbox.outboxPatternSteps();
        assertEquals(8, steps.size());
    }

    @Test
    void testBenefits() {
        List<Day153TransactionalOutbox.Benefit> benefits =
                Day153TransactionalOutbox.benefits();
        assertEquals(5, benefits.size());
        assertTrue(benefits.stream().anyMatch(b -> b.title().equals("Atomicity")));
    }
}
