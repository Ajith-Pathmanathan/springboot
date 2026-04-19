package com.techleadguru.phase3.day45;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 45 — Test: CsvProductConverter is selected when Accept: text/csv.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day45MessageConverterTest {

    @Autowired
    MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Test 1: Default Accept (*/*) → JSON via Jackson
    // -----------------------------------------------------------------------
    @Test
    void default_accept_returns_json() throws Exception {
        mockMvc.perform(get("/api/day45/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value("P001"))
                .andExpect(jsonPath("$[0].name").value("Laptop"));

        System.out.println("[DAY 45] Accept: */* → MappingJackson2HttpMessageConverter → JSON");
    }

    // -----------------------------------------------------------------------
    // Test 2: Accept: text/csv → CsvProductConverter
    // -----------------------------------------------------------------------
    @Test
    void text_csv_accept_returns_csv_format() throws Exception {
        String csv = mockMvc.perform(get("/api/day45/products")
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(csv).contains("id,name,price,category"); // header row
        assertThat(csv).contains("P001,Laptop");
        assertThat(csv).contains("P002,Mouse");

        System.out.println("[DAY 45] Accept: text/csv → CsvProductConverter → CSV output");
        System.out.println("[DAY 45] CSV:\n" + csv);
    }

    // -----------------------------------------------------------------------
    // Test 3: Accept: application/json explicitly → JSON
    // -----------------------------------------------------------------------
    @Test
    void explicit_json_accept_returns_json() throws Exception {
        mockMvc.perform(get("/api/day45/products")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));

        System.out.println("[DAY 45] Explicit Accept: application/json → JSON converter selected");
    }

    // -----------------------------------------------------------------------
    // Test 4: Unsupported Accept → 406 Not Acceptable
    // -----------------------------------------------------------------------
    @Test
    void unsupported_accept_returns_406() throws Exception {
        mockMvc.perform(get("/api/day45/products")
                        .accept("text/html"))
                .andExpect(status().isNotAcceptable());

        System.out.println("[DAY 45] Accept: text/html → No matching converter → 406 Not Acceptable");
    }

    // -----------------------------------------------------------------------
    // Test 5: Document converter selection
    // -----------------------------------------------------------------------
    @Test
    void document_converter_selection_algorithm() {
        System.out.println("[DAY 45] HTTP MESSAGE CONVERTER SELECTION:");
        System.out.println();
        System.out.println("  On WRITE (response body):");
        System.out.println("    1. Get desired media types from Accept header");
        System.out.println("    2. For each converter: canWrite(returnType, mediaType)?");
        System.out.println("    3. Find best match (highest quality factor q=)");
        System.out.println("    4. If no match → 406 Not Acceptable");
        System.out.println();
        System.out.println("  On READ (request body):");
        System.out.println("    1. Get Content-Type from request header");
        System.out.println("    2. For each converter: canRead(paramType, contentType)?");
        System.out.println("    3. First matching converter wins");
        System.out.println("    4. If no match → 415 Unsupported Media Type");
        System.out.println();
        System.out.println("  REGISTRATION:");
        System.out.println("    extendMessageConverters()  → ADDS (keep Jackson)");
        System.out.println("    configureMessageConverters() → REPLACES (removes Jackson!)");
        assertThat(true).isTrue();
    }
}
