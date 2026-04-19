package com.techleadguru.phase7.day136;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Day 136 — Spring Cloud LoadBalancer: round-robin and weighted distribution.
 *
 * Spring Cloud LoadBalancer replaces the deprecated Ribbon.
 * It is integrated with the Eureka registry — instances are fetched from there.
 *
 * Built-in strategies:
 *   - RoundRobinLoadBalancer   (default)
 *   - RandomLoadBalancer
 *
 * Custom strategies: implement ReactorServiceInstanceLoadBalancer.
 *
 * Spring WebClient usage:
 *   @Bean
 *   @LoadBalanced
 *   public WebClient.Builder webClientBuilder() { return WebClient.builder(); }
 *
 *   // URL is symbolic — resolved via load balancer
 *   webClientBuilder.build().get().uri("http://order-service/orders").retrieve()...
 */
public class Day136LoadBalancer {

    // ─────────────────────────────────────────────────────────────────────────
    // Service instance model
    // ─────────────────────────────────────────────────────────────────────────

    public record ServiceEndpoint(String instanceId, String host, int port, int weight) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Round-robin load balancer (pure Java simulation)
    // ─────────────────────────────────────────────────────────────────────────

    public static class RoundRobinBalancer {

        private final List<ServiceEndpoint> instances;
        private final AtomicInteger counter = new AtomicInteger(0);

        public RoundRobinBalancer(List<ServiceEndpoint> instances) {
            this.instances = List.copyOf(instances);
        }

        /** Choose the next instance in round-robin order. Returns empty if no instances. */
        public Optional<ServiceEndpoint> choose() {
            if (instances.isEmpty()) return Optional.empty();
            int idx = Math.abs(counter.getAndIncrement() % instances.size());
            return Optional.of(instances.get(idx));
        }

        public int instanceCount() { return instances.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Weighted random load balancer
    // ─────────────────────────────────────────────────────────────────────────

    public static class WeightedBalancer {

        private final List<ServiceEndpoint> instances;
        private final Random random;

        public WeightedBalancer(List<ServiceEndpoint> instances) {
            this(instances, new Random());
        }

        WeightedBalancer(List<ServiceEndpoint> instances, Random random) {
            this.instances = List.copyOf(instances);
            this.random = random;
        }

        public Optional<ServiceEndpoint> choose() {
            if (instances.isEmpty()) return Optional.empty();
            int totalWeight = instances.stream().mapToInt(ServiceEndpoint::weight).sum();
            int roll = random.nextInt(totalWeight);
            int cumulative = 0;
            for (ServiceEndpoint ep : instances) {
                cumulative += ep.weight();
                if (roll < cumulative) return Optional.of(ep);
            }
            return Optional.of(instances.getLast());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Call distribution tracker (for test validation)
    // ─────────────────────────────────────────────────────────────────────────

    public static class DistributionTracker {
        private final Map<String, Integer> counts = new LinkedHashMap<>();

        public void record(ServiceEndpoint ep) {
            counts.merge(ep.instanceId(), 1, Integer::sum);
        }

        public Map<String, Integer> counts() { return Collections.unmodifiableMap(counts); }

        public int totalCalls() { return counts.values().stream().mapToInt(Integer::intValue).sum(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Configuration guide
    // ─────────────────────────────────────────────────────────────────────────

    public record LbStrategy(String name, String className, String useCase) {}

    public static List<LbStrategy> strategies() {
        return List.of(
            new LbStrategy("Round Robin",
                "RoundRobinLoadBalancer",
                "Default; distributes load evenly across healthy instances"),
            new LbStrategy("Random",
                "RandomLoadBalancer",
                "Simple random selection; good when instances are heterogeneous"),
            new LbStrategy("Weighted",
                "Custom ReactorServiceInstanceLoadBalancer",
                "Route more traffic to newer/larger instances")
        );
    }

    public static String defaultStrategy() { return "RoundRobinLoadBalancer"; }
}
