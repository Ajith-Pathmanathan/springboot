package com.techleadguru.phase4.day76;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 76 — BlockingQueue & Producer-Consumer Pattern
 *
 * BLOCKING QUEUE — the right abstraction for producer-consumer:
 *
 *   put(item)   → blocks if queue is FULL  (backpressure on producer)
 *   take()      → blocks if queue is EMPTY (consumer waits for work)
 *   offer(item) → non-blocking put, returns false if full
 *   poll()      → non-blocking take, returns null if empty
 *   offer(item, timeout, unit) → timed put
 *   poll(timeout, unit)        → timed take
 *
 * IMPLEMENTATIONS:
 *   ArrayBlockingQueue(capacity)   → bounded FIFO, array-backed, fair option
 *   LinkedBlockingQueue(capacity)  → bounded FIFO, linked list, higher throughput
 *   LinkedBlockingQueue()          → UNBOUNDED — careful! OOM if producer >> consumer
 *   PriorityBlockingQueue          → unbounded, ordered by Comparable
 *   SynchronousQueue               → zero-capacity — direct handoff producer→consumer
 *   DelayQueue                     → elements become available only after delay
 *
 * BACKPRESSURE:
 *   Bounded queue + put() blocks = natural backpressure.
 *   When the queue is full, the producer's put() call blocks, slowing the producer
 *   to match the consumer's processing rate. This prevents OOM from unbounded queues.
 *
 * SHUTDOWN PATTERN — poison pill:
 *   Producer sends a special "POISON PILL" sentinel value.
 *   Consumer receives the pill → stops processing → exits gracefully.
 *
 * REAL-WORLD USES:
 *   - Thread pool's work queue (ExecutorService uses BlockingQueue internally)
 *   - Email/SMS outbox: producer adds jobs, consumer sends them
 *   - Log buffer: business code puts log entries, background thread writes to file
 *   - Event pipeline: source produces events, processor consumes+transforms them
 */
@Slf4j
public class Day76BlockingQueue {

    // Poison pill sentinel for shutdown
    public static final String POISON_PILL = "__SHUTDOWN__";

    // =========================================================================
    // Email queue service — realistic use case
    // =========================================================================

    @Slf4j
    public static class EmailQueueService {

        private final BlockingQueue<EmailTask> queue;
        private final AtomicInteger sent       = new AtomicInteger(0);
        private final AtomicInteger dropped    = new AtomicInteger(0);
        private volatile boolean running       = true;

        public EmailQueueService(int maxQueueSize) {
            this.queue = new ArrayBlockingQueue<>(maxQueueSize);
        }

        /**
         * Producer method: add email to queue.
         * If queue is full, drops the email (use offer) rather than blocking the HTTP thread.
         */
        public boolean submitEmail(String to, String subject) {
            EmailTask task = new EmailTask(to, subject);
            boolean added = queue.offer(task); // non-blocking
            if (!added) {
                log.warn("Email queue FULL — dropping email to {}", to);
                dropped.incrementAndGet();
            }
            return added;
        }

