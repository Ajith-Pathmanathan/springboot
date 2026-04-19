package com.techleadguru.phase8.day168;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Day168WebMvcTestTest {

    @Test
    void mockMvcRequestFactoryMethods() {
        var get    = Day168WebMvcTest.MockMvcRequest.get("/api/orders");
        var post   = Day168WebMvcTest.MockMvcRequest.post("/api/orders", "{\"item\":\"x\"}");
        var put    = Day168WebMvcTest.MockMvcRequest.put("/api/orders/1", "{}");
        var delete = Day168WebMvcTest.MockMvcRequest.delete("/api/orders/1");

        assertEquals("GET",    get.method());
        assertEquals("POST",   post.method());
        assertEquals("PUT",    put.method());
        assertEquals("DELETE", delete.method());
        assertNull(get.body());
        assertEquals("{\"item\":\"x\"}", post.body());
        assertTrue(post.headers().containsKey("Content-Type"));
    }

    @Test
    void orderControllerSetupHasMockedBeans() {
        var setup = Day168WebMvcTest.orderControllerSetup();
        assertNotNull(setup.controllerClass());
        assertFalse(setup.mockedBeans().isEmpty());
        assertFalse(setup.securitySetup().isEmpty());
    }

    @Test
    void webMvcTestAnnotationsListIsComplete() {
        var list = Day168WebMvcTest.webMvcTestAnnotations();
        assertFalse(list.isEmpty());
        boolean hasMockBean = list.stream()
                .anyMatch(a -> a.annotation().equals("@MockBean"));
        assertTrue(hasMockBean);
    }

    @Test
    void securityTestPatternsAreNonEmpty() {
        var patterns = Day168WebMvcTest.securityTestPatterns();
        assertFalse(patterns.isEmpty());
        patterns.forEach(p -> assertNotNull(p.scenario()));
    }

    @Test
    void commonMockMvcPatternsAreNonEmpty() {
        var patterns = Day168WebMvcTest.commonMockMvcPatterns();
        assertFalse(patterns.isEmpty());
        assertTrue(patterns.stream().anyMatch(p -> p.contains("status().isOk()")));
    }
}
