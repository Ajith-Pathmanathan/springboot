# Spring Boot → Technical Lead: Daily Progress Tracker

> 180 Days | 1 Hour/Day | Started: \***\*\_\_\_\*\***
> Rule: Do NOT check a box until you have (1) understood the concept AND (2) running code that proves it.

---

## PHASE 1 — Spring Core Internals (Days 1–21)

### Week 1 — IoC Container & Bean Lifecycle

- [ ] Day 1 — `BeanFactory` vs `ApplicationContext` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 2 — Bean registration: `@Component` vs `@Bean` vs XML | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 3 — Bean lifecycle: init → populate → PostConstruct → destroy| Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 4 — `BeanPostProcessor`: intercept every bean at init time | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 5 — `BeanFactoryPostProcessor`: modify bean definitions | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 6 — `@Scope`: singleton vs prototype — the injection bug | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 7 — `ApplicationContext` events + custom domain events | Date: \***\*\_\_\*\*** | Notes:

### Week 2 — Dependency Injection Internals

- [ ] Day 8 — Constructor vs setter vs field injection | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 9 — `@Autowired` resolution: type → name → `@Qualifier` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 10 — `@Primary` vs `@Qualifier` vs `@Resource` vs `@Inject` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 11 — `@Lazy` and `ObjectProvider` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 12 — Optional dependencies: `Optional<T>` injection | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 13 — Circular dependency: why Spring Boot 2.6+ breaks it | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 14 — `@Configuration` CGLIB proxy: the multiple pool bug | Date: \***\*\_\_\*\*** | Notes:

### Week 3 — AOP: The Engine Behind @Transactional, @Async, @Cacheable

- [ ] Day 15 — AOP basics: join points, pointcuts, advice, aspect | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 16 — JDK proxy vs CGLIB: `final` class breaks Spring | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 17 — `@Around` advice — the silent data loss bug | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 18 — Pointcut expressions: `execution`, `within`, `@annotation`| Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 19 — THE SELF-INVOCATION TRAP ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 20 — `exposeProxy=true` and AspectJ vs Spring AOP | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 21 — Auto-configuration: `@Conditional` + custom starter | Date: \***\*\_\_\*\*** | Notes:

---

## PHASE 2 — Data Layer Deep Dive (Days 22–42)

### Week 4 — @Transactional Internals

- [ ] Day 22 — How `@Transactional` really works: AOP + `TransactionSynchronizationManager` | Date: **\_\_** | Notes:
- [ ] Day 23 — Propagation: `REQUIRED` vs `REQUIRES_NEW` | Date: **\_\_** | Notes:
- [ ] Day 24 — Propagation: `NESTED` and savepoints | Date: **\_\_** | Notes:
- [ ] Day 25 — `MANDATORY`, `NEVER`, `NOT_SUPPORTED`, `SUPPORTS` | Date: **\_\_** | Notes:
- [ ] Day 26 — Isolation levels: dirty reads, phantom reads, lost updates | Date: **\_\_** | Notes:
- [ ] Day 27 — Rollback rules: checked exceptions don't rollback by default | Date: **\_\_** | Notes:
- [ ] Day 28 — `@Transactional` on private method — silent no-op | Date: **\_\_** | Notes:

### Week 5 — JPA & Hibernate Internals

- [ ] Day 29 — `EntityManager` & Persistence Context (1st level cache) | Date: **\_\_** | Notes:
- [ ] Day 30 — Entity states: Transient → Managed → Detached → Removed | Date: **\_\_** | Notes:
- [ ] Day 31 — N+1 Problem: live reproduction ⚠️ | Date: **\_\_** | Notes:
- [ ] Day 32 — Fix N+1: `JOIN FETCH` | Date: **\_\_** | Notes:
- [ ] Day 33 — Fix N+1: `@EntityGraph` | Date: **\_\_** | Notes:
- [ ] Day 34 — Fix N+1: `@BatchSize` and `default_batch_fetch_size` | Date: **\_\_** | Notes:
- [ ] Day 35 — JPQL vs Criteria API vs native query | Date: **\_\_** | Notes:

### Week 6 — Connection Pooling & Production DB Issues