        /**
         * Consumer method: continuously takes from queue and "sends" emails.
         * Blocks on take() when queue is empty — no busy-waiting.
         * Runs until shutdown() is called.
         */
        public void startConsumer() {
            Thread worker = new Thread(() -> {
                log.info("Email consumer started on {}", Thread.currentThread().getName());
                while (running || !queue.isEmpty()) {
                    try {
                        EmailTask task = queue.poll(200, TimeUnit.MILLISECONDS);
                        if (task != null) {
                            sendEmail(task);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                log.info("Email consumer stopped. Sent: {}", sent.get());
            }, "email-sender");
            worker.setDaemon(true);
            worker.start();
        }

        private void sendEmail(EmailTask task) {
            // Simulate sending — in real code: JavaMailSender, SES, SendGrid, etc.
            log.info("Sending email to {} subject='{}'", task.to(), task.subject());
            sent.incrementAndGet();
        }

        public void shutdown()         { running = false; }
        public int getSentCount()      { return sent.get(); }
        public int getDroppedCount()   { return dropped.get(); }
        public int getQueueSize()      { return queue.size(); }
        public boolean isQueueFull()   { return !queue.offer(new EmailTask("_test_", "")); }

        public record EmailTask(String to, String subject) {}
    }

    // =========================================================================
    // Classic producer-consumer with poison pill shutdown
    // =========================================================================

    @Slf4j
    public static class ProducerConsumerDemo {

        private final BlockingQueue<String> queue;
        private final List<String> processed = new CopyOnWriteArrayList<>();
        private final int numConsumers;

        public ProducerConsumerDemo(int capacity, int numConsumers) {
            this.queue = new ArrayBlockingQueue<>(capacity);
            this.numConsumers = numConsumers;
        }

        /**
         * Producer: puts items into the queue.
         * put() BLOCKS if queue is full → natural backpressure.
         */
        public Thread startProducer(List<String> items) {
            Thread producer = new Thread(() -> {
                for (String item : items) {
                    try {
                        queue.put(item);  // blocks when full!
                        log.info("Produced: {} (queue size: {})", item, queue.size());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                // Send one poison pill per consumer to signal shutdown
                for (int i = 0; i < numConsumers; i++) {
                    try { queue.put(POISON_PILL); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
            }, "producer");
            producer.setDaemon(true);
            producer.start();
            return producer;
        }

        /**
         * Consumer: takes items and processes them.
         * take() BLOCKS if queue is empty — no busy-waiting.
         * Stops when it receives the POISON_PILL.
         */
        public List<Thread> startConsumers(int count) {
            List<Thread> consumers = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                final int id = i;
                Thread consumer = new Thread(() -> {
                    while (true) {
                        try {
                            String item = queue.take(); // blocks when empty!
                            if (POISON_PILL.equals(item)) {
                                log.info("Consumer-{} received shutdown signal", id);
                                return;
                            }
                            processed.add(item);
                            log.info("Consumer-{} processed: {}", id, item);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }, "consumer-" + i);
                consumer.setDaemon(true);
                consumer.start();
                consumers.add(consumer);
            }
            return consumers;
        }

        public List<String> getProcessed()   { return List.copyOf(processed); }
        public int getQueueSize()            { return queue.size(); }
    }

    // =========================================================================
    // Backpressure demonstration: queue caps throughput
    // =========================================================================

    public static class BackpressureDemo {

        /**
         * Shows that a bounded queue slows the producer when consumer is slower.
         * Producer can only advance as fast as the consumer drains items.
         */
        public BackpressureResult run(int queueCapacity, int producerItems,
                                      long consumerDelayMs) throws InterruptedException {
            BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(queueCapacity);
            AtomicInteger produced = new AtomicInteger(0);
            AtomicInteger consumed = new AtomicInteger(0);
            CountDownLatch done = new CountDownLatch(1);

            // Slow consumer
            Thread consumer = new Thread(() -> {
                while (true) {
                    try {
                        Integer item = queue.poll(500, TimeUnit.MILLISECONDS);
                        if (item == null) { done.countDown(); return; }
                        Thread.sleep(consumerDelayMs);
                        consumed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });
            consumer.setDaemon(true);
            consumer.start();

            // Fast producer — will block when queue fills up
            long start = System.currentTimeMillis();
            for (int i = 0; i < producerItems; i++) {
                queue.put(i); // BLOCKS when queue full → backpressure!
                produced.incrementAndGet();
            }
            done.await(60, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - start;

            log.info("Produced: {} | Consumed: {} | Elapsed: {}ms", produced.get(), consumed.get(), elapsed);
            return new BackpressureResult(produced.get(), consumed.get(), queueCapacity, elapsed);
        }

        public record BackpressureResult(int produced, int consumed, int maxQueueSize, long elapsedMs) {}
    }
}
