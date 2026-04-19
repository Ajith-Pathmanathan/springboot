package com.techleadguru.phase3.day46;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DAY 46 — Content Negotiation: Serving JSON and XML from the Same Endpoint
 *
 * WHAT IS CONTENT NEGOTIATION?
 *   The process by which a server decides WHICH representation to send:
 *     Client sends: Accept: application/json   → server responds with JSON
 *     Client sends: Accept: application/xml    → server responds with XML
 *     Client sends: Accept: *\/*              → server picks its preferred format
 *
 * HOW SPRING PICKS THE CONVERTER:
 *   1. ContentNegotiationStrategy determines the desired media types from:
 *      a) Accept request header (default, most common)
 *      b) Path extension: /products.json vs /products.xml (deprecated)
 *      c) Request parameter: /products?format=json (can be enabled)
 *   2. Filter converters that can produce the desired media type.
 *   3. Pick the converter with the highest quality factor (q=) match.
 *
 * REQUIREMENTS FOR XML SUPPORT:
 *   1. jackson-dataformat-xml on classpath → Spring Boot auto-registers
 *      MappingJackson2XmlHttpMessageConverter
 *   2. Response class annotated with @JacksonXmlRootElement (for root element name)
 *      OR @XmlRootElement (JAXB annotation — also works with jackson-dataformat-xml)
 *
 * COMMON TECH LEAD QUESTIONS:
 *   "Client sends Accept: application/xml but gets JSON back?"
 *     → Check: is jackson-dataformat-xml on the classpath?
 *     → Check: is the DTO annotated with @JacksonXmlRootElement?
 *     → Check: controller @GetMapping uses produces = {JSON, XML}
 *   "Default format when Accept: *\/*?"
 *     → First producer in the @GetMapping produces list wins (usually JSON first)
 */
@Slf4j
public class Day46ContentNegotiation {

    // =========================================================================
    // Response model — annotated for both JSON (Jackson) and XML (Jackson XML)
    // =========================================================================

    @JacksonXmlRootElement(localName = "salesReport")  // root XML element name
    public record SalesReport(
            String reportId,
            LocalDate generatedOn,
            BigDecimal totalRevenue,
            int orderCount,
            List<String> topProducts
    ) {}

    // =========================================================================
    // Content negotiation configuration
    // =========================================================================

    @Configuration
    public static class ContentNegotiationConfig implements WebMvcConfigurer {

        @Override
        public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
            configurer
                    // JSON is the default when Accept header is *\/*, text\/html, or absent
                    .defaultContentType(MediaType.APPLICATION_JSON)
                    // Ignore path extensions (deprecated, causes issues with dots in URLs)
                    .favorPathExtension(false)
                    // Allow format=json or format=xml query parameter (optional, disabled by default)
                    .favorParameter(false);
        }
    }

    // =========================================================================
    // Controller — same endpoint produces both JSON and XML
    // =========================================================================

    @RestController
    @RequestMapping("/api/day46/reports")
    @Slf4j
    public static class ReportController {

        private static final SalesReport SAMPLE_REPORT = new SalesReport(
                "RPT-2024-001",
                LocalDate.of(2024, 1, 1),
                BigDecimal.valueOf(125_430.50),
                342,
                List.of("Laptop", "Monitor", "Keyboard")
        );

        /**
         * Produces JSON or XML depending on Accept header.
         * Accept: application/json  → MappingJackson2HttpMessageConverter
         * Accept: application/xml   → MappingJackson2XmlHttpMessageConverter
         * Accept: *\/*              → JSON (listed first → highest priority)
         */
        @GetMapping(
                value = "/sales",
                produces = {
                        MediaType.APPLICATION_JSON_VALUE,
                        MediaType.APPLICATION_XML_VALUE
                }
        )
        public SalesReport getSalesReport() {
            log.debug("[Day46] GET /reports/sales — content negotiation determines format");
            return SAMPLE_REPORT;
        }

        /**
         * Endpoint that forces JSON only (ignores Accept header if client requests XML).
         * Returns 406 Not Acceptable for non-JSON Accept headers.
         */
        @GetMapping(value = "/json-only", produces = MediaType.APPLICATION_JSON_VALUE)
        public SalesReport getJsonOnly() {
            return SAMPLE_REPORT;
        }
    }
}
