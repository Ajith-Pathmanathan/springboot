package com.techleadguru.phase3.day51;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;

/**
 * DAY 51 — REST Pagination: Pageable + Page<T>
 *
 * THE PROBLEM: GET /api/products might return 1,000,000 rows.
 *   → Kills the database with a full table scan
 *   → Kills the API server serializing 1M objects
 *   → Kills the client parsing 1M objects
 *   → Network timeout for users
 *
 * THE SOLUTION: Server-side pagination
 *   GET /api/products?page=0&size=20&sort=name,asc
 *   → Return 20 items + metadata:
 *     {
 *       "content": [...20 products...],
 *       "totalElements": 1000000,
 *       "totalPages": 50000,
 *       "number": 0,
 *       "size": 20,
 *       "first": true,
 *       "last": false
 *     }
 *
 * HOW Spring Data Pageable WORKS:
 *   1. Spring Boot auto-registers PageableHandlerMethodArgumentResolver
 *   2. Resolver converts query params → Pageable object:
 *        page=0&size=20&sort=name,asc → PageRequest.of(0, 20, Sort.by("name").ascending())
 *   3. Controller receives Pageable → pass to repository.findAll(pageable)
 *   4. Repository returns Page<T> (from JPA) or you build PageImpl<T> manually
 *
 * BEST PRACTICES:
 *   @PageableDefault(size = 20, sort = "id") — defaults for when client omits params
 *   Set spring.data.web.pageable.max-page-size=100 — prevent size=999999
 *   Always sort by a unique column (id) to prevent non-deterministic pages
 *   Return Page<T> not List<T> so clients know totalPages/totalElements
 *
 * CURSOR-BASED vs OFFSET PAGINATION:
 *   Offset (page/size):  Easy to implement, but slow on large pages (big OFFSET scan)
 *   Cursor (after=lastId): Fast at any depth, but can't jump to "page 100"
 *   Use cursor for infinite scroll, offset for numbered pages
 */
@Slf4j
public class Day51Pagination {

    public record Product(String id, String name, BigDecimal price, String category) {}

    // =========================================================================
    // In-memory "repository" with pagination support
    // =========================================================================

    @Service
    @Slf4j
    public static class ProductCatalogService {

        private final List<Product> products;

        public ProductCatalogService() {
            // Generate 50 products for demo
            products = new ArrayList<>();
            String[] categories = {"Electronics", "Books", "Clothing", "Home", "Sports"};
            String[] names = {"Widget", "Gadget", "Device", "Tool", "Accessory"};
            for (int i = 1; i <= 50; i++) {
                products.add(new Product(
                        "P" + String.format("%03d", i),
                        names[(i - 1) % names.length] + "-" + i,
                        BigDecimal.valueOf(10 + (i * 3.5)),
                        categories[(i - 1) % categories.length]
                ));
            }
        }

        public Page<Product> findAll(Pageable pageable) {
            List<Product> sorted = new ArrayList<>(products);

            // Apply sorting
            if (pageable.getSort().isSorted()) {
                for (Sort.Order order : pageable.getSort()) {
                    Comparator<Product> cmp = switch (order.getProperty()) {
                        case "name"  -> Comparator.comparing(Product::name);
                        case "price" -> Comparator.comparing(Product::price);
                        case "id"    -> Comparator.comparing(Product::id);
                        default      -> Comparator.comparing(Product::id);
                    };
                    if (order.isDescending()) cmp = cmp.reversed();
                    sorted.sort(cmp);
                }
            }

            // Apply pagination
            int offset = (int) pageable.getOffset();
            int pageSize = pageable.getPageSize();
            List<Product> pageContent = offset >= sorted.size()
                    ? Collections.emptyList()
                    : sorted.subList(offset, Math.min(offset + pageSize, sorted.size()));

            log.debug("[Day51] findAll: page={}, size={}, total={}, returned={}",
                    pageable.getPageNumber(), pageSize, sorted.size(), pageContent.size());

            return new PageImpl<>(pageContent, pageable, sorted.size());
        }

        public Page<Product> findByCategory(String category, Pageable pageable) {
            List<Product> filtered = products.stream()
                    .filter(p -> p.category().equalsIgnoreCase(category))
                    .toList();

            int offset = (int) pageable.getOffset();
            int pageSize = pageable.getPageSize();
            List<Product> pageContent = offset >= filtered.size()
                    ? Collections.emptyList()
                    : filtered.subList(offset, Math.min(offset + pageSize, filtered.size()));

            return new PageImpl<>(pageContent, pageable, filtered.size());
        }

        public int getTotalCount() {
            return products.size();
        }
    }

    // =========================================================================
    // Controller — Pageable auto-resolved from query params
    // =========================================================================

    @RestController
    @RequestMapping("/api/day51/products")
    @Slf4j
    public static class ProductPageController {

        private final ProductCatalogService service;

        public ProductPageController(ProductCatalogService service) {
            this.service = service;
        }

        /**
         * GET /api/day51/products?page=0&size=10&sort=name,asc
         * Pageable is auto-resolved by PageableHandlerMethodArgumentResolver.
         * @PageableDefault sets defaults when client omits parameters.
         */
        @GetMapping
        public Page<Product> listProducts(
                @PageableDefault(size = 10, sort = "id") Pageable pageable) {
            log.debug("[Day51] GET /products page={} size={} sort={}",
                    pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
            return service.findAll(pageable);
        }

        /**
         * GET /api/day51/products/by-category/{cat}?page=0&size=5
         */
        @GetMapping("/by-category/{category}")
        public Page<Product> listByCategory(
                @PathVariable String category,
                @PageableDefault(size = 5) Pageable pageable) {
            return service.findByCategory(category, pageable);
        }
    }
}
