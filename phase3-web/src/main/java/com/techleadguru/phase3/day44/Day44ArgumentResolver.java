package com.techleadguru.phase3.day44;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.lang.annotation.*;
import java.util.List;

/**
 * DAY 44 — Custom HandlerMethodArgumentResolver
 *
 * PROBLEM: Every controller needs the authenticated user's details (userId, username, roles).
 *   BAD:  @GetMapping public Profile getProfile(@RequestHeader("X-User-Id") String userId,
 *                                               @RequestHeader("X-User-Name") String name)
 *   Every endpoint duplicates header extraction. No validation. No encapsulation.
 *
 * SOLUTION: HandlerMethodArgumentResolver
 *   Spring MVC checks all registered resolvers for each method parameter.
 *   If supportsParameter() returns true → resolveArgument() is called to produce the value.
 *   Result is passed as a method parameter.
 *
 *   GOOD:  @GetMapping public Profile getProfile(@CurrentUser UserContext user)
 *           → resolver reads X-User-Id + X-User-Name + X-User-Roles from headers
 *           → builds UserContext ONCE, reused everywhere
 *
 * BUILT-IN RESOLVERS (registered by RequestMappingHandlerAdapter):
 *   @RequestParam  → RequestParamMethodArgumentResolver
 *   @PathVariable  → PathVariableMethodArgumentResolver
 *   @RequestBody   → RequestResponseBodyMethodProcessor
 *   @RequestHeader → RequestHeaderMethodArgumentResolver
 *   Pageable       → PageableHandlerMethodArgumentResolver  (Spring Data)
 *   Principal      → PrincipalMethodArgumentResolver
 *
 * REGISTRATION: Override WebMvcConfigurer.addArgumentResolvers()
 * ORDER: Custom resolvers run BEFORE built-in resolvers.
 *
 * REAL-WORLD USE CASES:
 *   - @CurrentUser → resolve from JWT or session
 *   - @TenantId → resolve from subdomain or header  
 *   - Pageable → already done by Spring Data
 */
@Slf4j
public class Day44ArgumentResolver {

    // =========================================================================
    // @CurrentUser annotation — marks a parameter for custom resolution
    // =========================================================================

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface CurrentUser {}

    // =========================================================================
    // UserContext — the resolved DTO passed to controller methods
    // =========================================================================

    public record UserContext(String userId, String username, List<String> roles) {
        public boolean hasRole(String role) {
            return roles != null && roles.contains(role);
        }
    }

    // =========================================================================
    // The resolver implementation
    // =========================================================================

    public static class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

        /**
         * Spring calls this for EVERY parameter of every @RequestMapping method.
         * Only return true if we know how to resolve this parameter.
         */
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class)
                    && parameter.getParameterType().equals(UserContext.class);
        }

        /**
         * Called only when supportsParameter() returned true.
         * Read headers, build and return the UserContext.
         */
        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {

            String userId = webRequest.getHeader("X-User-Id");
            String username = webRequest.getHeader("X-User-Name");
            String rolesHeader = webRequest.getHeader("X-User-Roles"); // "ADMIN,USER"

            if (userId == null || userId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "X-User-Id header is required");
            }

            List<String> roles = (rolesHeader != null && !rolesHeader.isBlank())
                    ? List.of(rolesHeader.split(","))
                    : List.of();

            UserContext ctx = new UserContext(userId, username != null ? username : "anonymous", roles);
            log.debug("[Day44] Resolved UserContext: userId={}, roles={}", userId, roles);
            return ctx;
        }
    }

    // =========================================================================
    // WebConfig — registers the resolver with Spring MVC
    // =========================================================================

    @Configuration
    public static class Day44WebConfig implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            // Custom resolvers run BEFORE Spring's built-in resolvers
            resolvers.add(new CurrentUserArgumentResolver());
            log.debug("[Day44] CurrentUserArgumentResolver registered");
        }
    }

    // =========================================================================
    // Controller — uses @CurrentUser instead of raw @RequestHeader extraction
    // =========================================================================

    @RestController
    @RequestMapping("/api/day44/profile")
    @Slf4j
    public static class ProfileController {

        @GetMapping
        public ProfileResponse getProfile(@CurrentUser UserContext user) {
            log.debug("[Day44] getProfile called for userId={}", user.userId());
            return new ProfileResponse(
                    user.userId(),
                    user.username(),
                    user.roles(),
                    user.hasRole("ADMIN")
            );
        }

        @GetMapping("/admin-only")
        public String adminOnly(@CurrentUser UserContext user) {
            if (!user.hasRole("ADMIN")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
            }
            return "Welcome, admin " + user.username();
        }
    }

    public record ProfileResponse(String userId, String username, List<String> roles, boolean isAdmin) {}
}
