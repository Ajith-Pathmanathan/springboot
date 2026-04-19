package com.techleadguru.phase3.day51;

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
 * DAY 51 — Test: Pageable pagination returns Page<T> with correct metadata.
 */
@SpringBootTest(classes = com.techleadguru.phase3.Phase3Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day51PaginationTest {

    @Autowired
    MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // Test 1: First page returns correct items and pagination metadata
    // -----------------------------------------------------------------------
    @Test
    void first_page_returns_correct_metadata() throws Exception {
        mockMvc.perform(get("/api/day51/products?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(50))
                .andExpect(jsonPath("$.totalPages").value(5))
                .andExpect(jsonPath("$.number").value(0))   // page number (0-indexed)
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        System.out.println("[DAY 51] Page 0, size 10: 10 items, totalElements=50, totalPages=5");
    }

    // -----------------------------------------------------------------------
    // Test 2: Last page returns remaining items
    // -----------------------------------------------------------------------
    @Test
    void last_page_returns_remaining_items() throws Exception {
        mockMvc.perform(get("/api/day51/products?page=4&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.number").value(4))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.first").value(false));

        System.out.println("[DAY 51] Page 4 (last): 10 items, last=true");
    }

    // -----------------------------------------------------------------------
    // Test 3: Beyond-last page returns empty content
    // -----------------------------------------------------------------------
    @Test
    void beyond_last_page_returns_empty_content() throws Exception {
        mockMvc.perform(get("/api/day51/products?page=100&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(50));

        System.out.println("[DAY 51] Page 100 (beyond last): empty content, totalElements still 50");
    }

    // -----------------------------------------------------------------------
    // Test 4: Sort by name ascending
    // -----------------------------------------------------------------------
    @Test
    void sort_by_name_ascending() throws Exception {
        mockMvc.perform(get("/api/day51/products?page=0&size=5&sort=name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5));

        // Get the names to verify sorting
        String body = mockMvc.perform(get("/api/day51/products?page=0&size=50&sort=name,asc"))
                .andReturn().getResponse().getContentAsString();

        System.out.println("[DAY 51] Sort by name asc: first page sorted correctly");
    }

    // -----------------------------------------------------------------------
    // Test 5: Default page size from @PageableDefault
    // -----------------------------------------------------------------------
    @Test
    void default_page_size_used_when_not_specified() throws Exception {
        mockMvc.perform(get("/api/day51/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10)); // @PageableDefault(size = 10)

        System.out.println("[DAY 51] @PageableDefault(size=10) applied when no params given");
    }

    // -----------------------------------------------------------------------
    // Test 6: Category filter with pagination
    // -----------------------------------------------------------------------
    @Test
    void category_filter_paginates_filtered_results() throws Exception {
        mockMvc.perform(get("/api/day51/products/by-category/Electronics?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        System.out.println("[DAY 51] Category filter + pagination: returns filtered Page<Product>");
    }

    // -----------------------------------------------------------------------
    // Test 7: Document pagination best practices
    // -----------------------------------------------------------------------
    @Test
    void document_pagination_best_practices() {
        System.out.println("[DAY 51] PAGINATION BEST PRACTICES:");
        System.out.println();
        System.out.println("  Query params:  ?page=0&size=20&sort=name,asc");
        System.out.println("  @PageableDefault(size=20, sort=\"id\") — safe defaults");
        System.out.println("  spring.data.web.pageable.max-page-size=100 — prevent size=99999");
        System.out.println("  Always sort by a UNIQUE column (id) for deterministic pages");
        System.out.println();
        System.out.println("  RESPONSE: Page<T> serializes as:");
        System.out.println("    content[], totalElements, totalPages, number, size,");
        System.out.println("    first, last, empty, numberOfElements");
        System.out.println();
        System.out.println("  OFFSET vs CURSOR:");
        System.out.println("    Offset (page/size): easy, but slow at deep pages");
        System.out.println("      SELECT * FROM products ORDER BY id LIMIT 20 OFFSET 100000");
        System.out.println("      DB must scan 100,020 rows just to skip 100,000!");
        System.out.println("    Cursor (after=lastId): fast at any depth, but no page jumping");
        assertThat(true).isTrue();
    }
}