- [ ] Day 36 — HikariCP internals: pool lifecycle | Date: **\_\_** | Notes:
- [ ] Day 37 — Pool sizing formula + Actuator metrics | Date: **\_\_** | Notes:
- [ ] Day 38 — Connection leak: `leakDetectionThreshold` | Date: **\_\_** | Notes:
- [ ] Day 39 — Read replica routing: `AbstractRoutingDataSource` | Date: **\_\_** | Notes:
- [ ] Day 40 — Flyway DB migrations | Date: **\_\_** | Notes:
- [ ] Day 41 — Slow query detection: Hibernate stats, P6Spy | Date: **\_\_** | Notes:
- [ ] Day 42 — Optimistic locking: `@Version` prevents lost updates | Date: **\_\_** | Notes:

---

## PHASE 3 — Web Layer Internals (Days 43–56)

- [ ] Day 43 — `DispatcherServlet` full request lifecycle | Date: **\_\_** | Notes:
- [ ] Day 44 — Custom `HandlerMethodArgumentResolver` | Date: **\_\_** | Notes:
- [ ] Day 45 — `HttpMessageConverter` internals | Date: **\_\_** | Notes:
- [ ] Day 46 — Content negotiation: JSON vs XML via `Accept` header | Date: **\_\_** | Notes:
- [ ] Day 47 — Filters vs Interceptors vs AOP — when to use which | Date: **\_\_** | Notes:
- [ ] Day 48 — `OncePerRequestFilter` + MDC correlation ID ⚠️ | Date: **\_\_** | Notes:
- [ ] Day 49 — `@ControllerAdvice` + RFC 7807 `ProblemDetail` | Date: **\_\_** | Notes:
- [ ] Day 50 — Bean Validation: custom `ConstraintValidator` | Date: **\_\_** | Notes:
- [ ] Day 51 — REST pagination: `Pageable` + `Page<T>` | Date: **\_\_** | Notes:
- [ ] Day 52 — API versioning: URI vs header vs content-type | Date: **\_\_** | Notes:
- [ ] Day 53 — Idempotency keys for POST requests | Date: **\_\_** | Notes:
- [ ] Day 54 — Rate limiting with Bucket4j | Date: **\_\_** | Notes:
- [ ] Day 55 — CORS: fix the security misconfiguration | Date: **\_\_** | Notes:
- [ ] Day 56 — WebFlux vs MVC under load test | Date: \***\*\_\_\*\*** | Notes:

---

## PHASE 4 — Async, Scheduling & Concurrency (Days 57–77)

### Week 9 — @Async Deep Dive

- [ ] Day 57 — `@Async` internals: AOP proxy + TaskExecutor | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 58 — `ThreadPoolTaskExecutor` tuning | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 59 — `@Async` + `CompletableFuture<T>`: parallel calls | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 60 — `@Async` exception black hole ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 61 — `@Async` + `SecurityContext` trap ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 62 — `@Async` + `@Transactional` — separate TX per call | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 63 — Virtual threads (Java 21 + Spring Boot 3.2) | Date: \***\*\_\_\*\*** | Notes:

### Week 10 — @Scheduled & Quartz

- [ ] Day 64 — `fixedRate` vs `fixedDelay` vs `cron` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 65 — Overlapping scheduled tasks | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 66 — Clustered scheduling: duplicate execution problem ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 67 — ShedLock: distributed lock for `@Scheduled` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 68 — Quartz: JDBC `JobStore`, survives restart | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 69 — Dynamic scheduling at runtime | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 70 — Cron expression mastery | Date: \***\*\_\_\*\*** | Notes:

### Week 11 — Concurrency: Production Thread Problems

- [ ] Day 71 — Thread dump: `jstack`, find `BLOCKED` threads | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 72 — Deadlock: reproduce, diagnose, fix ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 73 — Livelock & starvation | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 74 — `ReentrantLock` vs `synchronized`: `tryLock` timeout | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 75 — `ConcurrentHashMap` vs `HashMap` under load ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 76 — `BlockingQueue` producer-consumer | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 77 — `CompletableFuture` error pipeline | Date: \***\*\_\_\*\*** | Notes:

---

## PHASE 5 — Production Problem Solving (Days 78–105)

### Week 12 — Memory Leaks

