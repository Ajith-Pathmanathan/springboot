package com.techleadguru.phase3.day55;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 55 — Test: CORS preflight and actual request handling.
 *
 * The default test properties already set:
 *   phase3.day55.cors.enabled=true
 *   phase3.day55.cors.allowed-origin=http://localhost:3000
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day55CorsTest {

    @Autowired
    MockMvc mockMvc;

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String BLOCKED_ORIGIN  = "http://evil.example.com";

    // -----------------------------------------------------------------------
    // Test 1: OPTIONS preflight from allowed origin → 200 + CORS allow headers
    // -----------------------------------------------------------------------
    @Test
    void preflight_from_allowed_origin_returns_cors_headers() throws Exception {
        mockMvc.perform(options("/api/day55/data")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")));

        System.out.println("[DAY 55] OPTIONS preflight from allowed origin → 200 + allow headers");
    }

    // -----------------------------------------------------------------------
    // Test 2: GET from allowed origin includes Access-Control-Allow-Origin
    // -----------------------------------------------------------------------
    @Test
    void get_from_allowed_origin_includes_cors_header() throws Exception {
        mockMvc.perform(get("/api/day55/data")
                        .header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));

        System.out.println("[DAY 55] GET from allowed origin → Access-Control-Allow-Origin present");
    }

    // -----------------------------------------------------------------------
    // Test 3: Access-Control-Allow-Credentials is true for allowed origin
    // -----------------------------------------------------------------------
    @Test
    void preflight_includes_allow_credentials_header() throws Exception {
        mockMvc.perform(options("/api/day55/data")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        System.out.println("[DAY 55] Access-Control-Allow-Credentials: true (required for cookies)");
    }

    // -----------------------------------------------------------------------
    // Test 4: Preflight for disallowed origin → no CORS headers (blocked)
    // -----------------------------------------------------------------------
    @Test
    void preflight_from_disallowed_origin_has_no_allow_origin_header() throws Exception {
        mockMvc.perform(options("/api/day55/data")
                        .header("Origin", BLOCKED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        System.out.println("[DAY 55] Preflight from blocked origin → no Access-Control-Allow-Origin");
        System.out.println("[DAY 55] Browser will block the preflight → request never reaches server");
    }

    // -----------------------------------------------------------------------
    // Test 5: CORS guide endpoint (documentation controller)
    // -----------------------------------------------------------------------
    @Test
    void cors_guide_endpoint_returns_json_guide() throws Exception {
        mockMvc.perform(get("/api/day55/cors-guide")
                        .header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].scenario").isNotEmpty());

        System.out.println("[DAY 55] /cors-guide endpoint returns CORS misconceptions guide");
    }

    // -----------------------------------------------------------------------
    // Test 6: Document CORS misconceptions
    // -----------------------------------------------------------------------
    @Test
    void document_cors_misconceptions() {
        System.out.println("[DAY 55] CORS MISCONCEPTIONS:");
        System.out.println();
        System.out.println("  WRONG #1: allowedOrigins(\"*\") + allowCredentials(true)");
        System.out.println("    → Spring throws IllegalArgumentException (security restriction)");
        System.out.println("    → Wildcards cannot be combined with credentials");
        System.out.println();
        System.out.println("  WRONG #2: CORS is a server-side security mechanism");
        System.out.println("    → FALSE. CORS is enforced by the BROWSER only.");
        System.out.println("    → curl/Postman/server-to-server calls ignore CORS entirely.");
        System.out.println("    → CORS only prevents cross-origin JS code from reading responses.");
        System.out.println();
        System.out.println("  WRONG #3: Blocking CORS prevents the request from reaching the server");
        System.out.println("    → FALSE for simple requests (GET, POST/text). Browser allows the");
        System.out.println("    → request but blocks JavaScript from reading the response.");
        System.out.println("    → For preflighted requests, the actual request IS blocked.");
        System.out.println();
        System.out.println("  RIGHT: Use specific allowed origins in production:");
        System.out.println("    → allowedOriginPatterns(\"https://*.myapp.com\")");
        System.out.println("    → Never use allowedOrigins(\"*\") for authenticated endpoints");
        assertThat(true).isTrue();
    }
}
