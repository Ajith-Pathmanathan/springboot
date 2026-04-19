package com.techleadguru.phase3.day43;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 43 — Test: DispatcherServlet routes requests through the full MVC pipeline.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day43DispatcherServletTest {

    @Autowired
    MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Test 1: GET /api/day43/orders returns JSON array
    // -----------------------------------------------------------------------
    @Test
    void list_orders_returns_json_array() throws Exception {
        mockMvc.perform(get("/api/day43/orders"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));

        System.out.println("[DAY 43] GET /orders → 200 OK, Content-Type: application/json");
        System.out.println("[DAY 43] DispatcherServlet pipeline: HandlerMapping → HandlerAdapter → MessageConverter");
    }

    // -----------------------------------------------------------------------
    // Test 2: GET /api/day43/orders/{id} — existing order
    // -----------------------------------------------------------------------
    @Test
    void get_existing_order_returns_200() throws Exception {
        mockMvc.perform(get("/api/day43/orders/ORD-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ORD-1"))
                .andExpect(jsonPath("$.customerId").value("CUST-A"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        System.out.println("[DAY 43] @PathVariable resolved ORD-1 via PathVariableMethodArgumentResolver");
    }

    // -----------------------------------------------------------------------
    // Test 3: GET unknown → 404 (ResponseStatusExceptionResolver)
    // -----------------------------------------------------------------------
    @Test
    void unknown_order_returns_404() throws Exception {
        mockMvc.perform(get("/api/day43/orders/NONEXISTENT"))
                .andExpect(status().isNotFound());

        System.out.println("[DAY 43] ResponseStatusException(NOT_FOUND) → 404 via ResponseStatusExceptionResolver");
    }

    // -----------------------------------------------------------------------
    // Test 4: POST /api/day43/orders — @RequestBody deserialization
    // -----------------------------------------------------------------------
    @Test
    void create_order_returns_201() throws Exception {
        mockMvc.perform(post("/api/day43/orders")
                        .contentType(APPLICATION_JSON)
                        .content("{\"customerId\":\"CUST-Z\",\"amount\":49.99}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.customerId").value("CUST-Z"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        System.out.println("[DAY 43] @RequestBody JSON → MappingJackson2HttpMessageConverter → CreateOrderRequest");
        System.out.println("[DAY 43] @ResponseStatus(CREATED) → 201 response status");
    }

    // -----------------------------------------------------------------------
    // Test 5: Document the DispatcherServlet pipeline
    // -----------------------------------------------------------------------
    @Test
    void document_dispatcher_servlet_pipeline() {
        System.out.println("[DAY 43] DISPATCHERSERVLET REQUEST LIFECYCLE:");
        System.out.println();
        System.out.println("  HTTP Request (Tomcat)");
        System.out.println("    ↓ DispatcherServlet.doDispatch()");
        System.out.println("    ↓ HandlerMapping.getHandler()  → HandlerExecutionChain");
        System.out.println("    ↓ HandlerAdapter.handle()      → invoke @RequestMapping method");
        System.out.println("      ↓ @RequestBody  → HttpMessageConverter.read()   [JSON → POJO]");
        System.out.println("      ↓ @PathVariable → UriTemplateVariablesHelper");
        System.out.println("      ↓ return value  → HttpMessageConverter.write()  [POJO → JSON]");
        System.out.println("    ↓ On Exception: HandlerExceptionResolver chain");
        System.out.println("      ↓ ExceptionHandlerExceptionResolver (@ControllerAdvice)");
        System.out.println("      ↓ ResponseStatusExceptionResolver  (ResponseStatusException)");
        System.out.println("      ↓ DefaultHandlerExceptionResolver  (MVC built-in exceptions)");
        assertThat(true).isTrue();
    }
}
