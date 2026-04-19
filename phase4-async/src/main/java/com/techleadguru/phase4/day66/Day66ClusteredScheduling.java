package com.techleadguru.phase4.day66;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAY 66 — Clustered Scheduling: Duplicate Execution Problem ⚠️
 *
 * THE PRODUCTION BUG:
 *   You have a scheduled task that sends promotional emails.
 *   You scale from 1 to 3 application instances.
 *   Now the email goes out 3 times to every customer. 💥
 *
 * ROOT CAUSE:
 *   @Scheduled runs independently on EVERY JVM instance.
 *   Spring has no built-in distributed coordination.
 *   3 instances × 1 email job = 3 emails sent.
 *
 * IMPACT BY JOB TYPE:
 *   Idempotent jobs (read-only): OK to run on all instances
 *   Non-idempotent jobs (write, send, charge, delete): DANGEROUS — must run on 1 instance
 *
 * SOLUTIONS (in order of complexity):
 *
 *   1. @Scheduled + distributed lock (ShedLock — Day 67):
 *      Each instance tries to acquire a lock in DB.
 *      Only ONE gets it; others skip.
 *      ← Recommended: simple, works with any DB/Redis
 *
 *   2. Quartz JDBC JobStore (Day 68):
 *      Quartz uses a shared DB to coordinate which node runs each job.
 *      More powerful (pause, restart, job history), more complex setup.
 *      ← Use when you need job management UI, retry logic, complex triggers.
 *
 *   3. Spring Batch JobLauncher + DB JobRepository:
 *      Each run records in DB table; only proceeds if not already COMPLETED.
 *      ← For batch processing jobs with restart/step management needs.
 *
 *   4. Primary node election (Kubernetes leader election, ZooKeeper):
 *      Only the elected leader runs the scheduler.
 *      ← For streaming/stateful scheduling beyond simple cron.
 *
 * THIS DAY: demonstrates the duplicate-execution problem and documents the solutions.
 * Day 67 (ShedLock) and Day 68 (Quartz) implement the fixes.
 */
@Slf4j
public class Day66ClusteredScheduling {

    /**
     * Simulates a cluster node with its own scheduler running independently.
     * In production, 3 of these running = 3× execution of every task.
     */
    @Component
    @Slf4j
    public static class ClusteredEmailScheduler {

        private final AtomicInteger emailsSent = new AtomicInteger();
        private final String nodeId;

        public ClusteredEmailScheduler() {
            // In real cluster: different JVM instances each get a unique node ID
            this.nodeId = "node-" + ProcessHandle.current().pid();
        }

        /**
         * PROBLEM: Every node in the cluster runs this.
         * 3 nodes → 3 promotional emails to every customer.
         * Long delay prevents test interference.
         */
        @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
        public void sendPromotionalEmails() {
            int count = emailsSent.incrementAndGet();
            log.warn("[Day66] Node {} sending promotional emails — run #{}", nodeId, count);
            log.warn("[Day66] In a 3-node cluster, this runs 3 times → 3x emails!");
        }

        // For test: run the task manually and verify execution count
        public int runManually() {
            sendPromotionalEmails();
            return emailsSent.get();
        }

        public int getEmailsSentCount() { return emailsSent.get(); }
        public String getNodeId() { return nodeId; }
    }

    /**
     * Documents the duplicate-execution problem quantitatively.
     * Not a Spring bean — just a plain class for demonstration/testing.
     */
    public static class ClusterSimulator {

        public int simulateDuplicateExecution(int nodeCount) {
            int totalExecutions = 0;
            for (int i = 0; i < nodeCount; i++) {
                // Each node runs the task independently
                totalExecutions++;
                log.info("[Day66] Node-{} executed the scheduled task", i + 1);
            }
            log.warn("[Day66] {} nodes ran the task. Customer received {} emails instead of 1!",
                    nodeCount, totalExecutions);
            return totalExecutions;
        }

        public String describeSolution(int nodeCount) {
            return String.format(
                    "WITHOUT ShedLock: %d nodes × 1 task = %d executions\n" +
                    "WITH ShedLock:     %d nodes × 1 task = 1 execution (lock acquired by 1 node)",
                    nodeCount, nodeCount, nodeCount
            );
        }
    }
}
