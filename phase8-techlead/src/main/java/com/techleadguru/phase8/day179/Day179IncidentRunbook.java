package com.techleadguru.phase8.day179;

import java.util.*;

/**
 * Day 179 — Incident Runbook: OOM, Pool Exhaustion, Deadlock
 *
 * A runbook is a step-by-step guide for diagnosing and resolving a specific
 * incident type. It should be executable under stress, with concrete commands.
 */
public class Day179IncidentRunbook {

    // ─────────────────────────────────────────────────────────────────────────
    // Incident types
    // ─────────────────────────────────────────────────────────────────────────

    public enum IncidentType {
        OOM,               // OutOfMemoryError / Heap exhaustion
        POOL_EXHAUSTION,   // DB connection pool / thread pool full
        DEADLOCK,          // Thread or DB deadlock
        HIGH_LATENCY,      // P99 latency spikes
        DISK_FULL          // Disk space exhausted
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Runbook step
    // ─────────────────────────────────────────────────────────────────────────

    public record RunbookStep(
            int    order,
            String action,
            String command,
            String expectedOutcome) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Incident runbook
    // ─────────────────────────────────────────────────────────────────────────

    public record IncidentRunbook(
            IncidentType  type,
            List<String>  symptoms,
            List<RunbookStep> steps,
            List<String>  preventionActions) {}

    // ─────────────────────────────────────────────────────────────────────────
    // OOM runbook
    // ─────────────────────────────────────────────────────────────────────────

