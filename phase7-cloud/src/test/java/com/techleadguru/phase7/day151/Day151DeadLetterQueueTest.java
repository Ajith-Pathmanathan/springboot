package com.techleadguru.phase7.day151;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day151DeadLetterQueueTest {

    @Test
    void testRetryTrackerNotDlqUntilMaxAttempts() {
        Day151DeadLetterQueue.DlqConfig cfg = Day151DeadLetterQueue.DlqConfig.defaultConfig();
        Day151DeadLetterQueue.RetryTracker tracker = new Day151DeadLetterQueue.RetryTracker(cfg);
        tracker.recordFailure("msg-1");
        tracker.recordFailure("msg-1");
        assertFalse(tracker.shouldSendToDlq("msg-1"));
        tracker.recordFailure("msg-1");
        assertTrue(tracker.shouldSendToDlq("msg-1"));
    }

    @Test
    void testAttemptCount() {
        Day151DeadLetterQueue.DlqConfig cfg = Day151DeadLetterQueue.DlqConfig.defaultConfig();
        Day151DeadLetterQueue.RetryTracker tracker = new Day151DeadLetterQueue.RetryTracker(cfg);
        assertEquals(0, tracker.attemptCount("msg-x"));
        tracker.recordFailure("msg-x");
        tracker.recordFailure("msg-x");
        assertEquals(2, tracker.attemptCount("msg-x"));
    }

    @Test
    void testRetryTrackerReset() {
        Day151DeadLetterQueue.DlqConfig cfg = Day151DeadLetterQueue.DlqConfig.defaultConfig();
        Day151DeadLetterQueue.RetryTracker tracker = new Day151DeadLetterQueue.RetryTracker(cfg);
        tracker.recordFailure("msg-1");
        tracker.recordFailure("msg-1");
        tracker.recordFailure("msg-1");
        tracker.reset("msg-1");
        assertFalse(tracker.shouldSendToDlq("msg-1"));
    }

    @Test
    void testDlqMessageOf() {
        Day151DeadLetterQueue.DlqMessage msg =
                Day151DeadLetterQueue.DlqMessage.of(
                        "orders", "DeserializationException", 3, "{id:1}");
        assertEquals("orders", msg.originalTopic());
        assertEquals(3, msg.attemptCount());
        assertNotNull(msg.sentAt());
    }

    @Test
    void testDlqProperties() {
        Map<String, String> props =
                Day151DeadLetterQueue.dlqProperties("processOrder-in-0", 3);
        assertTrue(props.containsKey(
                "spring.cloud.stream.kafka.bindings.processOrder-in-0.consumer.enable-dlq"));
        assertEquals("true",
                props.get("spring.cloud.stream.kafka.bindings.processOrder-in-0.consumer.enable-dlq"));
    }

    @Test
    void testDlqHandlingSteps() {
        List<String> steps = Day151DeadLetterQueue.dlqHandlingSteps();
        assertEquals(8, steps.size());
    }

    @Test
    void testDefaultDlqConfig() {
        Day151DeadLetterQueue.DlqConfig cfg = Day151DeadLetterQueue.DlqConfig.defaultConfig();
        assertEquals(3, cfg.maxRetries());
        assertEquals(".DLT", cfg.dlqTopicSuffix());
    }
}
