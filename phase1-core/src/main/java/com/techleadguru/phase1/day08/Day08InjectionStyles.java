package com.techleadguru.phase1.day08;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 * DAY 8 — Constructor vs Setter vs Field Injection
 *
 * THE RULE (Tech Lead standard):
 *   ALWAYS use constructor injection for required dependencies.
 *   NEVER use field injection in production code.
 *
 * WHY CONSTRUCTOR INJECTION WINS:
 *   1. Unit testable without Spring context — new Service(mockRepo) just works.
 *   2. Immutability — final fields. Once set, cannot be changed.
 *   3. Fail-fast — NullPointerException at startup if dep is missing.
 *   4. Circular dependency detection — Spring CANNOT silently break constructor cycles.
 *
 * WHY FIELD INJECTION (@Autowired on field) IS BANNED:
 *   - Cannot unit test without Spring context (fields are null in plain new())
 *   - Hides dependencies — you can't see what a class needs from its constructor
 *   - Allows null injection if Spring misconfigured — NPE at runtime not startup
 *   - Makes circular dependencies invisible until runtime
 *
 * PRODUCTION SCENARIO — Untestable service:
 *   Team has @Autowired field injection everywhere.
 *   Spring Boot test takes 90s per run (~50 tests with @SpringBootTest).
 *   Tech lead mandates unit tests. Dev can't instantiate service without Spring context.
 *   Entire test suite is @SpringBootTest — wastes 90min of CI time per PR.
 *   FIX: Migrate to constructor injection. Unit tests run in <1s with plain new().
 */
@Slf4j
public class Day08InjectionStyles {

    public interface UserRepository {
        String findById(String id);
    }

    public interface MailService {
        void sendWelcome(String email);
    }

    // ===================================================================================
    // THE BAD WAY: Field injection — untestable without Spring
    // ===================================================================================

    @Service("userServiceBad")
    @Slf4j
    public static class UserServiceWithFieldInjection {

        @Autowired
        private UserRepository userRepository; // null in unit tests!

        @Autowired
        private MailService mailService; // null in unit tests!

        public String getUser(String id) {
            // In a unit test: new UserServiceWithFieldInjection().getUser("1") -> NullPointerException
            return userRepository.findById(id);
        }
    }

    // ===================================================================================
    // THE GOOD WAY: Constructor injection — unit testable, immutable, fails fast
    // ===================================================================================

    @Service("userServiceGood")
    @Slf4j
    public static class UserServiceWithConstructorInjection {

        private final UserRepository userRepository; // FINAL — immutable after construction
        private final MailService mailService;

        // Lombok @RequiredArgsConstructor generates this. Explicitly written here for clarity.
        public UserServiceWithConstructorInjection(
                UserRepository userRepository,
                MailService mailService) {
            this.userRepository = userRepository;
            this.mailService = mailService;
            log.info("[DAY 8] Constructor injection: deps injected at construction time");
        }

        public String getUser(String id) {
            return userRepository.findById(id);
        }

        public void registerUser(String id, String email) {
            // Can test this with: new UserServiceWithConstructorInjection(mockRepo, mockMail)
            mailService.sendWelcome(email);
        }
    }

    // ===================================================================================
    // Wiring for demo
    // ===================================================================================

    @Configuration
    static class Day08Config {

        @Bean
        public UserRepository userRepository() {
            return id -> "User[" + id + "]";
        }

        @Bean
        public MailService mailService() {
            return email -> log.info("[MailService] Welcome email sent to: {}", email);
        }

        @Bean
        public org.springframework.boot.ApplicationRunner day08Runner(
                UserServiceWithConstructorInjection goodService) {
            return args -> {
                System.out.println("=== DAY 8: Injection Styles ===");
                System.out.println("[GOOD] getUser: " + goodService.getUser("42"));
                System.out.println();
                System.out.println("RULE: Constructor injection only.");
                System.out.println("RULE: @RequiredArgsConstructor (Lombok) generates it for you.");
                System.out.println("RULE: No @Autowired on fields. Ever.");
                System.out.println("================================");
            };
        }
    }
}
