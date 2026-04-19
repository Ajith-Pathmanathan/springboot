package com.techleadguru.phase3.day43;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DAY 43 — DispatcherServlet: The Front Controller Pattern
 *
 * WHAT HAPPENS ON EVERY HTTP REQUEST (step by step):
 *
 *   1. Servlet container (Tomcat) receives raw HTTP bytes from the network.
 *   2. DispatcherServlet — the single Servlet registered in the app — gets the request.
 *   3. HandlerMapping.getHandler(request)
 *        → returns HandlerExecutionChain:  handler + matching interceptors
 *        Default: RequestMappingHandlerMapping (finds @RequestMapping methods)
 *                 RouterFunctionMapping (finds functional @Bean RouterFunction routes)
 *   4. HandlerAdapter.handle(request, response, handler)
 *        → RequestMappingHandlerAdapter invokes the reflected controller method.
 *        → Uses HandlerMethodArgumentResolver to fill each parameter:
 *            @RequestBody   → HttpMessageConverter.read()   (deserialize JSON → POJO)
 *            @PathVariable  → UriTemplateVariablesHelper
 *            @RequestParam  → ServletRequestUtils
 *   5. On return value: HttpMessageConverter.write()
 *        → MappingJackson2HttpMessageConverter serializes POJO → JSON
 *        → Content negotiation: picks converter matching Accept header
 *   6. On exception: HandlerExceptionResolver chain
 *        → ExceptionHandlerExceptionResolver checks @ControllerAdvice @ExceptionHandler
 *        → ResponseStatusExceptionResolver maps @ResponseStatus / ResponseStatusException
 *        → DefaultHandlerExceptionResolver handles Spring MVC built-in exceptions
 *
 * KEY CLASSES (all in spring-webmvc.jar):
 *   DispatcherServlet              — entry point, orchestrates everything
 *   RequestMappingHandlerMapping   — URL+HTTP-method → handler method
 *   RequestMappingHandlerAdapter   — invokes the handler method
 *   MappingJackson2HttpMessageConverter — JSON serialization
 *   ExceptionHandlerExceptionResolver   — delegates to @ExceptionHandler
 *
 * WHY IT MATTERS FOR TECH LEADS:
 *   "Why did my @ExceptionHandler not fire?" → Wrong exception type or wrong class placement.
 *   "Why did JSON not deserialize?" → Wrong Content-Type or missing @RequestBody.
 *   "DispatcherServlet.properties" — lists all default strategy beans Spring Boot overrides.
 */
@Slf4j
public class Day43DispatcherServlet {

    public record Order(String id, String customerId, BigDecimal amount, String status) {}

    public record CreateOrderRequest(String customerId, BigDecimal amount) {}

    // =========================================================================
    // Controller — demonstrates the full DispatcherServlet pipeline in action
    // =========================================================================

    @RestController
    @RequestMapping("/api/day43/orders")
    @Slf4j
    public static class OrderController {

        private final Map<String, Order> store = new ConcurrentHashMap<>();
        private final AtomicLong counter = new AtomicLong(3);

        public OrderController() {
            store.put("ORD-1", new Order("ORD-1", "CUST-A", BigDecimal.valueOf(150.00), "PENDING"));
            store.put("ORD-2", new Order("ORD-2", "CUST-B", BigDecimal.valueOf(299.99), "SHIPPED"));
        }

        /**
         * HandlerMapping path: GET /api/day43/orders
         * Return type List<Order> → MappingJackson2HttpMessageConverter → JSON array
         */
        @GetMapping
        public List<Order> listOrders() {
            log.debug("[Day43] GET /orders — RequestMappingHandlerAdapter invoked this method");
            return new ArrayList<>(store.values());
        }

        /**
         * @PathVariable → UriTemplateVariablesHelper extracts "id" from URL template
         * ResponseStatusException → ResponseStatusExceptionResolver maps to 404
         */
        @GetMapping("/{id}")
        public Order getOrder(@PathVariable String id) {
            Order order = store.get(id);
            if (order == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id);
            }
            return order;
        }

        /**
         * @RequestBody → HttpMessageConverter.read() deserializes JSON body → CreateOrderRequest
         * @ResponseStatus(CREATED) → response status 201
         */
        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public Order createOrder(@RequestBody CreateOrderRequest req) {
            String id = "ORD-" + counter.getAndIncrement();
            Order order = new Order(id, req.customerId(), req.amount(), "PENDING");
            store.put(id, order);
            log.debug("[Day43] Created order: {}", id);
            return order;
        }
    }
}
