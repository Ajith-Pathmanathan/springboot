package com.techleadguru.phase7.day148;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Day 148 — Kafka producer / consumer fundamentals
 *
 * Apache Kafka: distributed log, topics divided into partitions.
 * Producers write to a partition (key-based or round-robin).
 * Consumers poll from offset; commit offset to mark progress.
 * Consumer groups: each partition assigned to one consumer per group.
 */
public class Day148KafkaProducerConsumer {

    // ─────────────────────────────────────────────────────────────────────────
    // Message model
    // ─────────────────────────────────────────────────────────────────────────

    public record KafkaMessage(
            String topic,
            String key,
            String value,
            int    partition,
            long   offset) {}

    // ─────────────────────────────────────────────────────────────────────────
    // In-memory broker (single partition per topic for simplicity)
    // ─────────────────────────────────────────────────────────────────────────

    public static class InMemoryKafkaBroker {

        private final Map<String, List<KafkaMessage>> topics    = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong>         offsets   = new ConcurrentHashMap<>();
        private final Map<String, Long>               committed = new ConcurrentHashMap<>();

        /** Publish a message; returns the assigned offset. */
        public long publish(String topic, String key, String value) {
            topics.computeIfAbsent(topic, t -> new CopyOnWriteArrayList<>());
            offsets.computeIfAbsent(topic, t -> new AtomicLong(0));

            long offset = offsets.get(topic).getAndIncrement();
            KafkaMessage msg = new KafkaMessage(topic, key, value, 0, offset);
            topics.get(topic).add(msg);
            return offset;
        }

        /**
         * Poll up to maxMessages from the given consumer group's committed offset.
         * Returns list of messages and advances the internal cursor.
         */
        public List<KafkaMessage> poll(String topic, String consumerGroup, int maxMessages) {
            List<KafkaMessage> all = topics.getOrDefault(topic, List.of());
            long fromOffset = committed.getOrDefault(consumerGroup + ":" + topic, 0L);
            List<KafkaMessage> batch = all.stream()
                    .filter(m -> m.offset() >= fromOffset)
                    .limit(maxMessages)
                    .toList();
            return batch;
        }

        /** Commit offset (next expected offset = last consumed + 1). */
        public void commitOffset(String topic, String consumerGroup, long offset) {
            committed.put(consumerGroup + ":" + topic, offset + 1);
        }

        public long committedOffset(String topic, String consumerGroup) {
            return committed.getOrDefault(consumerGroup + ":" + topic, 0L);
        }

        public int pendingCount(String topic, String consumerGroup) {
            List<KafkaMessage> all = topics.getOrDefault(topic, List.of());
            long from = committed.getOrDefault(consumerGroup + ":" + topic, 0L);
            return (int) all.stream().filter(m -> m.offset() >= from).count();
        }

        public void clear() {
            topics.clear();
            offsets.clear();
            committed.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Spring Kafka configuration properties
    // ─────────────────────────────────────────────────────────────────────────

    public record ProducerConfigProperties(
            String bootstrapServers,
            String keySerializer,
            String valueSerializer,
            String acks) {}

    public record ConsumerConfigProperties(
            String bootstrapServers,
            String groupId,
            String keyDeserializer,
            String valueDeserializer,
            String autoOffsetReset) {}

    public static Map<String, String> producerProperties(String bootstrapServers) {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("spring.kafka.bootstrap-servers",               bootstrapServers);
        props.put("spring.kafka.producer.key-serializer",         "org.apache.kafka.common.serialization.StringSerializer");
        props.put("spring.kafka.producer.value-serializer",       "org.apache.kafka.common.serialization.StringSerializer");
        props.put("spring.kafka.producer.acks",                   "all");
        props.put("spring.kafka.producer.retries",                "3");
        props.put("spring.kafka.producer.properties.enable.idempotence", "true");
        return props;
    }

    public static Map<String, String> consumerProperties(String bootstrapServers, String groupId) {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("spring.kafka.bootstrap-servers",               bootstrapServers);
        props.put("spring.kafka.consumer.group-id",               groupId);
        props.put("spring.kafka.consumer.key-deserializer",       "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("spring.kafka.consumer.value-deserializer",     "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("spring.kafka.consumer.auto-offset-reset",      "earliest");
        props.put("spring.kafka.consumer.enable-auto-commit",     "false");
        return props;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Key points
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> keyPoints() {
        return List.of(
            "Partitions provide parallelism — one consumer per partition per group",
            "Message key determines partition (null key = round-robin)",
            "acks=all ensures durability (written to all in-sync replicas)",
            "enable.idempotence=true prevents duplicate delivery on retry",
            "Consumer commits offset after processing (manual preferably)",
            "auto-offset-reset=earliest replays from beginning if no committed offset",
            "Consumer group rebalancing triggered on member join/leave"
        );
    }
}
