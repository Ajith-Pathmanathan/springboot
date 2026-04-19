package com.techleadguru.phase2.day39;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAY 39 — Read Replica Routing with AbstractRoutingDataSource
 *
 * WHY READ REPLICAS?
 *   Most apps are read-heavy (80-90% reads, 10-20% writes).
 *   Problem: All reads and writes go to the PRIMARY DB → single-machine bottleneck.
 *   Solution: Add READ REPLICAS — separate DB instances that replicate from primary.
 *     - Writes → PRIMARY (master)
 *     - Reads  → REPLICA (slave / read replica)
 *   Result: 5-10x throughput for read-heavy workloads without app code changes.
 *
 * HOW AbstractRoutingDataSource WORKS:
 *   Spring's AbstractRoutingDataSource wraps multiple DataSources (primary + replicas).
 *   On each getConnection() call, it calls determineCurrentLookupKey() to decide which
 *   DataSource to use. You implement the routing logic in that method.
 *
 *   ROUTING LOGIC:
 *     - If transaction is read-only (@Transactional(readOnly=true)) → REPLICA
 *     - If transaction is read-write (@Transactional) → PRIMARY
 *     - If no transaction context → PRIMARY (safe default)
 *
 *   IMPLEMENTATION:
 *   1. Create RoutingDataSourceContextHolder (ThreadLocal for current key).
 *   2. Create CustomRoutingDataSource extends AbstractRoutingDataSource.
 *   3. Register both DataSources (primary + replica) with keys.
 *   4. Use AOP or @Transactional(readOnly=true) to set the context key.
 *
 * PRODUCTION CONSIDERATIONS:
 *   - Replication lag: replica may be 100ms-1s behind primary.
 *     Never read your own write from replica immediately after a write.
 *     Use @Transactional(readOnly=false) for operations that need consistent reads.
 *   - Health check: if replica goes down, failover to primary.
 *   - Multiple replicas: use round-robin among replicas for load distribution.
 *   - Session stickiness: some ORMs require all queries in a TX on same connection.
 *
 * THIS DEMO: Uses H2 for both primary and replica (same in-memory DB for simplicity).
 *            In production, these would be different servers.
 */
@Slf4j
public class Day39ReadReplicaRouting {

    // ===================================================================================
    // ThreadLocal context holder for routing key
    // ===================================================================================

    public static class RoutingDataSourceContext {

        private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

        public static void setDataSourceType(DataSourceType type) {
            CONTEXT.set(type);
        }

        public static DataSourceType getDataSourceType() {
            return CONTEXT.get();
        }

        public static void clear() {
            CONTEXT.remove();
        }

        public enum DataSourceType {
            PRIMARY, REPLICA
        }
    }

    // ===================================================================================
    // AbstractRoutingDataSource implementation
    // ===================================================================================

    /**
     * Custom routing data source that selects PRIMARY or REPLICA based on the
     * ThreadLocal context or transaction read-only flag.
     *
     * In this demo, the routing key is set manually via RoutingDataSourceContext.
     * In production, use an AOP aspect that reads @Transactional(readOnly=true).
     */
    public static class RoutingDataSource extends AbstractRoutingDataSource {

        @Override
        protected Object determineCurrentLookupKey() {
            RoutingDataSourceContext.DataSourceType type = RoutingDataSourceContext.getDataSourceType();
            String key = (type != null) ? type.name() : RoutingDataSourceContext.DataSourceType.PRIMARY.name();
            log.debug("[Day39] Routing to: {}", key);
            return key;
        }
    }

    // ===================================================================================
    // Service demonstrating read/write routing
    // ===================================================================================

    @Service
    @Slf4j
    public static class OrderReadService {

        /**
         * READ operation — routes to REPLICA.
         * @Transactional(readOnly=true) signals this is a read-only query.
         * In production: AOP intercepts readOnly=true and sets REPLICA context.
         */
        @Transactional(readOnly = true)
        public String readFromReplica(String simulatedData) {
            log.info("[Day39] Reading from REPLICA — data: {}", simulatedData);
            return "REPLICA:" + simulatedData;
        }

        /**
         * WRITE operation — routes to PRIMARY.
         * @Transactional (readOnly=false by default).
         */
        @Transactional
        public String writeToPrimary(String data) {
            log.info("[Day39] Writing to PRIMARY — data: {}", data);
            return "PRIMARY:" + data;
        }

        /**
         * Demonstrates routing decision logic.
         * Returns which data source would be selected for the current context.
         */
        public String getCurrentDataSourceType() {
            RoutingDataSourceContext.DataSourceType type = RoutingDataSourceContext.getDataSourceType();
            return type != null ? type.name() : "PRIMARY (default)";
        }
    }

    // ===================================================================================
    // AOP Aspect for automatic routing (production pattern)
    // ===================================================================================

    /**
     * PRODUCTION AOP ASPECT PATTERN (not registered as a bean in this demo, for reference):
     *
     *   @Aspect @Component
     *   public class ReadOnlyRoutingAspect {
     *       @Around("@annotation(transactional)")
     *       public Object route(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable {
     *           if (transactional.readOnly()) {
     *               RoutingDataSourceContext.setDataSourceType(REPLICA);
     *           }
     *           try {
     *               return pjp.proceed();
     *           } finally {
     *               RoutingDataSourceContext.clear();
     *           }
     *       }
     *   }
     *
     * This pattern automatically routes @Transactional(readOnly=true) to replica
     * and @Transactional to primary — zero code changes in services.
     */
    public static class ReadOnlyRoutingAspectDocumentation {}
}
