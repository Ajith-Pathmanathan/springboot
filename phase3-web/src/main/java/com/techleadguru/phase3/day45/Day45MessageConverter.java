package com.techleadguru.phase3.day45;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * DAY 45 — HttpMessageConverter Internals
 *
 * WHAT IS AN HttpMessageConverter?
 *   It converts between HTTP message bodies and Java objects.
 *   Two operations:
 *     read(Class type, HttpInputMessage)  → deserialize request body → Java object
 *     write(T object, MediaType, HttpOutputMessage) → serialize Java object → response body
 *
 * HOW SPRING SELECTS A CONVERTER (Content Negotiation):
 *   For WRITING (response):
 *     1. Collect all converters that canWrite(returnType, *\/*)
 *     2. Collect all media types from Accept header (client's preference)
 *     3. Find intersection → select the best match (highest quality factor)
 *   For READING (request body):
 *     1. Collect all converters that canRead(paramType, Content-Type)
 *
 * DEFAULT CONVERTERS registered by Spring Boot (in priority order):
 *   ByteArrayHttpMessageConverter      → byte[], *\/*
 *   StringHttpMessageConverter         → String, text\/plain, *\/*
 *   ResourceHttpMessageConverter       → Resource
 *   MappingJackson2HttpMessageConverter → any object, application\/json
 *   Jaxb2RootElementHttpMessageConverter → @XmlRootElement objects, application\/xml
 *
 * REGISTRATION:
 *   extendMessageConverters() → ADDS to the default list (Jackson stays active)
 *   configureMessageConverters() → REPLACES the entire default list (use with care!)
 *
 * WHY TECH LEADS CARE:
 *   "Why is my API returning XML instead of JSON?" → Accept header sent by client
 *   "Why did my custom format not work?" → Converter not registered or wrong media type
 *   "Client sends CSV, how do I parse it?" → Implement read() in your converter
 */
@Slf4j
public class Day45MessageConverter {

    // =========================================================================
    // Product model used in this demo
    // =========================================================================

    public record Product(String id, String name, BigDecimal price, String category) {}

    // =========================================================================
    // Custom CSV converter — converts List<Product> to/from text/csv
    // =========================================================================

    public static class CsvProductConverter extends AbstractHttpMessageConverter<List<Product>> {

        private static final MediaType TEXT_CSV = new MediaType("text", "csv");

        public CsvProductConverter() {
            super(TEXT_CSV);
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return List.class.isAssignableFrom(clazz);
        }

        /**
         * Deserialize CSV → List<Product>
         * Format: id,name,price,category  (one product per line, first line = header)
         */
        @Override
        protected List<Product> readInternal(Class<? extends List<Product>> clazz,
                                             HttpInputMessage inputMessage) throws IOException {
            String csv = new String(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);
            List<Product> products = new ArrayList<>();
            String[] lines = csv.split("\n");
            for (int i = 1; i < lines.length; i++) { // skip header row
                String[] cols = lines[i].trim().split(",");
                if (cols.length == 4) {
                    products.add(new Product(cols[0], cols[1], new BigDecimal(cols[2]), cols[3]));
                }
            }
            log.debug("[Day45] CsvProductConverter.read() parsed {} products", products.size());
            return products;
        }

        /**
         * Serialize List<Product> → CSV
         * First row is the header; subsequent rows are data.
         */
        @Override
        protected void writeInternal(List<Product> products, HttpOutputMessage outputMessage) throws IOException {
            StringBuilder sb = new StringBuilder("id,name,price,category\n");
            for (Product p : products) {
                sb.append(p.id()).append(",")
                        .append(p.name()).append(",")
                        .append(p.price()).append(",")
                        .append(p.category()).append("\n");
            }
            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            outputMessage.getBody().write(bytes);
            log.debug("[Day45] CsvProductConverter.write() wrote {} products as CSV", products.size());
        }
    }

    // =========================================================================
    // WebMvcConfigurer — registers the CSV converter alongside Jackson (JSON)
    // =========================================================================

    @Configuration
    @Slf4j
    public static class Day45MvcConfig implements WebMvcConfigurer {

        /**
         * extendMessageConverters() ADDS to defaults — Jackson JSON stays active.
         * configureMessageConverters() REPLACES defaults — would remove Jackson!
         */
        @Override
        public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
            converters.add(0, new CsvProductConverter()); // add first = higher priority
            log.debug("[Day45] CsvProductConverter registered at index 0");
        }
    }

    // =========================================================================
    // Controller — same endpoint returns JSON or CSV based on Accept header
    // =========================================================================

    @RestController
    @RequestMapping("/api/day45/products")
    @Slf4j
    public static class ProductController {

        private static final List<Product> CATALOG = List.of(
                new Product("P001", "Laptop", BigDecimal.valueOf(1299.99), "Electronics"),
                new Product("P002", "Mouse", BigDecimal.valueOf(29.99), "Accessories"),
                new Product("P003", "Keyboard", BigDecimal.valueOf(79.99), "Accessories")
        );

        /**
         * Returns product list.
         * Accept: application/json → MappingJackson2HttpMessageConverter → JSON
         * Accept: text/csv        → CsvProductConverter                 → CSV rows
         */
        @GetMapping(produces = {
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                "text/csv"
        })
        public List<Product> listProducts() {
            log.debug("[Day45] GET /products — converter selected by Accept header");
            return CATALOG;
        }
    }
}
