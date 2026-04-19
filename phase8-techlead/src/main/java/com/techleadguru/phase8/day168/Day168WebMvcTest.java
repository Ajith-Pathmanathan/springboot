package com.techleadguru.phase8.day168;

import java.util.*;

/**
 * Day 168 — @WebMvcTest: Controller Layer Isolation
 *
 * @WebMvcTest loads only the web layer (Controller, Filter, ControllerAdvice).
 * Use MockMvc to simulate HTTP requests without starting an actual server.
 *
 * Key annotations:
 *   @WebMvcTest(MyController.class)
 *   @MockBean — replaces service beans with Mockito mocks
 *   @WithMockUser — simulates authenticated user (Spring Security)
 */
public class Day168WebMvcTest {

    // ─────────────────────────────────────────────────────────────────────────
    // MockMvc request / assertion descriptors (reference guide)
    // ─────────────────────────────────────────────────────────────────────────

    public record MockMvcRequest(
            String method,
            String url,
            String body,
            Map<String, String> headers) {

        public static MockMvcRequest get(String url) {
            return new MockMvcRequest("GET", url, null, Map.of());
        }

        public static MockMvcRequest post(String url, String body) {
            return new MockMvcRequest("POST", url, body,
                    Map.of("Content-Type", "application/json"));
        }

        public static MockMvcRequest put(String url, String body) {
            return new MockMvcRequest("PUT", url, body,
                    Map.of("Content-Type", "application/json"));
        }

        public static MockMvcRequest delete(String url) {
            return new MockMvcRequest("DELETE", url, null, Map.of());
        }
    }

    public record MockMvcAssertion(
            int    expectedStatus,
            String expectedContentType,
            List<String> jsonPathAssertions) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Controller test setup reference
    // ─────────────────────────────────────────────────────────────────────────

    public record ControllerTestSetup(
            String controllerClass,
            List<String> mockedBeans,
            List<String> securitySetup,
            String description) {}

    public static ControllerTestSetup orderControllerSetup() {
        return new ControllerTestSetup(
            "OrderController",
            List.of("OrderService", "OrderMapper"),
            List.of("@WithMockUser(roles=\"USER\")", "csrf()"),
            "WebMvcTest for OrderController: mocks service layer, real dispatcher servlet"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Annotation guide
    // ─────────────────────────────────────────────────────────────────────────

    public record AnnotationInfo(String annotation, String purpose, String example) {}

    public static List<AnnotationInfo> webMvcTestAnnotations() {
        return List.of(
            new AnnotationInfo(
                "@WebMvcTest",
                "Load only web layer for a specific controller",
                "@WebMvcTest(OrderController.class)"),
            new AnnotationInfo(
                "@MockBean",
                "Replace service/repository beans with Mockito mocks",
                "@MockBean private OrderService orderService;"),
            new AnnotationInfo(
                "@AutoConfigureMockMvc",
                "Auto-configure MockMvc instance",
                "Included automatically with @WebMvcTest"),
            new AnnotationInfo(
                "@WithMockUser",
                "Simulate an authenticated user for Spring Security",
                "@WithMockUser(username=\"alice\", roles={\"ADMIN\"})"),
            new AnnotationInfo(
                "@Import",
                "Import additional configuration (e.g. security config beans)",
                "@Import(SecurityConfig.class)"),
            new AnnotationInfo(
                "@JsonTest",
                "Test JSON serialisation/deserialisation in isolation",
                "@JsonTest + JacksonTester<MyDto>")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Security test patterns
    // ─────────────────────────────────────────────────────────────────────────

    public record SecurityPattern(String scenario, String setup, String assertion) {}

    public static List<SecurityPattern> securityTestPatterns() {
        return List.of(
            new SecurityPattern(
                "Authenticated user with role",
                "@WithMockUser(username=\"alice\", roles={\"USER\"})",
                "mockMvc.perform(get(\"/api/orders\")).andExpect(status().isOk())"),
            new SecurityPattern(
                "CSRF token for POST",
                "mockMvc.perform(post(\"/api/orders\").with(csrf())…)",
                "andExpect(status().isCreated())"),
            new SecurityPattern(
                "Unauthenticated request",
                "No security setup",
                "andExpect(status().isUnauthorized()) or status().isForbidden()"),
            new SecurityPattern(
                "Admin-only endpoint",
                "@WithMockUser(roles={\"ADMIN\"})",
                "andExpect(status().isOk()) — USER role: issForbidden()"),
            new SecurityPattern(
                "Custom UserDetails",
                "@WithUserDetails(\"testUser\") + UserDetailsService bean",
                "Real UserDetailsService is called to load the user")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Common MockMvc patterns
    // ─────────────────────────────────────────────────────────────────────────

    public static List<String> commonMockMvcPatterns() {
        return List.of(
            "mockMvc.perform(get(\"/api/orders/{id}\", 1L)).andExpect(status().isOk())" +
                ".andExpect(jsonPath(\"$.orderId\").value(1L))",
            "mockMvc.perform(post(\"/api/orders\").contentType(APPLICATION_JSON)" +
                ".content(json)).andExpect(status().isCreated())" +
                ".andExpect(header().exists(\"Location\"))",
            "mockMvc.perform(delete(\"/api/orders/{id}\", 99L).with(csrf()))" +
                ".andExpect(status().isNoContent())",
            "mockMvc.perform(get(\"/api/orders\")).andExpect(jsonPath(\"$\", hasSize(3)))",
            "mockMvc.perform(get(\"/api/orders\")).andExpect(content().contentType(APPLICATION_JSON_VALUE))",
            "mockMvc.perform(get(\"/bad\")).andExpect(status().isNotFound())" +
                ".andExpect(jsonPath(\"$.message\").exists())"
        );
    }
}
