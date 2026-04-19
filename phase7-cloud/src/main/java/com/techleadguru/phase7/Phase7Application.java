package com.techleadguru.phase7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Phase 7 — Spring Cloud (Days 134–161)
 *
 * Topics:
 *   Eureka, LoadBalancer, Spring Cloud Gateway (Days 134-140)
 *   Config Server, @RefreshScope, Resilience4j (Days 141-147)
 *   Kafka, Spring Cloud Stream, Outbox pattern (Days 148-154)
 *   Distributed tracing, Micrometer, Prometheus, Grafana (Days 155-161)
 *
 * NOTE: Phase 7 requires a running Kafka broker and Eureka server.
 * Use the docker-compose.yml in this module's root.
 */
@SpringBootApplication
public class Phase7Application {
    public static void main(String[] args) {
        SpringApplication.run(Phase7Application.class, args);
    }
}