    public static IncidentRunbook oomRunbook() {
        return new IncidentRunbook(
            IncidentType.OOM,
            List.of(
                "java.lang.OutOfMemoryError: Java heap space in logs",
                "GC overhead limit exceeded",
                "Pod OOMKilled in Kubernetes (exit code 137)",
                "Response times degrade then service crashes"
            ),
            List.of(
                new RunbookStep(1,
                    "Capture heap dump before restart if possible",
                    "jcmd <pid> GC.heap_dump /tmp/heap.hprof  OR  kubectl exec <pod> -- jmap -dump:format=b,file=/tmp/heap.hprof <pid>",
                    "heap.hprof file created"),
                new RunbookStep(2,
                    "Restart pod to restore service",
                    "kubectl rollout restart deployment/<name>",
                    "Pod back to Running state"),
                new RunbookStep(3,
                    "Check heap usage trend in Grafana",
                    "query: jvm_memory_used_bytes{area='heap'} / jvm_memory_max_bytes{area='heap'}",
                    "Identify if heap grew linearly (leak) or spiked (large object)"),
                new RunbookStep(4,
                    "Analyse heap dump with Eclipse MAT",
                    "Open heap.hprof in Eclipse MAT → Leak Suspects report",
                    "Identify top retained object by class/package"),
                new RunbookStep(5,
                    "Check for unbounded caches or collections",
                    "grep -r 'new ArrayList\\|new HashMap' src/ | grep -v 'Collections.unmodifiable'",
                    "Find static/singleton-held growing collections"),
                new RunbookStep(6,
                    "Increase heap as temporary mitigation",
                    "JAVA_OPTS=-Xmx2g — update Kubernetes Deployment env",
                    "Buys time while root cause is fixed")
            ),
            List.of(
                "Set JVM heap limits (-Xms / -Xmx) and match Kubernetes memory limits",
                "Add JVM GC metrics to Grafana dashboard",
                "Configure HeapDump on OOM: -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp",
                "Review unbounded caches — use Caffeine with maximumSize",
                "Run load tests to catch memory leaks before production"
            )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pool exhaustion runbook
    // ─────────────────────────────────────────────────────────────────────────

    public static IncidentRunbook poolExhaustionRunbook() {
        return new IncidentRunbook(
            IncidentType.POOL_EXHAUSTION,
            List.of(
                "HikariPool-1 - Connection is not available, request timed out after 30000ms",
                "Unable to acquire JDBC Connection",
                "All threads blocked waiting for DB connection",
                "Service health check fails; 503s returned"
            ),
            List.of(
                new RunbookStep(1,
                    "Check current active connection count",
                    "SELECT count(*) FROM pg_stat_activity WHERE state='active';",
                    "Compare against pool max-size"),
                new RunbookStep(2,
                    "Find long-running queries holding connections",
                    "SELECT pid, query, state, query_start FROM pg_stat_activity WHERE state != 'idle' ORDER BY query_start;",
                    "Identify blocked/long queries"),
                new RunbookStep(3,
                    "Terminate long-running queries if safe",
                    "SELECT pg_cancel_backend(<pid>);  // soft cancel",
                    "Query cancels; connection returned to pool"),
                new RunbookStep(4,
                    "Increase pool size as temporary mitigation",
                    "spring.datasource.hikari.maximum-pool-size=20 — rolling restart",
                    "Pool expands; connections available"),
                new RunbookStep(5,
                    "Check for open transactions not committed",
                    "SELECT * FROM pg_stat_activity WHERE state='idle in transaction';",
                    "Find leaking transactions; kill if safe"),
                new RunbookStep(6,
                    "Review service code for connection leaks",
                    "Search for manual getConnection() without try-with-resources",
                    "Root cause identified; fix deployed")
            ),
            List.of(
                "Set hikari.connection-timeout=30000 and hikari.max-lifetime=1800000",
                "Enable Hikari metrics: management.metrics.enable.hikari=true",
                "Alert on: hikaricp_connections_active / hikaricp_connections_max > 0.8",
                "Use @Transactional(readOnly=true) for read-only queries (separate read pool possible)",
                "Add statement timeout: spring.datasource.hikari.connection-init-sql=SET statement_timeout='30s'"
            )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deadlock runbook
    // ─────────────────────────────────────────────────────────────────────────

    public static IncidentRunbook deadlockRunbook() {
        return new IncidentRunbook(
            IncidentType.DEADLOCK,
            List.of(
                "ERROR: could not serialize access due to concurrent update (PostgreSQL)",
                "DeadlockLoserDataAccessException in Spring logs",
                "Threads blocked in java.lang.Thread.State: BLOCKED in thread dump",
                "Circular wait in thread dump: Thread-A waits for Thread-B holds lock"
            ),
            List.of(
                new RunbookStep(1,
                    "Capture thread dump to detect JVM deadlock",
                    "jcmd <pid> Thread.print  OR  kill -3 <pid>  OR  kubectl exec <pod> -- jstack <pid>",
                    "Thread dump shows 'Found one Java-level deadlock' section"),
                new RunbookStep(2,
                    "Check PostgreSQL for DB-level deadlocks",
                    "SELECT * FROM pg_stat_activity WHERE wait_event_type='Lock';",
                    "Shows rows waiting on locks"),
                new RunbookStep(3,
                    "Find the circular lock dependency in thread dump",
                    "Search for 'waiting to lock' and 'locked' pairs in thread dump",
                    "Identify the two threads and two lock objects"),
                new RunbookStep(4,
                    "For DB deadlock: check lock acquisition order in code",
                    "grep -r 'SELECT.*FOR UPDATE' src/  to find lock ordering",
                    "Identify if two tables are locked in different order in different transactions"),
                new RunbookStep(5,
                    "Apply deadlock retry (short-term fix)",
                    "Add @Retryable(value=DeadlockLoserDataAccessException.class, maxAttempts=3)",
                    "Retries on deadlock; service recovers automatically"),
                new RunbookStep(6,
                    "Fix lock ordering (root cause)",
                    "Ensure all transactions lock tables/rows in the same canonical order",
                    "Deadlock cannot occur; fix verified in load test")
            ),
            List.of(
                "Enforce consistent lock acquisition order across all service transactions",
                "Use @Transactional with shortest possible scope",
                "Prefer optimistic locking (@Version) over pessimistic (SELECT FOR UPDATE) where possible",
                "Set deadlock_timeout in PostgreSQL and log deadlocks: log_lock_waits=on",
                "Add deadlock retry with exponential back-off (@Retryable)"
            )
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Diagnostic commands reference
    // ─────────────────────────────────────────────────────────────────────────

    public static Map<String, String> diagnosticCommands() {
        return Map.of(
            "JVM heap dump",          "jcmd <pid> GC.heap_dump /tmp/heap.hprof",
            "JVM thread dump",        "jcmd <pid> Thread.print",
            "GC stats",               "jstat -gcutil <pid> 1000 10",
            "Open file descriptors",  "lsof -p <pid> | wc -l",
            "PG active queries",      "SELECT pid,query,state,query_start FROM pg_stat_activity;",
            "PG lock waits",          "SELECT * FROM pg_stat_activity WHERE wait_event_type='Lock';",
            "PG kill query",          "SELECT pg_cancel_backend(<pid>);",
            "Kubernetes OOMKilled",   "kubectl describe pod <name> | grep -A5 'OOMKilled'"
        );
    }
}
