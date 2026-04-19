package com.techleadguru.phase5.day101;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;

class Day101DistributedLockTest {

    private Day101DistributedLock.InMemoryLockProvider provider;

    @BeforeEach
    void setUp() {
        provider = new Day101DistributedLock.InMemoryLockProvider();
    }

    @Test
    void inMemoryLock_acquired_when_key_not_held() {
        var handle = provider.tryAcquire("job:invoices", 5000);
        assertThat(handle).isPresent();
        assertThat(handle.get().getLockKey()).isEqualTo("job:invoices");
        handle.get().close();
    }

    @Test
    void inMemoryLock_rejected_when_key_already_held() {
        var first = provider.tryAcquire("job:invoices", 5000);
        assertThat(first).isPresent();

        var second = provider.tryAcquire("job:invoices", 5000);
        assertThat(second).isEmpty();

        first.get().close();
    }

    @Test
    void inMemoryLock_released_on_close() {
        var handle = provider.tryAcquire("job:email", 5000);
        assertThat(handle).isPresent();
        assertThat(provider.isHeld("job:email")).isTrue();

        handle.get().close();
        assertThat(provider.isHeld("job:email")).isFalse();
    }

    @Test
    void inMemoryLock_can_be_re_acquired_after_release() {
        var first = provider.tryAcquire("job:cleanup", 5000);
        first.get().close();

        var second = provider.tryAcquire("job:cleanup", 5000);
        assertThat(second).isPresent();
        second.get().close();
    }

    @Test
    void inMemoryLock_tracks_acquire_and_reject_counts() {
        var first = provider.tryAcquire("job:report", 5000);
        provider.tryAcquire("job:report", 5000); // rejected
        provider.tryAcquire("job:report", 5000); // rejected

        assertThat(provider.getAcquireCount()).isEqualTo(1);
        assertThat(provider.getRejectCount()).isEqualTo(2);
        first.get().close();
    }

    @Test
    void runWithLock_returns_true_when_lock_acquired() {
        AtomicBoolean ran = new AtomicBoolean(false);
        boolean result = Day101DistributedLock.runWithLock(provider, "job:test", 5000,
                () -> ran.set(true));
        assertThat(result).isTrue();
        assertThat(ran.get()).isTrue();
    }

    @Test
    void runWithLock_returns_false_when_lock_held() {
        var hold = provider.tryAcquire("job:held", 5000);
        boolean result = Day101DistributedLock.runWithLock(provider, "job:held", 5000, () -> {});
        assertThat(result).isFalse();
        hold.get().close();
    }

    @Test
    void supplyWithLock_returns_supplier_result_when_acquired() {
        var result = Day101DistributedLock.supplyWithLock(provider, "job:supply", 5000,
                () -> "hello");
        assertThat(result).isPresent().contains("hello");
    }

    @Test
    void supplyWithLock_returns_empty_when_lock_held() {
        var hold = provider.tryAcquire("job:supply2", 5000);
        var result = Day101DistributedLock.supplyWithLock(provider, "job:supply2", 5000,
                () -> "should-not-run");
        assertThat(result).isEmpty();
        hold.get().close();
    }

    @Test
    void redisLockDocumentation_setnxPattern_contains_setnx_keyword() {
        String pattern = Day101DistributedLock.RedisLockDocumentation.setnxPattern();
        assertThat(pattern).containsIgnoringCase("SET");
        assertThat(pattern).contains("NX");
    }

    @Test
    void redisLockDocumentation_redissonUsageExample_contains_tryLock() {
        String example = Day101DistributedLock.RedisLockDocumentation.redissonUsageExample();
        assertThat(example).contains("tryLock");
    }
}
