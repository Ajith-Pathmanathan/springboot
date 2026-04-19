package com.techleadguru.phase8.day180;

import java.util.*;

/**
 * Day 180 — CAPSTONE: Design Order Management System End-to-End ⭐
 *
 * A complete architecture for an Order Management System (OMS) applying
 * patterns from Phases 1-8:
 *   - Event Sourcing + CQRS (Day 163/164)
 *   - Saga choreography via Kafka (Day 162)
 *   - BFF per client (Day 165)
 *   - Redis cache with per-TTL (Day 174)
 *   - Spring Cloud Gateway (Phase 7)
 *   - Resilience4j circuit breaker (Phase 7)
 *   - Actuator + Prometheus metrics (Phase 5)
 *   - Spring Security (Phase 6)
 *   - ADRs for all major decisions (Day 177)
 */
public class Day180Capstone {

    // ─────────────────────────────────────────────────────────────────────────
    // System components
    // ─────────────────────────────────────────────────────────────────────────

    public enum ComponentType {
        API_GATEWAY,
        ORDER_SERVICE,
        PAYMENT_SERVICE,
        INVENTORY_SERVICE,
        NOTIFICATION_SERVICE,
        EVENT_BUS,
        CACHE_LAYER,
        READ_MODEL_STORE
    }

    public record ArchitectureComponent(
            ComponentType type,
            String        technology,
            List<String>  responsibilities,
            List<ComponentType> dependencies) {}

    // ─────────────────────────────────────────────────────────────────────────
    // System design
    // ─────────────────────────────────────────────────────────────────────────

    public record SystemDesign(
            String                     name,
            List<ArchitectureComponent> components,
            List<String>               adrs) {}

