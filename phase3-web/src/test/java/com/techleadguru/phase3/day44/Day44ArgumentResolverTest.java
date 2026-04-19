package com.techleadguru.phase3.day44;

import com.techleadguru.phase3.day44.Day44ArgumentResolver.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 44 — Test: @CurrentUser HandlerMethodArgumentResolver resolves UserContext from headers.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day44ArgumentResolverTest {

    @Autowired
    MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Test 1: Resolver populates all UserContext fields from headers
    // -----------------------------------------------------------------------
    @Test
    void resolver_populates_user_context_from_headers() throws Exception {
        mockMvc.perform(get("/api/day44/profile")
                        .header("X-User-Id", "USR-42")
                        .header("X-User-Name", "Alice")
                        .header("X-User-Roles", "USER,VIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("USR-42"))
                .andExpect(jsonPath("$.username").value("Alice"))
                .andExpect(jsonPath("$.isAdmin").value(false));

        System.out.println("[DAY 44] @CurrentUser UserContext resolved from X-User-Id/Name/Roles headers");
    }

    // -----------------------------------------------------------------------
    // Test 2: ADMIN role propagated correctly
    // -----------------------------------------------------------------------
    @Test
    void admin_role_grants_access_to_admin_endpoint() throws Exception {
        mockMvc.perform(get("/api/day44/profile/admin-only")
                        .header("X-User-Id", "USR-1")
                        .header("X-User-Name", "Bob")
                        .header("X-User-Roles", "ADMIN,USER"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bob")));

        System.out.println("[DAY 44] ADMIN role check passed — user.hasRole('ADMIN') = true");
    }

    // -----------------------------------------------------------------------
    // Test 3: Missing X-User-Id → 401
    // -----------------------------------------------------------------------
    @Test
    void missing_user_id_header_returns_401() throws Exception {
        mockMvc.perform(get("/api/day44/profile"))
                .andExpect(status().isUnauthorized());

        System.out.println("[DAY 44] Missing X-User-Id header → resolver threw 401");
    }

    // -----------------------------------------------------------------------
    // Test 4: Non-admin cannot access admin endpoint → 403
    // -----------------------------------------------------------------------
    @Test
    void non_admin_cannot_access_admin_endpoint() throws Exception {
        mockMvc.perform(get("/api/day44/profile/admin-only")
                        .header("X-User-Id", "USR-2")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());

        System.out.println("[DAY 44] USER role rejected from admin endpoint → 403 Forbidden");
    }

    // -----------------------------------------------------------------------
    // Test 5: Document the resolver mechanism
    // -----------------------------------------------------------------------
    @Test
    void document_argument_resolver_mechanism() {
        System.out.println("[DAY 44] HOW HandlerMethodArgumentResolver WORKS:");
        System.out.println();
        System.out.println("  RequestMappingHandlerAdapter.invokeHandlerMethod()");
        System.out.println("    → for each method param, ask each resolver:");
        System.out.println("        resolver.supportsParameter(param)? → true/false");
        System.out.println("    → first resolver returning true wins:");
        System.out.println("        value = resolver.resolveArgument(param, request, ...)");
        System.out.println("    → inject value into method call");
        System.out.println();
        System.out.println("  REGISTRATION: WebMvcConfigurer.addArgumentResolvers()");
        System.out.println("  Custom resolvers run BEFORE built-in resolvers.");
        System.out.println();
        System.out.println("  REAL-WORLD PATTERNS:");
        System.out.println("    @CurrentUser  → resolve from JWT token (Authorization header)");
        System.out.println("    @TenantId     → resolve from X-Tenant-Id or subdomain");
        System.out.println("    @ClientVersion → resolve from X-App-Version header");
        assertThat(true).isTrue();
    }
}
