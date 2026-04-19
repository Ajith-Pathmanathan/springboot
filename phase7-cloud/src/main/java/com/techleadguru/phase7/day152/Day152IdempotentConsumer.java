package com.techleadguru.phase7.day152;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Day 152 — Idempotent consumer pattern
 *
 * At-least-once delivery means a consumer may receive the same message twice
 * (after a crash before committing offset, or a network hiccup).
 * The idempotent consumer pattern records processed message IDs and skips
 * duplicates.
 *
 * Storage options: Redis (SETNX + TTL), DB (unique index on messageId), in-memory.
 */
public class Day152IdempotentConsumer {

    // ─────────────────────────────────────────────────────────────────────────
    // Result
    // ─────────────────────────────────────────────────────────────────────────

    public enum DeduplicationResult { NEW, DUPLICATE }

    // ─────────────────────────────────────────────────────────────────────────
    // Processed record
    // ─────────────────────────────────────────────────────────────────────────

    public record ProcessedRecord(String messageId, Instant processedAt) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Idempotency store (in-memory with TTL — models Redis SETNX + TTL)
    // ─────────────────────────────────────────────────────────────────────────

    public static class IdempotencyStore {

        private final long                      ttlMs;
        private final Map<String, ProcessedRecord> store = new ConcurrentHashMap<>();

        public IdempotencyStore(long ttlMs) {
            this.ttlMs = ttlMs;
        }

        /**
         * Marks a message as processed.
         * Returns NEW if it was not yet present, DUPLICATE if it was.
         */
        public DeduplicationResult markProcessed(String messageId) {
            evictExpired();
            ProcessedRecord existing = store.putIfAbsent(
                    messageId, new ProcessedRecord(messageId, Instant.now()));
            return existing == null ? DeduplicationResult.NEW : DeduplicationResult.DUPLICATE;
        }

        public boolean isProcessed(String messageId) {
            evictExpired();
            return store.containsKey(messageId);
        }

        private void evictExpired() {
            Instant cutoff = Instant.now().minusMillis(ttlMs);
            store.entrySet().removeIf(e -> e.getValue().processedAt().isBefore(cutoff));
        }

        public int size() {
            evictExpired();
            return store.size();
        }

        public void clear() { store.clear(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strategies
    // ─────────────────────────────────────────────────────────────────────────

    public record IdempotencyStrategy(
            String name,
            String storage,
            String pros,
            String cons) {}

    public static List<IdempotencyStrategy> idempotencyStrategies() {
        return List.of(
            new IdempotencyStrategy(
                "Redis SETNX + TTL",
                "Redis",
                "Fast; built-in TTL; shared across instances",
                "External dependency; TTL must exceed message retention"),
            new IdempotencyStrategy(
                "Database unique index",
                "RDBMS table with unique(message_id)",
                "Durable; works with existing DB; transactional",
                "Additional DB write per message; needs cleanup job"),
            new IdempotencyStrategy(
                "Kafka transactional consumer",
                "Kafka offset + transactional producer",
                "Exactly-once semantics if Kafka→Kafka only",
                "Complex; only works when both source and sink are Kafka"),
            new IdempotencyStrategy(
                "In-memory (bounded LRU)",
                "ConcurrentHashMap / Caffeine",
                "Zero latency; no external dependency",
                "Lost on restart; not shared across replicas")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Key design guide
    // ─────────────────────────────────────────────────────────────────────────

    public record KeyDesignRule(String recommendation, String example) {}

    public static List<KeyDesignRule> keyDesignGuide() {
        return List.of(
            new KeyDesignRule(
                "Use business identifier as message ID when possible",
                "orderId + ':' + eventType (e.g. 'ORD-123:CREATED')"),
            new KeyDesignRule(
                "Include version/sequence for idempotent updates",
                "orderId + ':' + version (e.g. 'ORD-123:v3')"),
            new KeyDesignRule(
                "Include source system to avoid cross-system collision",
                "source + ':' + businessId (e.g. 'payment-svc:PAY-456')"),
            new KeyDesignRule(
                "Set TTL ≥ message retention period",
                "If topic retention = 7 days, TTL = 8 days minimum")
        );
    }
}
