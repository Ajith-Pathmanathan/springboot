package com.techleadguru.phase1.day13;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * DAY 13 — Circular Dependency: Why Spring Boot 2.6+ Breaks It
 *
 * WHAT IS A CIRCULAR DEPENDENCY:
 *   ServiceA depends on ServiceB.
 *   ServiceB depends on ServiceA.
 *   Spring must create A first, but A needs B, and B needs A → infinite loop.
 *
 * SPRING BOOT 2.5 AND EARLIER:
 *   Spring ALLOWED circular dependencies via a workaround:
 *   1. Create empty shell of ServiceA (not fully initialised).
 *   2. Inject that shell into ServiceB.
 *   3. Finish initialising ServiceA.
 *   Only worked with SETTER or FIELD injection — NOT constructor injection.
 *   Risk: half-initialised beans. Methods called before @PostConstruct ran. Silent bugs.
 *
 * SPRING BOOT 2.6+:
 *   spring.main.allow-circular-references=false (DEFAULT).
 *   Circular dependencies now throw BeanCurrentlyInCreationException at startup.
 *   You MUST fix the design. This is the correct behaviour — circular dep = design smell.
 *
 * THE REAL FIX — redesign, not workaround:
 *   Extract shared logic into a third @Component (the "common" pattern).
 *   Use an event/listener pattern to decouple.
 *   Introduce @Lazy on ONE leg as a last resort.
 *
 * PRODUCTION SCENARIO — half-initialised SecurityService:
 *   SecurityService (singleton) depends on UserService.
 *   UserService (singleton) depends on SecurityService.
 *   Spring Boot 2.5: starts. But SecurityService.init() runs before UserService is ready.
 *   SecurityService.validateToken() uses UserService — works sometimes, NPE sometimes.
 *   Race condition. Fails under load. Never reproduced locally.
 *   FIX (Boot 2.6+): startup refuses with BeanCurrentlyInCreationException.
 *   Correct fix: extract SecurityContext as a shared @Component with no back-reference.
 */
@Slf4j
public class Day13CircularDependency {

    // ===================================================================================
    // THE BROKEN DESIGN — circular dependency (DO NOT DO THIS)
    // ===================================================================================

    /**
     * In a real Spring context this would throw BeanCurrentlyInCreationException.
     * We document it here without wiring it up in a Spring context.
     */
    @Slf4j
    public static class BrokenServiceA {
        private final BrokenServiceB serviceB;

        public BrokenServiceA(BrokenServiceB serviceB) { // can't create: B needs A
            this.serviceB = serviceB;
        }

        public String run() { return "A(" + serviceB.run() + ")"; }
    }

    @Slf4j
    public static class BrokenServiceB {
        private final BrokenServiceA serviceA;

        public BrokenServiceB(BrokenServiceA serviceA) { // can't create: A needs B
            this.serviceA = serviceA;
        }

        public String run() { return "B"; }
    }

    // ===================================================================================
    // THE CORRECT FIX — extract shared logic to a third component
    // ===================================================================================

    /** Common utility extracted to break the cycle */
    @Slf4j
    public static class SharedAuthContext {
        private String currentUserId;

        public void setCurrentUser(String userId) {
            this.currentUserId = userId;
            log.debug("[Day13] Auth context set: {}", userId);
        }

        public String getCurrentUser() { return currentUserId; }
    }

    @Slf4j
    public static class AuthService {
        private final SharedAuthContext authContext;

        public AuthService(SharedAuthContext authContext) {
            this.authContext = authContext;
            log.info("[Day13] AuthService created (no circular dep)");
        }

        public void login(String userId) {
            authContext.setCurrentUser(userId);
        }

        public String getCurrentUser() {
            return authContext.getCurrentUser();
        }
    }

    @Slf4j
    public static class UserService {
        private final SharedAuthContext authContext;

        public UserService(SharedAuthContext authContext) {
            this.authContext = authContext;
            log.info("[Day13] UserService created (no circular dep)");
        }

        public String getProfile() {
            String user = authContext.getCurrentUser();
            return user != null ? "Profile[" + user + "]" : "NOT_LOGGED_IN";
        }
    }

    // ===================================================================================
    // @Lazy workaround — last resort when full refactor is not possible
    // ===================================================================================

    @Slf4j
    public static class LegacyServiceA {
        private final LegacyServiceB serviceB;

        public LegacyServiceA(@Lazy LegacyServiceB serviceB) { // @Lazy breaks the cycle
            this.serviceB = serviceB;
            log.info("[Day13] LegacyServiceA created with @Lazy ServiceB proxy");
        }

        public String doWork() { return "A+B:" + serviceB.greet(); }
    }

    @Slf4j
    public static class LegacyServiceB {
        private final LegacyServiceA serviceA;

        public LegacyServiceB(LegacyServiceA serviceA) {
            this.serviceA = serviceA;
            log.info("[Day13] LegacyServiceB created");
        }

        public String greet() { return "LegacyB"; }
    }

    // ===================================================================================
    // Configuration (uses the correct fix only)
    // ===================================================================================

    @Configuration
    public static class Day13Config {

        @Bean
        public SharedAuthContext sharedAuthContext() {
            return new SharedAuthContext();
        }

        @Bean
        public AuthService authService(SharedAuthContext ctx) {
            return new AuthService(ctx);
        }

        @Bean
        public UserService userService(SharedAuthContext ctx) {
            return new UserService(ctx);
        }
    }
}
