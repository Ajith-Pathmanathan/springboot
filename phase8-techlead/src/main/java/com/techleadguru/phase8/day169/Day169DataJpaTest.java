package com.techleadguru.phase8.day169;

import java.util.*;

/**
 * Day 169 — @DataJpaTest: Repository with Real DB Queries
 *
 * @DataJpaTest loads only JPA-related beans (repositories, entities,
 * data source, Hibernate). Uses an embedded H2 DB by default.
 *
 * Key points:
 *  - Tests are transactional and roll back after each test
 *  - Use TestEntityManager to seed data
 *  - Test custom @Query methods and Specification predicates
 */
public class Day169DataJpaTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Test builder (builder pattern for creating test entities without Spring)
    // ─────────────────────────────────────────────────────────────────────────

    public static class TestOrderBuilder {
        private Long    id;
        private String  customerId = "customer-1";
        private String  status     = "PENDING";
        private double  total      = 100.0;
        private String  currency   = "USD";

        public TestOrderBuilder id(Long id)              { this.id         = id; return this; }
        public TestOrderBuilder customerId(String v)     { this.customerId = v;  return this; }
        public TestOrderBuilder status(String v)         { this.status     = v;  return this; }
        public TestOrderBuilder total(double v)          { this.total      = v;  return this; }
        public TestOrderBuilder currency(String v)       { this.currency   = v;  return this; }

        public Map<String, Object> build() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (id != null) m.put("id", id);
            m.put("customerId", customerId);
            m.put("status",     status);
            m.put("total",      total);
            m.put("currency",   currency);
            return m;
        }

        public static TestOrderBuilder anOrder() { return new TestOrderBuilder(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Annotation guide
    // ─────────────────────────────────────────────────────────────────────────

    public record AnnotationInfo(String annotation, String description) {}

    public static List<AnnotationInfo> dataJpaTestAnnotations() {
        return List.of(
            new AnnotationInfo("@DataJpaTest",
                "Loads only JPA slice: entities, repos, DataSource, JPA config. No @Service, @Controller."),
            new AnnotationInfo("@AutoConfigureTestDatabase(replace=NONE)",
                "Do NOT replace DataSource with embedded H2 — use configured DB (e.g. Testcontainers)."),
            new AnnotationInfo("@Transactional",
                "Default on @DataJpaTest — each test rolls back. Avoid @Commit unless intentional."),
            new AnnotationInfo("@Sql",
                "Run SQL scripts to seed test data: @Sql(\"/seed.sql\") on class or method."),
            new AnnotationInfo("@Import",
                "Import extra beans needed (e.g. AuditorAware, custom converters)."),
            new AnnotationInfo("TestEntityManager",
                "Injected helper to persist, flush, and clear entities without going through the repo.")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query test patterns
    // ─────────────────────────────────────────────────────────────────────────

    public record QueryTestPattern(String queryType, String example, String assertion) {}

    public static List<QueryTestPattern> queryTestPatterns() {
        return List.of(
            new QueryTestPattern(
                "Derived query (findBy)",
                "repo.findByCustomerId(\"c1\")",
                "assertThat(result).hasSize(2).allMatch(o -> o.getCustomerId().equals(\"c1\"))"),
            new QueryTestPattern(
                "Custom JPQL @Query",
                "repo.findActiveOrdersAfter(LocalDate.now().minusDays(7))",
                "assertThat(result).isNotEmpty().allMatch(o -> o.getCreatedAt().isAfter(cutoff))"),
            new QueryTestPattern(
                "Native @Query",
                "repo.countByStatus(\"PENDING\")",
                "assertEquals(3, count)"),
            new QueryTestPattern(
                "Specification / Criteria",
                "repo.findAll(OrderSpec.byCustomer(\"c1\").and(OrderSpec.hasStatus(\"PENDING\")))",
                "assertThat(result).allMatch(o -> o.getStatus().equals(\"PENDING\"))"),
            new QueryTestPattern(
                "Pagination",
                "repo.findByStatus(\"PENDING\", PageRequest.of(0, 5, Sort.by(\"createdAt\")))",
                "assertThat(page.getContent()).hasSizeLessThanOrEqualTo(5)")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transaction behaviour guide
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> transactionBehaviourGuide() {
        return List.of(
            "Each @DataJpaTest test is wrapped in a transaction that rolls back after the test",
            "Use em.persistAndFlush(entity) to force an INSERT before your query under test",
            "em.clear() after flush to evict L1 cache and force DB read",
            "For tests that verify @Transactional(propagation=REQUIRES_NEW) use @Commit on a dedicated test",
            "Lazy loading issues surface in @DataJpaTest — use JOIN FETCH or @Transactional on test method",
            "@DataJpaTest does NOT start the full Spring context — add missing beans with @Import"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Repository test guide
    // ─────────────────────────────────────────────────────────────────────────

    public record RepositoryTestGuide(String step, String action) {}

    public static List<RepositoryTestGuide> repositoryTestGuide() {
        return List.of(
            new RepositoryTestGuide("1. Arrange", "Use TestEntityManager.persist() to create entities"),
            new RepositoryTestGuide("2. Flush",   "Call em.flush() to synchronise to DB"),
            new RepositoryTestGuide("3. Clear",   "Call em.clear() to evict first-level cache"),
            new RepositoryTestGuide("4. Act",     "Call repository method under test"),
            new RepositoryTestGuide("5. Assert",  "Use assertThat / assertEquals on result"),
            new RepositoryTestGuide("6. Rollback","@DataJpaTest rolls back automatically after test")
        );
    }
}
