package com.techleadguru.phase3.day49;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 49 — Test: @ControllerAdvice maps exceptions to RFC 7807 ProblemDetail.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day49GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Test 1: ResourceNotFoundException → 404 ProblemDetail
    // -----------------------------------------------------------------------
    @Test
    void resource_not_found_exception_returns_404_problem_detail() throws Exception {
        mockMvc.perform(get("/api/day49/orders/ORD-MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("ORD-MISSING")))
                .andExpect(jsonPath("$.resourceType").value("Order"))
                .andExpect(jsonPath("$.resourceId").value("ORD-MISSING"));

        System.out.println("[DAY 49] ResourceNotFoundException → 404 ProblemDetail (RFC 7807)");
    }

    // -----------------------------------------------------------------------
    // Test 2: Existing order returns 200 (no exception)
    // -----------------------------------------------------------------------
    @Test
    void existing_order_returns_200() throws Exception {
        mockMvc.perform(get("/api/day49/orders/ORD-10"))
                .andExpect(status().isOk());

        System.out.println("[DAY 49] Existing order ORD-10 → 200 OK (no exception thrown)");
    }

    // -----------------------------------------------------------------------
    // Test 3: BusinessRuleException → 422 ProblemDetail
    // -----------------------------------------------------------------------
    @Test
    void business_rule_exception_returns_422_problem_detail() throws Exception {
        mockMvc.perform(post("/api/day49/orders/ORD-10/cancel"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Business Rule Violation"))
                .andExpect(jsonPath("$.errorCode").value("ORDER_ALREADY_SHIPPED"));

        System.out.println("[DAY 49] BusinessRuleException → 422 Unprocessable Entity ProblemDetail");
    }

    // -----------------------------------------------------------------------
    // Test 4: @Valid failure → 400 ProblemDetail with fieldErrors
    // -----------------------------------------------------------------------
    @Test
    void validation_failure_returns_400_with_field_errors() throws Exception {
        mockMvc.perform(post("/api/day49/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"\",\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        System.out.println("[DAY 49] @Valid failure → 400 ProblemDetail with fieldErrors list");
    }

    // -----------------------------------------------------------------------
    // Test 5: ProblemDetail has required RFC 7807 fields
    // -----------------------------------------------------------------------
    @Test
    void problem_detail_has_all_rfc7807_required_fields() throws Exception {
        String body = mockMvc.perform(get("/api/day49/orders/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("\"type\"");
        assertThat(body).contains("\"title\"");
        assertThat(body).contains("\"status\"");
        assertThat(body).contains("\"detail\"");

        System.out.println("[DAY 49] ProblemDetail contains RFC 7807 required fields: type, title, status, detail");
        System.out.println("[DAY 49] Body: " + body);
    }

    // -----------------------------------------------------------------------
    // Test 6: Document ProblemDetail
    // -----------------------------------------------------------------------
    @Test
    void document_rfc7807_problem_detail() {
        System.out.println("[DAY 49] RFC 7807 PROBLEM DETAILS:");
        System.out.println();
        System.out.println("  Standard response format for HTTP API errors:");
        System.out.println("  {");
        System.out.println("    \"type\":     \"https://api.example.com/errors/not-found\",");
        System.out.println("    \"title\":    \"Resource Not Found\",");
        System.out.println("    \"status\":   404,");
        System.out.println("    \"detail\":   \"Order ORD-99 does not exist.\",");
        System.out.println("    \"instance\": \"/api/orders/ORD-99\"");
        System.out.println("  }");
        System.out.println();
        System.out.println("  Spring Boot 3 / Spring 6: ProblemDetail is built-in!");
        System.out.println("  ResponseEntityExceptionHandler already returns ProblemDetail.");
        System.out.println("  Content-Type: application/problem+json");
        System.out.println();
        System.out.println("  @RestControllerAdvice = @ControllerAdvice + @ResponseBody");
        System.out.println("  Applied globally to ALL @RestController classes.");
        assertThat(true).isTrue();
    }
}
