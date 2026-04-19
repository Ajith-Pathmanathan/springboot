package com.techleadguru.phase3.day46;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 46 — Test: Content negotiation serves JSON or XML based on Accept header.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day46ContentNegotiationTest {

    @Autowired
    MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Test 1: Accept: application/json → JSON response
    // -----------------------------------------------------------------------
    @Test
    void json_accept_returns_json_response() throws Exception {
        mockMvc.perform(get("/api/day46/reports/sales")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reportId").value("RPT-2024-001"))
                .andExpect(jsonPath("$.orderCount").value(342))
                .andExpect(jsonPath("$.topProducts").isArray());

        System.out.println("[DAY 46] Accept: application/json → MappingJackson2HttpMessageConverter → JSON");
    }

    // -----------------------------------------------------------------------
    // Test 2: Accept: application/xml → XML response
    // -----------------------------------------------------------------------
    @Test
    void xml_accept_returns_xml_response() throws Exception {
        String xml = mockMvc.perform(get("/api/day46/reports/sales")
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(xml).contains("salesReport");       // @JacksonXmlRootElement name
        assertThat(xml).contains("RPT-2024-001");
        assertThat(xml).contains("342");

        System.out.println("[DAY 46] Accept: application/xml → MappingJackson2XmlHttpMessageConverter → XML");
        System.out.println("[DAY 46] Requires jackson-dataformat-xml on classpath.");
    }

    // -----------------------------------------------------------------------
    // Test 3: No Accept header → defaults to JSON (listed first in produces)
    // -----------------------------------------------------------------------
    @Test
    void no_accept_header_defaults_to_json() throws Exception {
        mockMvc.perform(get("/api/day46/reports/sales"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        System.out.println("[DAY 46] No Accept header → JSON default (first in produces list)");
    }

    // -----------------------------------------------------------------------
    // Test 4: JSON-only endpoint rejects XML request
    // -----------------------------------------------------------------------
    @Test
    void json_only_endpoint_rejects_xml_accept() throws Exception {
        mockMvc.perform(get("/api/day46/reports/json-only")
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());

        System.out.println("[DAY 46] produces=JSON only → Accept: application/xml → 406 Not Acceptable");
    }

    // -----------------------------------------------------------------------
    // Test 5: Document content negotiation
    // -----------------------------------------------------------------------
    @Test
    void document_content_negotiation() {
        System.out.println("[DAY 46] CONTENT NEGOTIATION STRATEGY:");
        System.out.println();
        System.out.println("  Client:  Accept: application/xml, application/json;q=0.8");
        System.out.println("  Server:  @GetMapping(produces = {JSON, XML})");
        System.out.println("  Result:  XML wins (higher quality factor)");
        System.out.println();
        System.out.println("  XML SUPPORT REQUIREMENTS:");
        System.out.println("    1. com.fasterxml.jackson.dataformat:jackson-dataformat-xml on classpath");
        System.out.println("    2. @JacksonXmlRootElement on response DTO");
        System.out.println("    Spring Boot auto-registers MappingJackson2XmlHttpMessageConverter");
        System.out.println();
        System.out.println("  STRATEGIES (configured in ContentNegotiationConfigurer):");
        System.out.println("    Accept header  — default, most common");
        System.out.println("    Path extension — /products.json vs .xml — DEPRECATED (security risk)");
        System.out.println("    Query param    — ?format=json — opt-in, useful for URL links/downloads");
        assertThat(true).isTrue();
    }
}
