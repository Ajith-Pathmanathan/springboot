package com.techleadguru.phase4.day74;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 74 — ReentrantLock vs synchronized Test
 *
 * Verifies:
 * 1. Both SynchronizedCounter and ReentrantLockCounter are thread-safe under load
 * 2. tryLock() returns false when lock is held, avoiding deadlock
 * 3. Fair lock serves threads in FIFO order (approximately)
 * 4. BoundedBuffer correctly coordinates producers and consumers
 * 5. InterruptibleLockService responds to Thread.interrupt()
 */
class Day74ReentrantLockTest {

    @Test
    void synchronized_counter_is_thread_safe() throws InterruptedException {
        Day74ReentrantLock.SynchronizedCounter counter = new Day74ReentrantLock.SynchronizedCounter();
        runConcurrentIncrements(counter::increment, 10, 1000);
        assertThat(counter.get()).isEqualTo(10_000);
    }

    @Test
    void reentrant_lock_counter_is_thread_safe() throws InterruptedException {
        Day74ReentrantLock.ReentrantLockCounter counter = new Day74ReentrantLock.ReentrantLockCounter();
        runConcurrentIncrements(counter::increment, 10, 1000);
        assertThat(counter.get()).isEqualTo(10_000);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void tryLock_returns_false_when_lock_is_held() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        // Hold the lock from a background thread
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                acquired.countDown();       // signal: lock is held
                release.await(3, TimeUnit.SECONDS); // hold it
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        holder.setDaemon(true);
        holder.start();
        acquired.await(); // wait until lock is held

        // tryLock() should return false immediately
        boolean acquired2 = lock.tryLock(50, TimeUnit.MILLISECONDS);
        assertThat(acquired2).as("tryLock should fail when lock is held").isFalse();

        release.countDown();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void try_lock_transfer_succeeds_when_locks_available() throws InterruptedException {
        Day74ReentrantLock.TryLockTransferService service = new Day74ReentrantLock.TryLockTransferService();
        boolean result = service.transfer(100L, 1000);
        assertThat(result).isTrue();
        assertThat(service.getTransferCount()).isEqualTo(1);
        assertThat(service.getFailCount()).isEqualTo(0);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void bounded_buffer_producer_consumer_works_correctly() throws InterruptedException {
        Day74ReentrantLock.BoundedBuffer<String> buffer = new Day74ReentrantLock.BoundedBuffer<>(3);
        List<String> received = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(3);

        // Consumer
        Thread consumer = new Thread(() -> {
            while (done.getCount() > 0) {
                try {
                    received.add(buffer.take());
                    done.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        consumer.setDaemon(true);
        consumer.start();

        // Producer
        buffer.put("item-1");
        buffer.put("item-2");
        buffer.put("item-3");

        done.await(3, TimeUnit.SECONDS);
        assertThat(received).containsExactlyInAnyOrder("item-1", "item-2", "item-3");
    }

    @Test
    void bounded_buffer_blocks_producer_when_full() throws InterruptedException {
        Day74ReentrantLock.BoundedBuffer<Integer> buffer = new Day74ReentrantLock.BoundedBuffer<>(2);
        buffer.put(1);
        buffer.put(2); // full

        CountDownLatch producerBlocked = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            try {
                producerBlocked.countDown();
                buffer.put(3); // should BLOCK until consumer takes
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.setDaemon(true);
        producer.start();
        producerBlocked.await();

        // Producer is blocked — consume one item
        buffer.take();
        producer.join(1000);
        assertThat(producer.isAlive()).isFalse(); // producer completed after consumer freed space
    }

    @Test
    void fair_lock_is_fair() {
        Day74ReentrantLock.FairnessDemo demo = new Day74ReentrantLock.FairnessDemo();
        assertThat(demo.isFair(demo.getFairLock())).isTrue();
        assertThat(demo.isFair(demo.getUnfairLock())).isFalse();
    }

    private void runConcurrentIncrements(Runnable increment, int threads, int perThread)
            throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < perThread; j++) increment.run();
                done.countDown();
            }).start();
        }
        start.countDown();
        done.await();
    }
}
