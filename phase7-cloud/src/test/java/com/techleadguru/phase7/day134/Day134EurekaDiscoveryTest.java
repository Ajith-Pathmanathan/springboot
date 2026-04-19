package com.techleadguru.phase7.day134;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Day134EurekaDiscoveryTest {

    @Test
    void testInstanceStatusValues() {
        Day134EurekaDiscovery.InstanceStatus[] statuses = Day134EurekaDiscovery.InstanceStatus.values();
        assertEquals(5, statuses.length);
    }

    @Test
    void testEurekaRegistrySimulatorRegisterAndRetrieve() {
        Day134EurekaDiscovery.EurekaRegistrySimulator registry =
                new Day134EurekaDiscovery.EurekaRegistrySimulator();

        Day134EurekaDiscovery.ServiceInstance inst =
                new Day134EurekaDiscovery.ServiceInstance(
                        "ORDER-SERVICE", "order-1", "host1", 8080,
                        Day134EurekaDiscovery.InstanceStatus.UP);

        registry.register(inst);
        List<Day134EurekaDiscovery.ServiceInstance> up =
                registry.getUpInstances("ORDER-SERVICE");
        assertEquals(1, up.size());
        assertEquals("order-1", up.get(0).instanceId());
    }

    @Test
    void testDeregister() {
        Day134EurekaDiscovery.EurekaRegistrySimulator registry =
                new Day134EurekaDiscovery.EurekaRegistrySimulator();

        Day134EurekaDiscovery.ServiceInstance inst =
                new Day134EurekaDiscovery.ServiceInstance(
                        "ORDER-SERVICE", "order-1", "host1", 8080,
                        Day134EurekaDiscovery.InstanceStatus.UP);
        registry.register(inst);
        registry.deregister("ORDER-SERVICE", "order-1");
        assertEquals(0, registry.getUpInstances("ORDER-SERVICE").size());
    }

    @Test
    void testTotalInstanceCountAndRegisteredApps() {
        Day134EurekaDiscovery.EurekaRegistrySimulator registry =
                new Day134EurekaDiscovery.EurekaRegistrySimulator();

        registry.register(new Day134EurekaDiscovery.ServiceInstance(
                "SVC-A", "a-1", "h1", 8080, Day134EurekaDiscovery.InstanceStatus.UP));
        registry.register(new Day134EurekaDiscovery.ServiceInstance(
                "SVC-A", "a-2", "h2", 8080, Day134EurekaDiscovery.InstanceStatus.UP));
        registry.register(new Day134EurekaDiscovery.ServiceInstance(
                "SVC-B", "b-1", "h3", 9090, Day134EurekaDiscovery.InstanceStatus.UP));

        assertEquals(3, registry.totalInstanceCount());
        assertEquals(2, registry.registeredApps().size());
    }

    @Test
    void testDefaultHeartbeatConfig() {
        Day134EurekaDiscovery.HeartbeatConfig cfg = Day134EurekaDiscovery.defaultHeartbeatConfig();
        assertEquals(30, cfg.renewalIntervalSeconds());
        assertEquals(90, cfg.evictionThresholdSeconds());
    }

    @Test
    void testKeyConcepts() {
        List<Day134EurekaDiscovery.EurekaConcept> concepts = Day134EurekaDiscovery.keyConcepts();
        assertEquals(6, concepts.size());
        assertTrue(concepts.stream().anyMatch(c -> c.concept().equals("Eureka Server")));
    }

    @Test
    void testClientProperties() {
        Map<String, String> props = Day134EurekaDiscovery.clientProperties(
                "order-service", "http://eureka:8761/eureka");
        assertFalse(props.isEmpty());
        assertTrue(props.containsKey("spring.application.name"));
        assertEquals("order-service", props.get("spring.application.name"));
    }

    @Test
    void testClearRegistry() {
        Day134EurekaDiscovery.EurekaRegistrySimulator registry =
                new Day134EurekaDiscovery.EurekaRegistrySimulator();
        registry.register(new Day134EurekaDiscovery.ServiceInstance(
                "SVC", "s-1", "h1", 8080, Day134EurekaDiscovery.InstanceStatus.UP));
        registry.clear();
        assertEquals(0, registry.totalInstanceCount());
    }
}
