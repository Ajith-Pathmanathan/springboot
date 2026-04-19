package com.techleadguru.phase3.day49;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DAY 49 — @ControllerAdvice + RFC 7807 ProblemDetail
 *
 * THE PROBLEM with naive error handling:
 *   Some APIs return:  {"error": "not found"}
 *   Others return:     {"status": 404, "message": "Order ORD-99 not found"}
 *   Others return:     plain text "404 error"
 *   → Inconsistent. Clients must handle every format differently.
 *
 * SOLUTION: RFC 7807 Problem Details for HTTP APIs
 *   Standardized JSON error response format:
 *   {
 *     "type":     "https://api.example.com/errors/order-not-found",   // URI reference
 *     "title":    "Order Not Found",                                   // short summary
 *     "status":   404,                                                 // HTTP status
 *     "detail":   "Order ORD-99 does not exist.",                     // human-readable
 *     "instance": "/api/day49/orders/ORD-99"                          // which request
 *   }
 *
 * Spring 6 / Spring Boot 3: ProblemDetail class is built-in.
 *   @Bean ErrorAttributes, ResponseEntityExceptionHandler → already uses ProblemDetail.
 *   Just extend ResponseEntityExceptionHandler in @ControllerAdvice.
 *
 * @ControllerAdvice:
 *   Global component that applies to ALL controllers.
 *   Typical uses:
 *     @ExceptionHandler → handle specific exception types
 *     @ModelAttribute   → add attributes to every model
 *     @InitBinder       → configure data binding
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 */
@Slf4j
public class Day49GlobalExceptionHandler {

    // =========================================================================
    // Custom exception hierarchy
    // =========================================================================

    public static class ResourceNotFoundException extends RuntimeException {
        private final String resourceType;
        private final String resourceId;

        public ResourceNotFoundException(String resourceType, String resourceId) {
            super(resourceType + " not found: " + resourceId);
            this.resourceType = resourceType;
            this.resourceId = resourceId;
        }

        public String getResourceType() { return resourceType; }
        public String getResourceId()   { return resourceId; }
    }

    public static class BusinessRuleException extends RuntimeException {
        private final String errorCode;

        public BusinessRuleException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() { return errorCode; }
    }

    // =========================================================================
    // Global exception handler — maps exceptions to ProblemDetail responses
    // =========================================================================

    @RestControllerAdvice
    @Slf4j
    public static class GlobalExceptionHandler {

        /**
         * Maps ResourceNotFoundException → 404 ProblemDetail
         */
        @ExceptionHandler(ResourceNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
            log.warn("[Day49] Resource not found: {} {}", ex.getResourceType(), ex.getResourceId());

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
            problem.setType(URI.create("https://api.example.com/errors/resource-not-found"));
            problem.setTitle("Resource Not Found");
            problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));

            // Custom extensions — any additional fields
            problem.setProperty("resourceType", ex.getResourceType());
            problem.setProperty("resourceId", ex.getResourceId());
            problem.setProperty("timestamp", Instant.now().toString());

            return problem;
        }

        /**
         * Maps BusinessRuleException → 422 Unprocessable Entity ProblemDetail
         */
        @ExceptionHandler(BusinessRuleException.class)
        @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
        public ProblemDetail handleBusinessRule(BusinessRuleException ex, WebRequest request) {
            log.warn("[Day49] Business rule violated: {} — {}", ex.getErrorCode(), ex.getMessage());

            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
            problem.setType(URI.create("https://api.example.com/errors/business-rule-violation"));
            problem.setTitle("Business Rule Violation");
            problem.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
            problem.setProperty("errorCode", ex.getErrorCode());
            problem.setProperty("timestamp", Instant.now().toString());

            return problem;
        }

        /**
         * Maps @Valid validation failures → 400 ProblemDetail with field errors
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
            log.debug("[Day49] Validation failed: {} field errors", ex.getErrorCount());

            ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problem.setType(URI.create("https://api.example.com/errors/validation-failed"));
            problem.setTitle("Validation Failed");
            problem.setDetail("Request contains " + ex.getErrorCount() + " validation error(s)");

            List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                    .map(fe -> Map.of(
                            "field", fe.getField(),
                            "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"
                    ))
                    .toList();

            problem.setProperty("fieldErrors", errors);
            problem.setProperty("timestamp", Instant.now().toString());

            return problem;
        }
    }

    // =========================================================================
    // Demo controller that throws the custom exceptions
    // =========================================================================

    @RestController
    @RequestMapping("/api/day49/orders")
    @Slf4j
    public static class OrderApiController {

        private static final Map<String, String> store = Map.of(
                "ORD-10", "ORDER_10_DATA",
                "ORD-11", "ORDER_11_DATA"
        );

        @GetMapping("/{id}")
        public String getOrder(@PathVariable String id) {
            String data = store.get(id);
            if (data == null) {
                throw new ResourceNotFoundException("Order", id); // → 404 ProblemDetail
            }
            return data;
        }

        @PostMapping("/{id}/cancel")
        public String cancelOrder(@PathVariable String id) {
            throw new BusinessRuleException("ORDER_ALREADY_SHIPPED",
                    "Cannot cancel order " + id + ": already shipped"); // → 422 ProblemDetail
        }

        @PostMapping
        public String createOrder(@jakarta.validation.Valid @RequestBody CreateRequest req) {
            return "Created: " + req.customerId();
        }
    }

    public record CreateRequest(
            @jakarta.validation.constraints.NotBlank String customerId,
            @jakarta.validation.constraints.Positive java.math.BigDecimal amount
    ) {}
}
