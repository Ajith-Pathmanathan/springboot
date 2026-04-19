package com.techleadguru.phase7.day153;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Day 153 — Transactional Outbox Pattern
 *
 * Problem: writing to DB and publishing to Kafka in one atomic unit is hard.
 * If DB commit succeeds but Kafka publish fails, data and events diverge.
 *
 * Solution — Transactional Outbox:
 *   1. Write business entity change + outbox event in the SAME DB transaction
 *   2. A poller reads unpublished outbox events and publishes them to Kafka
 *   3. Mark event as published (or delete it)
 *
 * Guarantees at-least-once delivery (combine with idempotent consumer).
 * Implementations: polling (scheduler), CDC / Debezium (WAL log tailing).
 */
public class Day153TransactionalOutbox {

    // ─────────────────────────────────────────────────────────────────────────
    // Outbox event model
    // ─────────────────────────────────────────────────────────────────────────

    public record OutboxEvent(
            long    id,
            String  aggregateType,    // e.g. "Order"
            String  aggregateId,      // e.g. "ORD-123"
            String  eventType,        // e.g. "OrderCreated"
            String  payload,          // JSON
            Instant createdAt,
            Instant publishedAt) {    // null if not yet published

        public boolean isPublished() { return publishedAt != null; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // In-memory outbox store (simulates a DB table)
    // ─────────────────────────────────────────────────────────────────────────

    public static class OutboxStore {

        private final List<OutboxEvent>  events     = new CopyOnWriteArrayList<>();
        private final AtomicLong         sequence   = new AtomicLong(1);

        public OutboxEvent save(String aggregateType, String aggregateId,
                                String eventType, String payload) {
            OutboxEvent event = new OutboxEvent(
                    sequence.getAndIncrement(),
                    aggregateType, aggregateId, eventType, payload,
                    Instant.now(), null);
            events.add(event);
            return event;
        }

        public void markPublished(long id) {
            int idx = -1;
            for (int i = 0; i < events.size(); i++) {
                if (events.get(i).id() == id) { idx = i; break; }
            }
            if (idx >= 0) {
                OutboxEvent old = events.get(idx);
                events.set(idx, new OutboxEvent(
                        old.id(), old.aggregateType(), old.aggregateId(),
                        old.eventType(), old.payload(), old.createdAt(), Instant.now()));
            }
        }

        public List<OutboxEvent> findUnpublished(int limit) {
            return events.stream()
                    .filter(e -> !e.isPublished())
                    .limit(limit)
                    .toList();
        }

        public long publishedCount() {
            return events.stream().filter(OutboxEvent::isPublished).count();
        }

        public int totalCount() { return events.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Outbox pattern steps
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> outboxPatternSteps() {
        return List.of(
            "1. Begin DB transaction",
            "2. Persist business entity change (e.g. INSERT INTO orders ...)",
            "3. INSERT into outbox_events table in the SAME transaction",
            "4. Commit DB transaction — both records saved atomically",
            "5. Outbox poller runs (e.g. every 100ms) — SELECT unpublished events",
            "6. Publish each event to Kafka topic",
            "7. UPDATE outbox_events SET published_at = now() WHERE id = ?",
            "8. Optionally: delete old published events via scheduled cleanup"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Benefits
    // ─────────────────────────────────────────────────────────────────────────

    public record Benefit(String title, String description) {}

    public static List<Benefit> benefits() {
        return List.of(
            new Benefit("Atomicity",
                "Entity change and event are committed in one DB transaction — no split-brain"),
            new Benefit("At-least-once delivery",
                "Poller retries on publish failure; combine with idempotent consumer"),
            new Benefit("No 2PC required",
                "Avoids distributed transactions between DB and Kafka"),
            new Benefit("Audit trail",
                "Outbox table provides a history of all domain events"),
            new Benefit("Framework agnostic",
                "Works with any JPA/JDBC datastore; can use Debezium for zero-polling")
        );
    }
}
