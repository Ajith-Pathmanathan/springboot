package com.techleadguru.phase7.day150;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day 150 — Kafka consumer groups and partition assignment
 *
 * Each Kafka topic has N partitions.
 * Within a consumer group, each partition is assigned to exactly one consumer.
 * Adding consumers up to the partition count increases parallelism.
 * Consumers > partitions means some consumers will be idle.
 */
public class Day150ConsumerGroups {

    // ─────────────────────────────────────────────────────────────────────────
    // Model
    // ─────────────────────────────────────────────────────────────────────────

    public record PartitionAssignment(String consumerId, List<Integer> partitions) {}

    public record ConsumerGroup(
            String                        groupId,
            List<String>                  members,
            int                           topicPartitionCount) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Consumer group simulator (range assignment strategy)
    // ─────────────────────────────────────────────────────────────────────────

    public static class ConsumerGroupSimulator {

        private final String          groupId;
        private final int             partitionCount;
        private final List<String>    members = new ArrayList<>();
        private final AtomicInteger   rebalanceCount = new AtomicInteger(0);

        private Map<String, List<Integer>> assignments = new HashMap<>();

        public ConsumerGroupSimulator(String groupId, int partitionCount) {
            this.groupId        = groupId;
            this.partitionCount = partitionCount;
        }

        public void join(String consumerId) {
            if (!members.contains(consumerId)) {
                members.add(consumerId);
                Collections.sort(members);
                rebalance();
            }
        }

        public void leave(String consumerId) {
            if (members.remove(consumerId)) {
                rebalance();
            }
        }

        private void rebalance() {
            rebalanceCount.incrementAndGet();
            assignments = new LinkedHashMap<>();

            if (members.isEmpty()) return;

            int memberCount = members.size();
            // Range assignment: distribute partitions as evenly as possible
            int partitionsPerMember = partitionCount / memberCount;
            int extra               = partitionCount % memberCount;
            int cursor = 0;
            for (int i = 0; i < memberCount; i++) {
                int count = partitionsPerMember + (i < extra ? 1 : 0);
                List<Integer> parts = new ArrayList<>();
                for (int j = 0; j < count; j++) parts.add(cursor++);
                assignments.put(members.get(i), parts);
            }
        }

        public Map<String, List<Integer>> getAssignments() {
            return Collections.unmodifiableMap(assignments);
        }

        public int rebalanceCount() { return rebalanceCount.get(); }
        public List<String> members() { return Collections.unmodifiableList(members); }
        public String groupId() { return groupId; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rebalance steps
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> rebalanceSteps() {
        return List.of(
            "1. Consumer joins or leaves the group (heartbeat timeout also triggers leave)",
            "2. Group coordinator (a Kafka broker) detects membership change",
            "3. All consumers in the group receive a 'rebalance needed' signal",
            "4. Each consumer stops processing and revokes its partitions",
            "5. Group coordinator collects JoinGroup requests from all members",
            "6. Leader consumer (first to join) computes new assignment",
            "7. Coordinator distributes assignment via SyncGroup response",
            "8. Each consumer resumes at committed offset for its new partitions"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scaling guide
    // ─────────────────────────────────────────────────────────────────────────

    public record ScalingRule(String scenario, String recommendation) {}

    public static List<ScalingRule> scalingGuide() {
        return List.of(
            new ScalingRule(
                "Consumers < partitions",
                "Some consumers handle multiple partitions — valid but less parallel"),
            new ScalingRule(
                "Consumers == partitions",
                "Optimal: each consumer owns exactly one partition"),
            new ScalingRule(
                "Consumers > partitions",
                "Extra consumers sit idle — no benefit, increase partitions instead"),
            new ScalingRule(
                "Scaling out consumers",
                "Add consumers up to partition count; increase partition count first if needed"),
            new ScalingRule(
                "Ordering guarantee",
                "Guaranteed within a partition; across partitions depends on key routing")
        );
    }
}
