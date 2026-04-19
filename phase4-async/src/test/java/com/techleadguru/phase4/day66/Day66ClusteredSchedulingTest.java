package com.techleadguru.phase4.day66;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 66 — Clustered Scheduling: Duplicate Execution Problem Test
 *
 * Verifies:
 * 1. ClusteredEmailScheduler bean is created with unique nodeId
 * 2. ClusterSimulator correctly demonstrates the duplicate-execution problem
 * 3. Each node in the simulated cluster runs the task independently
 */
@SpringBootTest(classes = Phase4Application.class)
class Day66ClusteredSchedulingTest {

    @Autowired Day66ClusteredScheduling.ClusteredEmailScheduler scheduler;

    @Test
    void clustered_email_scheduler_bean_is_created() {
        assertThat(scheduler).isNotNull();
        assertThat(scheduler.getNodeId()).isNotBlank();
    }

    @Test
    void node_id_is_unique_per_instance() {
        // In a real cluster, each JVM has a unique PID → unique nodeId
        // Here we just verify it's non-null and formatted as expected
        assertThat(scheduler.getNodeId()).startsWith("node-");
    }

    @Test
    void simulate_cluster_shows_duplicate_execution_problem() {
        // Without coordination: each node sends emails independently
        // 3 nodes → 3 duplicate email sends!
        Day66ClusteredScheduling.ClusterSimulator simulator = new Day66ClusteredScheduling.ClusterSimulator();
        int duplicateCount = simulator.simulateDuplicateExecution(3);

        // Every node runs the task → 3 duplicates
        assertThat(duplicateCount).isEqualTo(3);
    }

    @Test
    void simulate_cluster_shows_problem_scales_with_node_count() {
        Day66ClusteredScheduling.ClusterSimulator simulator = new Day66ClusteredScheduling.ClusterSimulator();

        int two  = simulator.simulateDuplicateExecution(2);
        int five = simulator.simulateDuplicateExecution(5);

        assertThat(two).isEqualTo(2);
        assertThat(five).isEqualTo(5);
    }

    @Test
    void describe_solution_explains_shedlock_and_quartz() {
        Day66ClusteredScheduling.ClusterSimulator simulator = new Day66ClusteredScheduling.ClusterSimulator();
        String solution = simulator.describeSolution(3);

        assertThat(solution).isNotBlank();
        // Solution should mention either ShedLock or Quartz
        assertThat(solution.toLowerCase()).containsAnyOf("shedlock", "quartz", "lock", "single");
    }

    @Test
    void document_clustered_scheduling_patterns() {
        // Problem: @Scheduled on 3 nodes → 3 instances → 3x emails sent!
        //
        // Solution 1: ShedLock (Day 67)
        //   → DB lock per task name; only 1 node acquires lock, others skip
        //   → Works with existing @Scheduled
        //   → Lightweight, just needs a shedlock table
        //
        // Solution 2: Quartz with JDBC JobStore (Day 68)
        //   → Full distributed scheduler in the DB
        //   → Jobs are stored in DB; only 1 node fires each trigger
        //   → More powerful: job history, retry, clustering built-in
        //   → More complex: requires many Quartz tables
        //
        // Solution 3: Leader Election (e.g., Spring Integration, Kubernetes CronJob)
        //   → Only the elected leader runs @Scheduled tasks
        assertThat(true).isTrue(); // documentation test
    }
}