- [ ] Day 78 — JVM memory model: trigger `OutOfMemoryError` per region | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 79 — Heap dump: `-XX:+HeapDumpOnOutOfMemoryError` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 80 — Heap dump analysis with Eclipse MAT: dominator tree | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 81 — Static field memory leak ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 82 — Listener/callback registration leak ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 83 — `ThreadLocal` leak in thread pools ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 84 — Unbounded in-memory cache OOM → Caffeine fix | Date: \***\*\_\_\*\*** | Notes:

### Week 13 — Stuck Threads & Application Freeze

- [ ] Day 85 — Slow DB query exhausts connection pool ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 86 — HTTP client without timeout — silent killer ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 87 — DB-level deadlock: two TX lock rows in opposite order | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 88 — `OpenSessionInView` anti-pattern ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 89 — Thread pool saturation: queue buildup + rejection | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 90 — Graceful shutdown: in-flight TX, async tasks | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 91 — Production diagnosis: `jstack` + `top -H` + `jcmd` | Date: \***\*\_\_\*\*** | Notes:

### Week 14 — GC Tuning & JVM Performance

- [ ] Day 92 — GC basics: Young Gen, Old Gen, Metaspace, Minor vs Full GC | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 93 — G1GC vs ZGC vs Shenandoah | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 94 — GC log analysis: identify GC pressure | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 95 — Off-heap: `DirectByteBuffer`, native memory tracking | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 96 — CPU profiling with `async-profiler` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 97 — `String` memory: `StringBuilder` vs `+` in loops (JMH)| Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 98 — Production JVM tuning checklist | Date: \***\*\_\_\*\*** | Notes:

### Week 15 — Advanced Production Topics

- [ ] Day 99 — Actuator deep dive: custom endpoints, securing them | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 100 — Health indicators: liveness vs readiness in K8s | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 101 — Distributed lock patterns: Redis `SETNX`, Redisson | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 102 — Feature flags in production: dynamic config, gradual rollout | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 103 — Bulkhead pattern in monolith: isolate slow calls | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 104 — Connection pool for external APIs: `RestClient` pooling | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 105 — Production checklist: all learnings from Phase 5 | Date: \***\*\_\_\*\*** | Notes:

---

## PHASE 6 — Spring Security & OAuth 2.0 (Days 106–133)

### Week 15–16 — Spring Security Internals

- [ ] Day 106 — `SecurityFilterChain`: all filters, order, inspect | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 107 — Login request flow: Filter → `AuthenticationManager` → `SecurityContext` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 108 — Custom `AuthenticationProvider` (OTP-based) | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 109 — `SecurityContextHolder` in `@Async` ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 110 — BCrypt cost factor: brute force vs latency trade-off | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 111 — `@PreAuthorize`: expressions + `#param` binding | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 112 — CSRF: when to enable vs disable | Date: \***\*\_\_\*\*** | Notes:

### Week 17 — JWT Deep Dive

- [ ] Day 113 — JWT structure: decode manually, no library | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 114 — RS256 vs HS256: sign with RSA key pair | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 115 — Access token + refresh token lifecycle | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 116 — JWT security pitfalls: `alg: none` attack ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 117 — Custom claims: `tenantId`, `roles` in `@PreAuthorize` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 118 — Stateless vs stateful: horizontal scaling impact | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 119 — Token revocation: Redis blacklist with TTL ⚠️ | Date: \***\*\_\_\*\*** | Notes:

### Week 18 — OAuth 2.0 Flows

- [ ] Day 120 — OAuth 2.0 roles: draw flow diagram from memory | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 121 — Authorization Code Flow end-to-end | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 122 — PKCE: `code_verifier` + `code_challenge` (SHA-256) | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 123 — Client Credentials: service-to-service auth | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 124 — Refresh token rotation | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 125 — OpenID Connect: ID token, `userinfo` endpoint | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 126 — Resource Server: validate JWT from Keycloak | Date: \***\*\_\_\*\*** | Notes:

### Week 19 — Spring Authorization Server

- [ ] Day 127 — Spring Authorization Server: issue access tokens | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 128 — `OAuth2TokenCustomizer`: add roles to token | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 129 — PKCE + SAS: full Authorization Code flow | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 130 — Opaque tokens + introspection endpoint | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 131 — Multi-tenant OAuth2: `tenantId` in token + per-tenant JWKS | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 132 — Token exchange: impersonation + delegation | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 133 — Security architecture design: web + mobile + service-to-service | Date: \***\*\_\_\*\*** | Notes:

