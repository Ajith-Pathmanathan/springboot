package com.techleadguru.phase3.day55;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

/**
 * DAY 55 — CORS: Fix the Security Misconfiguration
 *
 * WHAT IS CORS?
 *   The browser's Same-Origin Policy blocks JavaScript on page A from
 *   making requests to server B (different origin = different scheme/host/port).
 *   CORS (Cross-Origin Resource Sharing) lets server B say:
 *   "I allow requests from origin A."
 *
 * HOW CORS WORKS:
 *   1. Browser sends preflight:  OPTIONS /api/data
 *                                 Origin: http://frontend.example.com
 *                                 Access-Control-Request-Method: POST
 *   2. Server responds:          Access-Control-Allow-Origin: http://frontend.example.com
 *                                 Access-Control-Allow-Methods: GET, POST, PUT, DELETE
 *                                 Access-Control-Allow-Headers: Content-Type, Authorization
 *   3. Browser sees "allowed" → sends actual request.
 *   4. Server includes CORS headers in real response too.
 *
 * THE COMMON SECURITY MISCONFIGURATION:
 *
 *   WRONG (INSECURE):
 *     .allowedOrigins("*").allowCredentials(true)
 *     → Spring throws IllegalArgumentException in Spring 5.3+
 *     → Even if it didn't throw: allows ANY origin to make credentialed requests
 *     → Attacker's page can make API calls WITH the victim's cookies/session
 *     → This is a CSRF/session-riding vulnerability
 *
 *   RIGHT — specific origin with credentials:
 *     .allowedOrigins("https://app.example.com").allowCredentials(true)
 *
 *   RIGHT — wildcard WITHOUT credentials (for public APIs):
 *     .allowedOriginPatterns("*").allowCredentials(false)
 *
 * @CrossOrigin vs global config:
 *   @CrossOrigin(origins = "http://ui.example.com") on @RestController
 *     → per-controller, quick to add but scattered across codebase
 *   WebMvcConfigurer.addCorsMappings()
 *     → global, centralized, preferred for production
 *
 * Spring Security CORS:
 *   If Spring Security is active, it has its own CORS filter that runs BEFORE
 *   Spring MVC's CorsFilter. You must configure CORS via HttpSecurity.cors(),
 *   not only via WebMvcConfigurer.
 */
@Slf4j
public class Day55Cors {

    // =========================================================================
    // CORS configuration — correct, secure
    // =========================================================================

    @Configuration
    @ConditionalOnProperty(name = "phase3.day55.cors.enabled", havingValue = "true", matchIfMissing = false)
    @Slf4j
    public static class CorsConfig implements WebMvcConfigurer {

        @Value("${phase3.day55.cors.allowed-origin:http://localhost:3000}")
        private String allowedOrigin;

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/day55/**")
                    // Specific origin only (not "*") when credentials are enabled
                    .allowedOrigins(allowedOrigin)
                    // Explicitly list allowed methods
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    // Allow Authorization + Content-Type headers
                    .allowedHeaders("Authorization", "Content-Type", "X-Request-ID", "X-API-Version")
                    // Allow cookies/authorization headers to be sent
                    .allowCredentials(true)
                    // Cache preflight for 1 hour (reduces OPTIONS round-trips)
                    .maxAge(3600);

            log.debug("[Day55] CORS configured: origin={}, credentials=true", allowedOrigin);
        }
    }

    // =========================================================================
    // Demo controller
    // =========================================================================

    @RestController
    @RequestMapping("/api/day55")
    public static class CorsApiController {

        @GetMapping("/data")
        public Map<String, String> getData() {
            return Map.of("message", "CORS-enabled data", "origin", "server");
        }

        @PostMapping("/data")
        public Map<String, String> postData(@RequestBody Map<String, String> body) {
            return Map.of("received", body.getOrDefault("message", "empty"), "status", "OK");
        }

        /**
         * Documents the WRONG way — Spring throws IllegalArgumentException if you try:
         *   allowedOrigins("*").allowCredentials(true)
         * This method documents WHY it's insecure.
         */
        @GetMapping("/cors-guide")
        public List<Map<String, String>> corsGuide() {
            return List.of(
                    Map.of("scenario", "Public API (no credentials)",
                            "config", ".allowedOriginPatterns(\"*\").allowCredentials(false)"),
                    Map.of("scenario", "Private API (with cookies/auth)",
                            "config", ".allowedOrigins(\"https://app.example.com\").allowCredentials(true)"),
                    Map.of("scenario", "DANGEROUS — allows attacker to use victim's session",
                            "config", ".allowedOrigins(\"*\").allowCredentials(true) — Spring FORBIDS this!")
            );
        }
    }
}
