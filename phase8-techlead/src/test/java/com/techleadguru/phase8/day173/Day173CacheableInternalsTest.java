package com.techleadguru.phase8.day173;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Day173CacheableInternalsTest {

    @Test
    void cacheInterceptorStepsAreOrdered() {
        var steps = Day173CacheableInternals.cacheInterceptorSteps();
        assertFalse(steps.isEmpty());
        for (int i = 0; i < steps.size(); i++) {
            assertEquals(i + 1, steps.get(i).order());
        }
    }

    @Test
    void spelKeyExamplesContainIdExample() {
        var examples = Day173CacheableInternals.spelKeyExamples();
        assertFalse(examples.isEmpty());
        boolean hasId = examples.stream().anyMatch(e -> e.expression().equals("#id"));
        assertTrue(hasId);
    }

    @Test
    void conditionalCacheGuideIsNonEmpty() {
        var guide = Day173CacheableInternals.conditionalCacheGuide();
        assertFalse(guide.isEmpty());
        assertTrue(guide.stream().anyMatch(s -> s.contains("condition")));
    }

    @Test
    void cacheAnnotationComparisonContainsAllThree() {
        var list = Day173CacheableInternals.cacheAnnotationComparison();
        assertEquals(3, list.size());
        boolean hasCacheable  = list.stream().anyMatch(c -> c.annotation().equals("@Cacheable"));
        boolean hasCachePut   = list.stream().anyMatch(c -> c.annotation().equals("@CachePut"));
        boolean hasCacheEvict = list.stream().anyMatch(c -> c.annotation().equals("@CacheEvict"));
        assertTrue(hasCacheable);
        assertTrue(hasCachePut);
        assertTrue(hasCacheEvict);
    }

    @Test
    void cacheManagerTypesIncludeRedis() {
        var types = Day173CacheableInternals.cacheManagerTypes();
        assertFalse(types.isEmpty());
        boolean hasRedis = types.stream().anyMatch(t -> t.name().contains("Redis"));
        assertTrue(hasRedis);
    }
}