    public static SystemDesign orderManagementSystem() {
        List<ArchitectureComponent> components = List.of(

            new ArchitectureComponent(
                ComponentType.API_GATEWAY,
                "Spring Cloud Gateway + Spring Security (JWT)",
                List.of(
                    "Route requests to downstream services",
                    "JWT authentication and auth header propagation",
                    "Rate limiting per client (Redis-backed)",
                    "BFF routing: /web/* → Web BFF, /mobile/* → Mobile BFF",
                    "Circuit breaker fallback responses",
                    "Request/response logging and correlation ID injection"
                ),
                List.of(ComponentType.ORDER_SERVICE, ComponentType.PAYMENT_SERVICE,
                        ComponentType.INVENTORY_SERVICE)
            ),

            new ArchitectureComponent(
                ComponentType.ORDER_SERVICE,
                "Spring Boot 3 + Event Sourcing (EventStore) + CQRS",
                List.of(
                    "Accept order creation, update, and cancellation commands",
                    "Store domain events in event store (append-only)",
                    "Publish OrderCreated/OrderConfirmed/OrderCancelled events to Kafka",
                    "Maintain read model (OrderQueryStore) for query endpoints",
                    "Cache order summaries in Redis (TTL 5 min)",
                    "Expose Actuator /health, /metrics, /info"
                ),
                List.of(ComponentType.EVENT_BUS, ComponentType.CACHE_LAYER,
                        ComponentType.READ_MODEL_STORE)
            ),

            new ArchitectureComponent(
                ComponentType.PAYMENT_SERVICE,
                "Spring Boot 3 + Resilience4j",
                List.of(
                    "Subscribe to OrderCreated Kafka topic",
                    "Process payment; publish PaymentCompleted or PaymentFailed",
                    "Circuit breaker around payment gateway API",
                    "Idempotent payment processing (deduplication key)",
                    "Retry with exponential back-off on transient failures"
                ),
                List.of(ComponentType.EVENT_BUS)
            ),

            new ArchitectureComponent(
                ComponentType.INVENTORY_SERVICE,
                "Spring Boot 3 + Spring Data JPA",
                List.of(
                    "Subscribe to OrderCreated and OrderCancelled topics",
                    "Reserve and release inventory atomically",
                    "Publish InventoryReserved or InventoryFailed events",
                    "Cache product stock levels in Redis (TTL 30 s)",
                    "Expose stock-check REST endpoint for BFF aggregation"
                ),
                List.of(ComponentType.EVENT_BUS, ComponentType.CACHE_LAYER)
            ),

            new ArchitectureComponent(
                ComponentType.NOTIFICATION_SERVICE,
                "Spring Boot 3 + JavaMail / FCM",
                List.of(
                    "Subscribe to all order lifecycle events",
                    "Send email / push notifications per event type",
                    "Template-based messages (Thymeleaf)",
                    "Rate-limit notifications per user (spam prevention)",
                    "Dead-letter queue for failed notifications"
                ),
                List.of(ComponentType.EVENT_BUS)
            ),

            new ArchitectureComponent(
                ComponentType.EVENT_BUS,
                "Apache Kafka 3.x",
                List.of(
                    "Topic: order-events (OrderCreated, OrderConfirmed, OrderCancelled)",
                    "Topic: payment-events (PaymentCompleted, PaymentFailed)",
                    "Topic: inventory-events (InventoryReserved, InventoryFailed)",
                    "Consumer groups per service — independent offset management",
                    "Retention: 7 days for replay capability",
                    "Dead-letter topics for failed message processing"
                ),
                List.of()
            ),

            new ArchitectureComponent(
                ComponentType.CACHE_LAYER,
                "Redis 7 Cluster",
                List.of(
                    "Product catalogue: 30-minute TTL",
                    "Order summaries: 5-minute TTL",
                    "User sessions: 2-hour TTL",
                    "Rate limiter counters: 1-minute TTL",
                    "Distributed lock for cache stampede prevention"
                ),
                List.of()
            ),

            new ArchitectureComponent(
                ComponentType.READ_MODEL_STORE,
                "PostgreSQL 16 (write) + Elasticsearch (read projections)",
                List.of(
                    "PostgreSQL: write side for orders, payments, inventory",
                    "Elasticsearch: order search, full-text and filter queries",
                    "Projections updated asynchronously from Kafka events",
                    "Read model can be rebuilt by replaying event store"
                ),
                List.of()
            )
        );

        return new SystemDesign(
            "Order Management System",
            components,
            List.of("ADR-001: PostgreSQL write DB", "ADR-002: Redis distributed cache",
                    "ADR-003: Kafka event bus / Saga choreography")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data flow: place order end-to-end
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> dataFlowSteps() {
        return List.of(
            "1.  Client POST /api/orders → API Gateway (validates JWT)",
            "2.  Gateway routes to Order Service BFF endpoint",
            "3.  Order Service validates request, dispatches CreateOrderCommand",
            "4.  Command handler appends ORDER_CREATED event to EventStore",
            "5.  Event projection updates OrderQueryStore (CQRS read model)",
            "6.  Order Service publishes OrderCreated to Kafka order-events topic",
            "7.  Payment Service consumes OrderCreated → processes payment",
            "8a. Payment success → publishes PaymentCompleted",
            "8b. Payment failure → publishes PaymentFailed → Saga compensation: OrderCancelled",
            "9.  Inventory Service consumes OrderCreated → reserves stock",
            "9a. Stock reserved → publishes InventoryReserved",
            "9b. No stock → publishes InventoryFailed → compensation: OrderCancelled + PaymentRefund",
            "10. Order Service consumes PaymentCompleted + InventoryReserved → publishes OrderConfirmed",
            "11. Notification Service sends email/push for OrderConfirmed",
            "12. Client polls GET /api/orders/{id} → cached in Redis → instant response"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Design decisions
    // ─────────────────────────────────────────────────────────────────────────

    public record DesignDecision(String area, String decision, String rationale) {}

    public static List<DesignDecision> designDecisions() {
        return List.of(
            new DesignDecision("Consistency model",
                "Eventual consistency via Saga choreography",
                "Avoids distributed transactions; each service owns its data"),
            new DesignDecision("State storage",
                "Event Sourcing for orders; traditional CRUD for inventory/payment",
                "Order history is critical; inventory needs simple stock counts"),
            new DesignDecision("Read scalability",
                "CQRS with separate read model; cache with Redis",
                "Order reads vastly outnumber writes; enable independent scaling"),
            new DesignDecision("Security",
                "JWT at gateway; service-to-service via mTLS (Kubernetes Istio)",
                "Zero-trust across services; no lateral movement"),
            new DesignDecision("Observability",
                "Micrometer + Prometheus + Grafana; distributed tracing (Zipkin)",
                "Need end-to-end visibility across async event flows"),
            new DesignDecision("Resilience",
                "Circuit breakers on all downstream calls; dead-letter queues for Kafka",
                "Payment gateway is the most likely failure point; must not cascade")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scalability considerations
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> scalabilityConsiderations() {
        return List.of(
            "Order Service: stateless; scale horizontally behind Gateway",
            "Kafka: add partitions for order-events to increase consumer parallelism",
            "Redis: cluster mode for horizontal scaling of cache layer",
            "PostgreSQL: read replicas for reporting queries; PgBouncer for connection pooling",
            "Payment Service: single instance with idempotency key prevents duplicate charges on scale-out",
            "Inventory Service: row-level locking (SELECT FOR UPDATE) + optimistic locking (@Version)",
            "Notification Service: scale independently; backpressure via consumer group lag monitoring",
            "Gateway: stateless; scale behind load balancer; rate limiter state in Redis"
        );
    }
}
