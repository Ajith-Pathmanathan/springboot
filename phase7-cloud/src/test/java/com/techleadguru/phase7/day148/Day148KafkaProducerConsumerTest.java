package com.techleadguru.phase7.day148;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day148KafkaProducerConsumerTest {

    @Test
    void testPublishAndPoll() {
        Day148KafkaProducerConsumer.InMemoryKafkaBroker broker =
                new Day148KafkaProducerConsumer.InMemoryKafkaBroker();
        broker.publish("orders", "k1", "order-placed");
        broker.publish("orders", "k2", "order-confirmed");

        List<Day148KafkaProducerConsumer.KafkaMessage> messages =
                broker.poll("orders", "order-svc", 10);
        assertEquals(2, messages.size());
        assertEquals("order-placed", messages.get(0).value());
    }

    @Test
    void testPublishReturnsOffset() {
        Day148KafkaProducerConsumer.InMemoryKafkaBroker broker =
                new Day148KafkaProducerConsumer.InMemoryKafkaBroker();
        long offset0 = broker.publish("events", "k", "msg0");
        long offset1 = broker.publish("events", "k", "msg1");
        assertEquals(0L, offset0);
        assertEquals(1L, offset1);
    }

    @Test
    void testCommitOffsetAffectsPoll() {
        Day148KafkaProducerConsumer.InMemoryKafkaBroker broker =
                new Day148KafkaProducerConsumer.InMemoryKafkaBroker();
        broker.publish("orders", "k", "m0");
        broker.publish("orders", "k", "m1");
        broker.publish("orders", "k", "m2");

        // consume and commit first two
        List<Day148KafkaProducerConsumer.KafkaMessage> batch =
                broker.poll("orders", "svc", 2);
        assertEquals(2, batch.size());
        broker.commitOffset("orders", "svc", batch.get(batch.size() - 1).offset());

        // next poll should return only m2
        List<Day148KafkaProducerConsumer.KafkaMessage> next =
                broker.poll("orders", "svc", 10);
        assertEquals(1, next.size());
        assertEquals("m2", next.get(0).value());
    }

    @Test
    void testPendingCount() {
        Day148KafkaProducerConsumer.InMemoryKafkaBroker broker =
                new Day148KafkaProducerConsumer.InMemoryKafkaBroker();
        broker.publish("topic", "k", "a");
        broker.publish("topic", "k", "b");
        assertEquals(2, broker.pendingCount("topic", "grp"));
        broker.commitOffset("topic", "grp", 0L);
        assertEquals(1, broker.pendingCount("topic", "grp"));
    }

    @Test
    void testProducerProperties() {
        Map<String, String> props =
                Day148KafkaProducerConsumer.producerProperties("localhost:9092");
        assertTrue(props.containsKey("spring.kafka.bootstrap-servers"));
        assertEquals("all", props.get("spring.kafka.producer.acks"));
    }

    @Test
    void testConsumerProperties() {
        Map<String, String> props =
                Day148KafkaProducerConsumer.consumerProperties("localhost:9092", "my-group");
        assertEquals("my-group",
                props.get("spring.kafka.consumer.group-id"));
        assertEquals("earliest",
                props.get("spring.kafka.consumer.auto-offset-reset"));
    }

    @Test
    void testKeyPoints() {
        List<String> points = Day148KafkaProducerConsumer.keyPoints();
        assertFalse(points.isEmpty());
    }
}
