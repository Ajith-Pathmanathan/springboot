package com.techleadguru.phase8.day177;

import java.time.LocalDate;
import java.util.*;

/**
 * Day 177 — Architecture Decision Records (ADR)
 *
 * An ADR documents a significant architectural decision: the context,
 * the decision, the consequences, and the alternatives considered.
 * ADRs live in version control alongside the code they describe.
 *
 * Typical location: docs/adr/  or  src/main/resources/architecture/
 */
public class Day177ADR {

    // ─────────────────────────────────────────────────────────────────────────
    // ADR status lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    public enum AdrStatus {
        PROPOSED,    // Under discussion
        ACCEPTED,    // Decision made
        DEPRECATED,  // No longer relevant
        SUPERSEDED   // Replaced by a newer ADR
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADR record
    // ─────────────────────────────────────────────────────────────────────────

    public record Adr(
            String        id,
            String        title,
            AdrStatus     status,
            String        context,
            String        decision,
            List<String>  consequences,
            List<String>  alternatives,
            LocalDate     date,
            String        supersededBy) {

        /** Convenience constructor without supersededBy. */
        public Adr(String id, String title, AdrStatus status, String context,
                   String decision, List<String> consequences, List<String> alternatives,
                   LocalDate date) {
            this(id, title, status, context, decision, consequences, alternatives, date, null);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADR registry
    // ─────────────────────────────────────────────────────────────────────────

    public static class AdrRegistry {

        private final Map<String, Adr> adrs = new LinkedHashMap<>();

        public AdrRegistry register(Adr adr) {
            adrs.put(adr.id(), adr);
            return this;
        }

        public Optional<Adr> findById(String id) {
            return Optional.ofNullable(adrs.get(id));
        }

        public List<Adr> findByStatus(AdrStatus status) {
            return adrs.values().stream()
                    .filter(a -> a.status() == status)
                    .toList();
        }

        public List<Adr> listAll() { return List.copyOf(adrs.values()); }
        public int size()          { return adrs.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADR template fields
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> adrTemplate() {
        return List.of(
            "# ADR-{id}: {title}",
            "Date: {YYYY-MM-DD}",
            "Status: Proposed | Accepted | Deprecated | Superseded by ADR-{id}",
            "",
            "## Context",
            "{Forces at play: why was this decision needed?}",
            "",
            "## Decision",
            "{The decision and its rationale}",
            "",
            "## Consequences",
            "{Positive and negative consequences of this decision}",
            "",
            "## Alternatives Considered",
            "{Other options evaluated and reasons for rejection}"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sample ADRs for an order management system
    // ─────────────────────────────────────────────────────────────────────────

    public static List<Adr> sampleAdrs() {
        return List.of(
            new Adr(
                "ADR-001",
                "Use PostgreSQL as the primary write database",
                AdrStatus.ACCEPTED,
                "We need a reliable relational store with ACID transactions for orders and payments.",
                "Use PostgreSQL 16 for the write side of CQRS. All command models persist to PG.",
                List.of(
                    "Strong ACID guarantees — critical for financial data",
                    "Mature Spring Data JPA support",
                    "Requires PostgreSQL operational expertise",
                    "Horizontal write scaling is limited"
                ),
                List.of("MySQL — similar capability but team has PG expertise",
                        "MongoDB — flexible schema but weaker transactions for financial data"),
                LocalDate.of(2024, 1, 15)
            ),

            new Adr(
                "ADR-002",
                "Use Redis for distributed caching with per-cache TTL",
                AdrStatus.ACCEPTED,
                "High read traffic on product catalogue and user sessions requires a distributed cache.",
                "Use Redis 7 with RedisCacheManager. Configure per-cache TTL (products=30m, sessions=2h).",
                List.of(
                    "Shared cache across all pods — consistent reads",
                    "TTL prevents unbounded memory growth",
                    "Redis becomes a critical dependency — plan for failover",
                    "Serialisation format must be managed across deployments"
                ),
                List.of("Caffeine (in-process) — no distributed invalidation",
                        "Memcached — no data structures beyond simple K/V"),
                LocalDate.of(2024, 2, 1)
            ),

            new Adr(
                "ADR-003",
                "Use Kafka for async order event bus (Saga choreography)",
                AdrStatus.ACCEPTED,
                "Order placement triggers downstream actions (payment, inventory, notifications). " +
                "Synchronous calls create tight coupling and cascade failures.",
                "Publish domain events to Kafka topics. Each service subscribes and reacts independently.",
                List.of(
                    "Decoupled services — each service evolves independently",
                    "Kafka provides replay for reprocessing and audit",
                    "Operational complexity: Kafka cluster to manage",
                    "Eventual consistency — UI must handle PENDING states"
                ),
                List.of("RabbitMQ — simpler but no replay / compaction",
                        "Synchronous REST callbacks — tight coupling, cascading failures"),
                LocalDate.of(2024, 3, 10)
            )
        );
    }
}