---

## PHASE 7 — Spring Cloud (Days 134–161)

### Week 19–20 — Discovery & Gateway

- [ ] Day 134 — Eureka: register 2 services, verify heartbeat | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 135 — Eureka internals: eviction, self-preservation mode | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 136 — Spring Cloud LoadBalancer: round-robin distribution | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 137 — Spring Cloud Gateway: routing + URI rewrite | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 138 — Gateway filters: rate limiter with Redis | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 139 — JWT validation at Gateway layer | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 140 — Gateway global exception handling (WebFlux) | Date: \***\*\_\_\*\*** | Notes:

### Week 21 — Config & Resilience

- [ ] Day 141 — Config Server: Git backend, client fetch on startup | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 142 — `@RefreshScope`: update config without restart | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 143 — Config encryption: encrypted DB password in Git | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 144 — Resilience4j: Circuit Breaker state machine ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 145 — Resilience4j: Retry + exponential backoff + jitter | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 146 — Resilience4j: Bulkhead (concurrent call limit) | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 147 — Resilience4j: RateLimiter (calls per time window) | Date: \***\*\_\_\*\*** | Notes:

### Week 22 — Messaging with Kafka

- [ ] Day 148 — Kafka: producer + consumer, manual offset commit | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 149 — Spring Cloud Stream: functional style | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 150 — Consumer groups: scale consumers | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 151 — Dead Letter Queue: 3 retries then DLQ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 152 — Idempotent consumer: skip duplicates ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 153 — Transactional Outbox pattern ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 154 — Kafka Streams: windowed aggregation | Date: \***\*\_\_\*\*** | Notes:

### Week 23 — Distributed Tracing & Observability

- [ ] Day 155 — Trace ID + Span ID propagation across services | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 156 — Micrometer + Zipkin: visualise request traces | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 157 — Custom business metrics: `Counter`, `Timer`, `Gauge` | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 158 — Prometheus + Grafana: RED metrics dashboard | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 159 — Structured JSON logging + MDC | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 160 — Alerting: Prometheus alert rules | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 161 — Custom `HealthIndicator` + K8s readiness probe | Date: \***\*\_\_\*\*** | Notes:

---

## PHASE 8 — Tech Lead Mastery (Days 162–180)

### Week 24 — Microservices Patterns

- [ ] Day 162 — Saga pattern: choreography vs orchestration ⚠️ | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 163 — CQRS: separate command/query models | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 164 — Event Sourcing: store events, replay to get state | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 165 — BFF: Backend for Frontend pattern | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 166 — Service mesh vs Spring Cloud | Date: \***\*\_\_\*\*** | Notes:

### Week 25 — Testing Strategy

- [ ] Day 167 — Unit testing: mock vs spy, argument captors | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 168 — `@WebMvcTest`: controller layer isolation | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 169 — `@DataJpaTest`: repository with real DB queries | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 170 — Testcontainers: real PostgreSQL + Kafka in CI | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 171 — Contract testing: Spring Cloud Contract | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 172 — Load testing with k6 | Date: \***\*\_\_\*\*** | Notes:

### Week 26 — Caching Deep Dive

- [ ] Day 173 — `@Cacheable` / `@CacheEvict` / `@CachePut` internals | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 174 — Redis cache: per-cache TTL | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 175 — Cache-aside vs write-through vs write-behind | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 176 — Cache stampede: thundering herd ⚠️ | Date: \***\*\_\_\*\*** | Notes:

### Week 27 — Architecture & Tech Lead Skills

- [ ] Day 177 — ADR (Architecture Decision Records) | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 178 — Code review as tech lead: what to look for | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 179 — Incident runbook: OOM, pool exhaustion, deadlock | Date: \***\*\_\_\*\*** | Notes:
- [ ] Day 180 — CAPSTONE: design order management system end-to-end ⭐| Date: \***\*\_\_\*\*** | Notes:

---

## Legend

- ⚠️ = High-value concept (often the source of production incidents)
- ⭐ = Milestone/capstone task
- Notes column: write what surprised you, what took extra time, or a command you want to remember
