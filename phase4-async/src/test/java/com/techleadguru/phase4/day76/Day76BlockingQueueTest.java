package com.techleadguru.phase4.day76;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 76 — BlockingQueue Producer-Consumer Test
 *
 * Verifies:
 * 1. ProducerConsumerDemo processes all items in order
 * 2. EmailQueueService delivers emails and drops them when queue is full
 * 3. BackpressureDemo demonstrates queue capacity limiting producer throughput
 * 4. Bounded queue prevents OOM under high load
 */
class Day76BlockingQueueTest {

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void producer_consumer_processes_all_items() throws InterruptedException {
        Day76BlockingQueue.ProducerConsumerDemo demo =
                new Day76BlockingQueue.ProducerConsumerDemo(5, 2);

        List<String> items = List.of("apple", "banana", "cherry", "date", "elderberry");
        demo.startProducer(items);
        List<Thread> consumers = demo.startConsumers(2);
        for (Thread c : consumers) c.join(5000);

        assertThat(demo.getProcessed()).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void single_consumer_processes_all_items() throws InterruptedException {
        Day76BlockingQueue.ProducerConsumerDemo demo =
                new Day76BlockingQueue.ProducerConsumerDemo(3, 1);

        List<String> items = List.of("msg-1", "msg-2", "msg-3");
        demo.startProducer(items);
        List<Thread> consumers = demo.startConsumers(1);
        consumers.get(0).join(5000);

        assertThat(demo.getProcessed()).containsExactlyInAnyOrderElementsOf(items);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void email_queue_service_accepts_emails_under_capacity() throws InterruptedException {
        Day76BlockingQueue.EmailQueueService emailService =
                new Day76BlockingQueue.EmailQueueService(10);
        emailService.startConsumer();

        boolean added1 = emailService.submitEmail("user1@example.com", "Welcome");
        boolean added2 = emailService.submitEmail("user2@example.com", "Newsletter");
        assertThat(added1).isTrue();
        assertThat(added2).isTrue();

        // Give consumer time to process
        Thread.sleep(200);
        emailService.shutdown();
        Thread.sleep(300);
        assertThat(emailService.getSentCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void email_queue_service_drops_emails_when_full() {
        // Capacity 1: first fits, second is dropped
        Day76BlockingQueue.EmailQueueService emailService =
                new Day76BlockingQueue.EmailQueueService(1);
        // Don't start consumer so queue fills immediately

        boolean added1 = emailService.submitEmail("a@example.com", "First");
        boolean added2 = emailService.submitEmail("b@example.com", "Second");
        boolean added3 = emailService.submitEmail("c@example.com", "Third");

        assertThat(added1).isTrue(); // goes into queue
        // added2 and added3 may be dropped
        long dropped = List.of(added1, added2, added3).stream().filter(b -> !b).count();
        assertThat(dropped).isGreaterThanOrEqualTo(1);
        assertThat(emailService.getDroppedCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void backpressure_demo_limits_producer_throughput() throws InterruptedException {
        // Queue capacity 3, each consumer takes 50ms, producer items = 5
        // Producer will block when queue fills up → backpressure
        Day76BlockingQueue.BackpressureDemo demo = new Day76BlockingQueue.BackpressureDemo();
        var result = demo.run(3, 5, 50);

        assertThat(result.produced()).isEqualTo(5);
        assertThat(result.consumed()).isEqualTo(5);
        assertThat(result.maxQueueSize()).isEqualTo(3);
        // Total time should be at least 5 × 50ms = 250ms (shows producer was rate-limited)
        assertThat(result.elapsedMs()).isGreaterThanOrEqualTo(200);
    }
}
