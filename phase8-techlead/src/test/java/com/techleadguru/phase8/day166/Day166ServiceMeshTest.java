package com.techleadguru.phase8.day166;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Day166ServiceMeshTest {

    @Test
    void meshVsSpringCloudListIsNonEmpty() {
        var list = Day166ServiceMesh.meshVsSpringCloud();
        assertFalse(list.isEmpty());
        list.forEach(f -> {
            assertNotNull(f.category());
            assertNotNull(f.meshApproach());
            assertNotNull(f.springCloudEquivalent());
        });
    }

    @Test
    void exampleSidecarHasExpectedValues() {
        var sidecar = Day166ServiceMesh.exampleSidecar();
        assertEquals("envoy", sidecar.proxyType());
        assertEquals(15001,   sidecar.proxyPort());
        assertFalse(sidecar.capabilities().isEmpty());
    }

    @Test
    void whenToUseServiceMeshContainsKeyFactors() {
        var guide = Day166ServiceMesh.whenToUseServiceMesh();
        assertFalse(guide.isEmpty());
        boolean hasLanguageFactor = guide.stream()
                .anyMatch(d -> d.factor().toLowerCase().contains("language"));
        assertTrue(hasLanguageFactor);
    }

    @Test
    void istioAnnotationsMapIsNonEmpty() {
        var annotations = Day166ServiceMesh.istioAnnotations();
        assertFalse(annotations.isEmpty());
        assertTrue(annotations.containsKey("sidecar.istio.io/inject"));
    }
}
