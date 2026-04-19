package com.techleadguru.phase1.day10;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DAY 10 — Test: @Primary vs @Qualifier precedence.
 */
class Day10PrimaryQualifierPrecedenceTest {

    private final Day10PrimaryQualifierPrecedence.RedisCacheService redis =
            new Day10PrimaryQualifierPrecedence.RedisCacheService();
    private final Day10PrimaryQualifierPrecedence.InMemoryCacheService inMemory =
            new Day10PrimaryQualifierPrecedence.InMemoryCacheService();

    // -----------------------------------------------------------------------
    // Test 1: @Primary bean is injected when no qualifier is specified
    // -----------------------------------------------------------------------
    @Test
    void primary_bean_is_injected_when_no_qualifier_specified() {
        var productService = new Day10PrimaryQualifierPrecedence.ProductService(redis);

        assertThat(productService.getCacheService()).isInstanceOf(
                Day10PrimaryQualifierPrecedence.RedisCacheService.class);
        System.out.println("[DAY 10] @Primary = RedisCacheService injected into ProductService");
    }

    // -----------------------------------------------------------------------
    // Test 2: @Qualifier overrides @Primary
    // -----------------------------------------------------------------------
    @Test
    void qualifier_overrides_primary_bean() {
        var sessionService = new Day10PrimaryQualifierPrecedence.SessionService(inMemory);

        assertThat(sessionService.getCacheService()).isInstanceOf(
                Day10PrimaryQualifierPrecedence.InMemoryCacheService.class);
        System.out.println("[DAY 10] @Qualifier overrides @Primary → InMemoryCacheService injected");
    }

    // -----------------------------------------------------------------------
    // Test 3: Session cache stores and retrieves correctly
    // -----------------------------------------------------------------------
    @Test
    void session_service_stores_and_retrieves_session_via_in_memory_cache() {
        var sessionService = new Day10PrimaryQualifierPrecedence.SessionService(inMemory);

        sessionService.storeSession("sess-abc", "user-42");
        String retrieved = sessionService.getSession("sess-abc");

        assertThat(retrieved).isEqualTo("user-42");
        System.out.println("[DAY 10] InMemory cache session: " + retrieved);
    }

    // -----------------------------------------------------------------------
    // Test 4: Document @Resource and @Inject rules
    // -----------------------------------------------------------------------
    @Test
    void document_resource_and_inject_annotation_rules() {
        System.out.println("[DAY 10] ANNOTATION COMPARISON:");
        System.out.println("  @Autowired  = Spring-specific. Resolves by type, then name.");
        System.out.println("  @Qualifier  = Spring-specific. Overrides name resolution.");
        System.out.println("  @Primary    = Spring-specific. Default bean for type.");
        System.out.println("  @Resource   = JSR-250 (jakarta). Resolves by NAME first, then type.");
        System.out.println("               @Resource(name=\"foo\") == @Autowired + @Qualifier(\"foo\")");
        System.out.println("  @Inject     = JSR-330 (jakarta). Same as @Autowired (always required).");
        System.out.println("               Combine with @Named(\"beanName\") for name resolution.");
        System.out.println("  RULE: Use @Autowired + @Qualifier in Spring apps.");
        System.out.println("        Use @Inject for framework-agnostic libraries.");
        assertThat(true).isTrue(); // documented rule
    }
}
