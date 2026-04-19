package com.techleadguru.phase7.day136;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class Day136LoadBalancerTest {

    private List<Day136LoadBalancer.ServiceEndpoint> endpoints() {
        return List.of(
                new Day136LoadBalancer.ServiceEndpoint("ep-1", "host1", 8080, 1),
                new Day136LoadBalancer.ServiceEndpoint("ep-2", "host2", 8081, 1),
                new Day136LoadBalancer.ServiceEndpoint("ep-3", "host3", 8082, 1)
        );
    }

    @Test
    void testRoundRobinDistribution() {
        Day136LoadBalancer.RoundRobinBalancer balancer =
                new Day136LoadBalancer.RoundRobinBalancer(endpoints());
        assertEquals(3, balancer.instanceCount());

        String first  = balancer.choose().map(Day136LoadBalancer.ServiceEndpoint::instanceId).orElse(null);
        String second = balancer.choose().map(Day136LoadBalancer.ServiceEndpoint::instanceId).orElse(null);
        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
    }

    @Test
    void testRoundRobinEmpty() {
        Day136LoadBalancer.RoundRobinBalancer balancer =
                new Day136LoadBalancer.RoundRobinBalancer(List.of());
        Optional<Day136LoadBalancer.ServiceEndpoint> result = balancer.choose();
        assertTrue(result.isEmpty());
    }

    @Test
    void testWeightedBalancer() {
        List<Day136LoadBalancer.ServiceEndpoint> weighted = List.of(
                new Day136LoadBalancer.ServiceEndpoint("ep-1", "host1", 8080, 10),
                new Day136LoadBalancer.ServiceEndpoint("ep-2", "host2", 8081, 1)
        );
        Day136LoadBalancer.WeightedBalancer balancer =
                new Day136LoadBalancer.WeightedBalancer(weighted, new Random(42));

        int ep1Count = 0;
        int total    = 1000;
        for (int i = 0; i < total; i++) {
            Optional<Day136LoadBalancer.ServiceEndpoint> chosen = balancer.choose();
            assertTrue(chosen.isPresent());
            if ("ep-1".equals(chosen.get().instanceId())) ep1Count++;
        }
        // ep-1 has 10/11 weight, so should be chosen roughly 90% of the time
        assertTrue(ep1Count > 800, "Expected ep-1 to be chosen > 80% of the time, was: " + ep1Count);
    }

    @Test
    void testDistributionTracker() {
        Day136LoadBalancer.DistributionTracker tracker = new Day136LoadBalancer.DistributionTracker();
        Day136LoadBalancer.ServiceEndpoint ep1 = new Day136LoadBalancer.ServiceEndpoint("ep-1", "h1", 80, 1);
        Day136LoadBalancer.ServiceEndpoint ep2 = new Day136LoadBalancer.ServiceEndpoint("ep-2", "h2", 80, 1);

        tracker.record(ep1);
        tracker.record(ep1);
        tracker.record(ep2);

        assertEquals(2, tracker.counts().get("ep-1"));
        assertEquals(1, tracker.counts().get("ep-2"));
        assertEquals(3, tracker.totalCalls());
    }

    @Test
    void testStrategies() {
        List<Day136LoadBalancer.LbStrategy> strategies = Day136LoadBalancer.strategies();
        assertEquals(3, strategies.size());
    }

    @Test
    void testDefaultStrategy() {
        assertEquals("RoundRobinLoadBalancer", Day136LoadBalancer.defaultStrategy());
    }
}
