package com.techleadguru.phase3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Phase 3 — Web Layer Internals (Days 43–56)
 *
 * Topics:
 *   DispatcherServlet, HandlerMethodArgumentResolver, HttpMessageConverter (Days 43-46)
 *   Filters, Interceptors, MDC correlation IDs, @ControllerAdvice (Days 47-49)
 *   Validation, pagination, versioning, idempotency, rate limiting, CORS, WebFlux (Days 50-56)
 */
@SpringBootApplication
public class Phase3Application {
    public static void main(String[] args) {
        SpringApplication.run(Phase3Application.class, args);
    }
}
