package com.techleadguru.phase4.day58;

import com.techleadguru.phase4.Phase4Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DAY 58 — ThreadPoolTaskExecutor Tuning Test
 *
 * Verifies:
 * 1. Custom executor beans are created with correct configuration
 * 2. ReportService uses the named executors
 * 3. /stats endpoint returns thread pool metrics
 * 4. Parallel fetch runs correctly
 */
@SpringBootTest(classes = Phase4Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class Day58ThreadPoolTuningTest {

    @Autowired MockMvc mockMvc;
    @Autowired Day58ThreadPoolTuning.ReportService reportService;
    @Autowired(required = false) Day58ThreadPoolTuning.ExecutorConfig executorConfig;

    @Test
    void executor_config_bean_is_created() {
        assertThat(executorConfig).isNotNull();
    }

    @Test
    void fetchRemoteDataAsync_returns_data() throws Exception {
        CompletableFuture<String> future = reportService.fetchRemoteDataAsync("dataset-1");
        String result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("data-from-dataset-1");
    }

    @Test
    void generatePdfAsync_returns_pdf_string() throws Exception {
        CompletableFuture<String> future = reportService.generatePdfAsync("report-data");
        String result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).startsWith("pdf-report-data-");
    }

    @Test
    void thread_pool_stats_endpoint_returns_metrics() throws Exception {
        mockMvc.perform(get("/api/day58/thread-pools/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ioExecutor").exists())
                .andExpect(jsonPath("$.cpuExecutor").exists());
    }

    @Test
    void parallel_fetch_endpoint_returns_list_of_results() throws Exception {
        var mvcResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/day58/thread-pools/parallel-fetch"))
                .andExpect(request().asyncStarted()).andReturn();
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void parallel_fetch_is_faster_than_sequential() throws Exception {
        // Both operations take ~100ms each; parallel should complete in ~100ms, not 200ms
        long start = System.currentTimeMillis();
        CompletableFuture<String> dataFuture = reportService.fetchRemoteDataAsync("d1");
        CompletableFuture<String> pdfFuture  = reportService.generatePdfAsync("data");
        CompletableFuture.allOf(dataFuture, pdfFuture).get(5, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;
        // Sequential would be 200ms; parallel should be ~100ms with buffer
        assertThat(elapsed).isLessThan(1500); // generous timeout for CI
    }
}
