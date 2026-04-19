package com.techleadguru.phase7.day151;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day 151 — Dead Letter Queue (DLQ)
 *
 * When a consumer repeatedly fails to process a message, after maxRetries
 * attempts the message is forwarded to the DLQ topic (suffix ".DLT" by default
 * in Spring Cloud Stream, or configured via spring.cloud.stream.kafka.bindings
 * .consumer.dlq-name).
 *
 * DLQ messages can be monitored, analysed, and replayed after fixing the bug.
 */
public class Day151DeadLetterQueue {

    // ─────────────────────────────────────────────────────────────────────────
    // Config
    // ─────────────────────────────────────────────────────────────────────────

    public record DlqConfig(
            int    maxRetries,     // e.g. 3 attempts before sending to DLQ
            long   backoffMs,      // delay between retries in milliseconds
            String dlqTopicSuffix) { // appended to original topic name

        public static DlqConfig defaultConfig() {
            return new DlqConfig(3, 1_000L, ".DLT");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Retry tracker
    // ─────────────────────────────────────────────────────────────────────────

    public static class RetryTracker {

        private final DlqConfig config;
        private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

        public RetryTracker(DlqConfig config) {
            this.config = config;
        }

        /** Record a processing failure for the given message ID. */
        public void recordFailure(String messageId) {
            attempts.computeIfAbsent(messageId, id -> new AtomicInteger(0))
                    .incrementAndGet();
        }

        /** True when the message has been tried maxRetries times. */
        public boolean shouldSendToDlq(String messageId) {
            AtomicInteger count = attempts.get(messageId);
            return count != null && count.get() >= config.maxRetries();
        }

        public int attemptCount(String messageId) {
            AtomicInteger count = attempts.get(messageId);
            return count == null ? 0 : count.get();
        }

        public void reset(String messageId) {
            attempts.remove(messageId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DLQ message envelope
    // ─────────────────────────────────────────────────────────────────────────

    public record DlqMessage(
            String  originalTopic,
            String  failureReason,
            int     attemptCount,
            String  payload,
            Instant sentAt) {

        public static DlqMessage of(String originalTopic, String reason,
                                    int attempts, String payload) {
            return new DlqMessage(originalTopic, reason, attempts, payload, Instant.now());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DLQ properties (Spring Cloud Stream Kafka binder)
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> dlqProperties(String bindingName, int maxRetries) {
        Map<String, String> props = new LinkedHashMap<>();
        String base = "spring.cloud.stream.kafka.bindings." + bindingName + ".consumer";
        props.put(base + ".enable-dlq",        "true");
        props.put(base + ".dlq-name",          bindingName + ".DLT");
        props.put(base + ".max-attempts",      String.valueOf(maxRetries));
        props.put(base + ".back-off-initial-interval", "1000");
        props.put(base + ".back-off-multiplier",       "2.0");
        props.put(base + ".back-off-max-interval",     "10000");
        return props;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Handling steps
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> dlqHandlingSteps() {
        return List.of(
            "1. Consumer fails to process message → exception thrown",
            "2. Spring error handler retries up to max-attempts with backoff",
            "3. After max attempts exceeded, message written to DLQ topic",
            "4. Original message headers preserved (original topic, offset, exception)",
            "5. DLQ consumer/monitoring alerts team of dead-lettered messages",
            "6. Team investigates root cause (bad data, bug, downstream issue)",
            "7. After fix: replay DLQ messages to original topic or process inline",
            "8. Delete DLQ messages once successfully reprocessed"
        );
    }
}
