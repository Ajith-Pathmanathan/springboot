package com.techleadguru.phase2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Phase 2 — Data Layer Deep Dive (Days 22–42)
 *
 * Topics covered:
 *   @Transactional propagation, isolation, rollback rules (Days 22-28)
 *   JPA/Hibernate: persistence context, N+1 problem, @EntityGraph (Days 29-35)
 *   HikariCP: pool tuning, leak detection, read replicas, Flyway, @Version (Days 36-42)
 *
 * Start here after completing Phase 1 (Days 1-21).
 * Each day has its own package: day22, day23, ... day42
 */
@SpringBootApplication
public class Phase2Application {

    public static void main(String[] args) {
        SpringApplication.run(Phase2Application.class, args);
    }
}
