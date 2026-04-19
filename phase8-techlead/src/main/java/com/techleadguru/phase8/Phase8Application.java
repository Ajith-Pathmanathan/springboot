package com.techleadguru.phase8;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Phase 8 — Tech Lead Mastery (Days 162–180)
 *
 * Topics:
 *   Saga, CQRS, Event Sourcing, BFF, service mesh (Days 162-166)
 *   Testing: unit, slice (@WebMvcTest, @DataJpaTest), Testcontainers, contracts (Days 167-172)
 *   Caching: Redis, cache-aside, stampede fix (Days 173-176)
 *   ADR, code review, incident runbook, system design capstone (Days 177-180)
 *
 * Day 180 Capstone: Design and build a mini order management system using ALL concepts
 * from Days 1-179. Justify every decision.
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
public class Phase8Application {
    public static void main(String[] args) {
        SpringApplication.run(Phase8Application.class, args);
    }
}
