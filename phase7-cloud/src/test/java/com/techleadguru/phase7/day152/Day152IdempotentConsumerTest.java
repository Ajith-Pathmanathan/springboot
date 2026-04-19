package com.techleadguru.phase7.day152;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Day152IdempotentConsumerTest {

    @Test
    void testFirstProcessIsNew() {
        Day152IdempotentConsumer.IdempotencyStore store =
                new Day152IdempotentConsumer.IdempotencyStore(60_000L);
        assertEquals(Day152IdempotentConsumer.DeduplicationResult.NEW,
                store.markProcessed("msg-1"));
    }

    @Test
    void testSecondProcessIsDuplicate() {
        Day152IdempotentConsumer.IdempotencyStore store =
                new Day152IdempotentConsumer.IdempotencyStore(60_000L);
        store.markProcessed("msg-1");
        assertEquals(Day152IdempotentConsumer.DeduplicationResult.DUPLICATE,
                store.markProcessed("msg-1"));
    }

    @Test
    void testIsProcessedReturnsTrueAfterMark() {
        Day152IdempotentConsumer.IdempotencyStore store =
                new Day152IdempotentConsumer.IdempotencyStore(60_000L);
        store.markProcessed("abc");
        assertTrue(store.isProcessed("abc"));
    }

    @Test
    void testIsProcessedReturnsFalseForNew() {
        Day152IdempotentConsumer.IdempotencyStore store =
                new Day152IdempotentConsumer.IdempotencyStore(60_000L);
        assertFalse(store.isProcessed("xyz"));
    }

    @Test
    void testDifferentMessageIds() {
        Day152IdempotentConsumer.IdempotencyStore store =
                new Day152IdempotentConsumer.IdempotencyStore(60_000L);
        store.markProcessed("m1");
        assertEquals(Day152IdempotentConsumer.DeduplicationResult.NEW,
                store.markProcessed("m2"));
    }

    @Test
    void testSizeAndClear() {
        Day152IdempotentConsumer.IdempotencyStore store =
                new Day152IdempotentConsumer.IdempotencyStore(60_000L);
        store.markProcessed("m1");
        store.markProcessed("m2");
        assertEquals(2, store.size());
        store.clear();
        assertEquals(0, store.size());
    }

    @Test
    void testIdempotencyStrategies() {
        List<Day152IdempotentConsumer.IdempotencyStrategy> strategies =
                Day152IdempotentConsumer.idempotencyStrategies();
        assertEquals(4, strategies.size());
    }

    @Test
    void testKeyDesignGuide() {
        List<Day152IdempotentConsumer.KeyDesignRule> rules =
                Day152IdempotentConsumer.keyDesignGuide();
        assertFalse(rules.isEmpty());
    }
}
