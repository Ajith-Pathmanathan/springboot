package com.techleadguru.phase5.day85;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 85 — Slow DB Query Exhausts Connection Pool
 *
 * SCENARIO:
 *   HikariCP pool has max 5 connections (from test application.properties).
 *   A slow SQL query holds a connection for 30 seconds.
 *   Concurrent requests that also need DB connections queue up.
 *   When the queue fills, new requests get "Connection timeout: pool is exhausted".
 *
 * HIKARICP DEFAULTS (production-critical settings):
 *   maximumPoolSize  = 10     → max concurrent DB connections
 *   connectionTimeout = 30000 → time (ms) to wait for a connection before throwing
 *   idleTimeout      = 600000 → idle connection removed after 10 min
 *   maxLifetime      = 1800000 → connection replaced after 30 min (avoids stale TCP)
 *   keepaliveTime    = 0      → disabled; set to 60000 to ping idle connections
 *   leakDetectionThreshold = 0 → set to 2000 to log connections held > 2s
 *
 * DIAGNOSIS:
 *   1. HikariCP logs: "Connection timeout: Unable to acquire connection from pool after 30000ms"
 *   2. Actuator: GET /actuator/metrics/hikaricp.connections.active
 *      → active == maximumPoolSize and pending > 0 → pool exhausted
 *   3. Thread dump: all handler threads blocked on HikariCP.getConnection()
 *
 * FIX STRATEGIES:
 *   A. Add DB index (most bang for buck)
 *   B. Increase pool size (but DB has limits too)
 *   C. Move slow query to async / batch job (don't hold HTTP thread)
 *   D. Set leakDetectionThreshold to catch the culprit query
 *   E. Circuit breaker: fail fast instead of queueing forever
 */
@Slf4j
public class Day85SlowQueryPoolExhaustion {

    // =========================================================================
    // Entity — products table
    // =========================================================================

    @Entity
    @Table(name = "products_day85")
    public static class Product {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String name;
        private String category;
        private Double price;

        public Product() {}
        public Product(String name, String category, double price) {
            this.name = name; this.category = category; this.price = price;
        }
        public Long getId()         { return id; }
        public String getName()     { return name; }
        public String getCategory() { return category; }
        public Double getPrice()    { return price; }
    }

    // =========================================================================
    // Repository
    // =========================================================================

    public interface ProductRepository extends org.springframework.data.jpa.repository.JpaRepository<Product, Long> {

        /** SLOW: full table scan — no index on category */
        @org.springframework.data.jpa.repository.Query(
                value = "SELECT p FROM Day85SlowQueryPoolExhaustion$Product p WHERE p.category = :cat")
        List<Product> findByCategoryNoIndex(String cat);

        /** FAST: with proper @Index on the entity (or manually via DDL) */
        List<Product> findByCategory(String category);
    }

    // =========================================================================
    // Service — pool exhaustion demo
    // =========================================================================

    @Service
    @Slf4j
    public static class ProductService {

        private final ProductRepository repository;
        private final AtomicInteger successCount  = new AtomicInteger();
        private final AtomicInteger timeoutCount  = new AtomicInteger();

        public ProductService(ProductRepository repository) {
            this.repository = repository;
        }

        @Transactional
        public List<Product> findProductsByCategory(String category) {
            log.debug("[Day85] findByCategory={} on thread={}", category, Thread.currentThread().getName());
            List<Product> results = repository.findByCategory(category);
            log.debug("[Day85] Found {} products", results.size());
            successCount.incrementAndGet();
            return results;
        }

        @Transactional
        public Product save(Product p) { return repository.save(p); }

        @Transactional(readOnly = true)
        public long count() { return repository.count(); }

        public int getSuccessCount() { return successCount.get(); }
        public int getTimeoutCount() { return timeoutCount.get(); }

        /**
         * Simulates pool exhaustion by submitting more concurrent tasks
         * than the pool can handle. Some will get timeout exceptions.
         */
        public PoolExhaustionResult simulateExhaustion(int concurrentRequests,
                                                        HikariDataSource dataSource) throws InterruptedException {
            ExecutorService exec = Executors.newFixedThreadPool(concurrentRequests);
            CountDownLatch ready = new CountDownLatch(concurrentRequests);
            CountDownLatch done  = new CountDownLatch(concurrentRequests);
            AtomicInteger successes = new AtomicInteger();
            AtomicInteger timeouts  = new AtomicInteger();

            for (int i = 0; i < concurrentRequests; i++) {
                exec.submit(() -> {
                    ready.countDown();
                    try { ready.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    try {
                        // Try to get connection directly to simulate load
                        var conn = dataSource.getConnection();
                        Thread.sleep(50); // hold connection briefly
                        conn.close();
                        successes.incrementAndGet();
                    } catch (Exception e) {
                        timeouts.incrementAndGet();
                        log.debug("[Day85] Pool exhaustion: {}", e.getMessage());
                    } finally {
                        done.countDown();
                    }
                });
            }

            done.await(10, TimeUnit.SECONDS);
            exec.shutdown();

            int activePool = dataSource.getHikariPoolMXBean().getActiveConnections();
            int totalPool  = dataSource.getHikariPoolMXBean().getTotalConnections();

            return new PoolExhaustionResult(concurrentRequests, successes.get(), timeouts.get(),
                    activePool, totalPool);
        }

        public record PoolExhaustionResult(int requested, int succeeded, int timedOut,
                                           int activeConnections, int totalConnections) {}
    }
}
